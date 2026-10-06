import {
  Injectable, ConflictException, UnauthorizedException,
  BadRequestException, NotFoundException, ForbiddenException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, MoreThan } from 'typeorm';
import * as bcrypt from 'bcrypt';
import * as crypto from 'crypto';
import { User, UserRole } from '../users/entities/user.entity';
import { RefreshToken } from '../users/entities/refresh-token.entity';
import { EmailVerificationToken } from './entities/email-verification-token.entity';
import { PasswordResetToken } from './entities/password-reset-token.entity';
import { LoginStatus } from './entities/login-history.entity';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { ChangePasswordDto } from './dto/change-password.dto';
import { JwtPayload } from './strategies/jwt.strategy';
import { MfaService } from './mfa.service';
import { LoginHistoryService } from './login-history.service';
import { SessionService } from './session.service';
import { QueueService } from '../queue/queue.service';
import { AppLogger } from '../../common/logger/logger.service';
import { RequestMeta } from '../../common/decorators/request-meta.decorator';

export interface AuthTokens {
  accessToken : string;
  refreshToken: string;
  expiresIn   : number;
}

export interface AuthResponse {
  user  : Partial<User>;
  tokens: AuthTokens;
}

export interface MfaChallengeResponse {
  mfaRequired: true;
  mfaToken   : string;
}

const MAX_FAILED_ATTEMPTS    = 5;
const FAILED_ATTEMPTS_WINDOW = 15; // minutes
const EMAIL_TOKEN_TTL_HOURS  = 24;
const RESET_TOKEN_TTL_HOURS  = 1;
const MFA_CHALLENGE_TTL      = '5m';

@Injectable()
export class AuthService {
  private readonly BCRYPT_ROUNDS = 12;

  constructor(
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
    @InjectRepository(RefreshToken)
    private readonly refreshTokenRepository: Repository<RefreshToken>,
    @InjectRepository(EmailVerificationToken)
    private readonly emailTokenRepository: Repository<EmailVerificationToken>,
    @InjectRepository(PasswordResetToken)
    private readonly resetTokenRepository: Repository<PasswordResetToken>,
    private readonly jwtService    : JwtService,
    private readonly configService : ConfigService,
    private readonly mfaService    : MfaService,
    private readonly loginHistory  : LoginHistoryService,
    private readonly sessionService: SessionService,
    private readonly queueService  : QueueService,
    private readonly logger        : AppLogger,
  ) {
    this.logger.setContext('AuthService');
  }

  // ════════════════════════════════════════════════════════════════════════
  // REGISTER
  // ════════════════════════════════════════════════════════════════════════

  async register(dto: RegisterDto, meta: RequestMeta): Promise<AuthResponse> {
    const { email, username, password, firstName, lastName, university, role } = dto;

    const existing = await this.userRepository.findOne({ where: [{ email }, { username }] });
    if (existing) {
      if (existing.email === email) throw new ConflictException('An account with this email already exists');
      throw new ConflictException('This username is already taken');
    }

    const passwordHash = await bcrypt.hash(password, this.BCRYPT_ROUNDS);
    const user = await this.userRepository.save(
      this.userRepository.create({
        email, username, passwordHash, firstName, lastName,
        university, role: role || UserRole.STUDENT,
      }),
    );

    // Issue email verification token + queue the email
    await this.issueEmailVerificationToken(user);

    const tokens = await this.generateAndSaveTokens(user, meta);

    this.logger.audit('user_registered', { userId: user.id, email: user.email });

    return { user: this.sanitize(user), tokens };
  }

  // ════════════════════════════════════════════════════════════════════════
  // LOGIN  (returns tokens, or an MFA challenge if enabled)
  // ════════════════════════════════════════════════════════════════════════

  async login(dto: LoginDto, meta: RequestMeta): Promise<AuthResponse | MfaChallengeResponse> {
    const { email, password } = dto;

    // Brute-force guard
    const recentFailures = await this.loginHistory.countRecentFailuresForEmail(email, FAILED_ATTEMPTS_WINDOW);
    if (recentFailures >= MAX_FAILED_ATTEMPTS) {
      throw new ForbiddenException(
        `Too many failed attempts. Please try again in ${FAILED_ATTEMPTS_WINDOW} minutes.`,
      );
    }

    const user = await this.userRepository.findOne({ where: { email } });

    if (!user) {
      await this.loginHistory.record({
        emailAttempted: email, status: LoginStatus.FAILED_NOT_FOUND,
        ipAddress: meta.ipAddress, userAgent: meta.userAgent,
      });
      throw new UnauthorizedException('Invalid email or password');
    }

    if (!user.isActive) {
      await this.loginHistory.record({
        userId: user.id, emailAttempted: email, status: LoginStatus.FAILED_INACTIVE,
        ipAddress: meta.ipAddress, userAgent: meta.userAgent,
      });
      throw new UnauthorizedException('Your account has been deactivated');
    }

    const valid = await bcrypt.compare(password, user.passwordHash);
    if (!valid) {
      await this.loginHistory.record({
        userId: user.id, emailAttempted: email, status: LoginStatus.FAILED_PASSWORD,
        ipAddress: meta.ipAddress, userAgent: meta.userAgent,
      });
      throw new UnauthorizedException('Invalid email or password');
    }

    // MFA challenge — password correct but a second factor is required
    if (await this.mfaService.isEnabled(user.id)) {
      const mfaToken = await this.jwtService.signAsync(
        { sub: user.id, purpose: 'mfa_challenge' },
        { secret: this.configService.get<string>('jwt.accessSecret'), expiresIn: MFA_CHALLENGE_TTL },
      );
      return { mfaRequired: true, mfaToken };
    }

    return this.completeLogin(user, meta);
  }

  /** Step 2 — exchange a valid mfaToken + TOTP/backup code for real tokens. */
  async verifyMfaAndLogin(mfaToken: string, code: string, meta: RequestMeta): Promise<AuthResponse> {
    let payload: { sub: string; purpose: string };
    try {
      payload = await this.jwtService.verifyAsync(mfaToken, {
        secret: this.configService.get<string>('jwt.accessSecret'),
      });
    } catch {
      throw new UnauthorizedException('MFA session expired — please log in again');
    }
    if (payload.purpose !== 'mfa_challenge') {
      throw new UnauthorizedException('Invalid MFA session');
    }

    const user = await this.userRepository.findOne({ where: { id: payload.sub, isActive: true } });
    if (!user) throw new UnauthorizedException('User not found');

    const codeValid = await this.mfaService.verifyCode(user.id, code);
    if (!codeValid) {
      await this.loginHistory.record({
        userId: user.id, status: LoginStatus.FAILED_MFA,
        ipAddress: meta.ipAddress, userAgent: meta.userAgent,
      });
      throw new UnauthorizedException('Invalid authentication code');
    }

    return this.completeLogin(user, meta);
  }

  private async completeLogin(user: User, meta: RequestMeta): Promise<AuthResponse> {
    user.lastSeenAt = new Date();
    await this.userRepository.save(user);

    const tokens = await this.generateAndSaveTokens(user, meta);

    await this.loginHistory.record({
      userId: user.id, emailAttempted: user.email, status: LoginStatus.SUCCESS,
      ipAddress: meta.ipAddress, userAgent: meta.userAgent,
    });

    return { user: this.sanitize(user), tokens };
  }

  // ════════════════════════════════════════════════════════════════════════
  // LOGOUT  (single device, or all devices)
  // ════════════════════════════════════════════════════════════════════════

  async logout(userId: string): Promise<void> {
    await this.refreshTokenRepository.delete({ userId });
    this.logger.audit('user_logout_all', { userId });
  }

  async logoutCurrentDevice(userId: string, refreshToken: string): Promise<void> {
    const tokens = await this.refreshTokenRepository.find({ where: { userId } });
    for (const stored of tokens) {
      if (await bcrypt.compare(refreshToken, stored.tokenHash)) {
        await this.refreshTokenRepository.delete({ id: stored.id });
        break;
      }
    }
  }

  // ════════════════════════════════════════════════════════════════════════
  // REFRESH
  // ════════════════════════════════════════════════════════════════════════

  async refreshTokens(userId: string, incomingToken: string, meta: RequestMeta): Promise<AuthTokens> {
    const user = await this.userRepository.findOne({ where: { id: userId, isActive: true } });
    if (!user) throw new UnauthorizedException('Access denied');

    const storedTokens = await this.refreshTokenRepository.find({ where: { userId } });

    let validToken: RefreshToken | null = null;
    for (const stored of storedTokens) {
      if (await bcrypt.compare(incomingToken, stored.tokenHash)) {
        validToken = stored;
        break;
      }
    }

    if (!validToken || validToken.expiresAt < new Date()) {
      // Possible token theft — proactively revoke all sessions for safety
      await this.refreshTokenRepository.delete({ userId });
      this.logger.audit('refresh_token_reuse_detected', { userId });
      throw new UnauthorizedException('Refresh token is invalid or expired');
    }

    await this.refreshTokenRepository.delete({ id: validToken.id });
    await this.sessionService.touchSession(validToken.id);

    return this.generateAndSaveTokens(user, meta);
  }

  // ════════════════════════════════════════════════════════════════════════
  // EMAIL VERIFICATION
  // ════════════════════════════════════════════════════════════════════════

  private async issueEmailVerificationToken(user: User): Promise<void> {
    const rawToken  = crypto.randomBytes(32).toString('hex');
    const tokenHash = crypto.createHash('sha256').update(rawToken).digest('hex');
    const expiresAt = new Date(Date.now() + EMAIL_TOKEN_TTL_HOURS * 60 * 60 * 1000);

    await this.emailTokenRepository.save(
      this.emailTokenRepository.create({ userId: user.id, tokenHash, expiresAt }),
    );

    await this.queueService.sendVerificationEmail(user.email, user.firstName, rawToken);
  }

  async resendVerificationEmail(email: string): Promise<void> {
    const user = await this.userRepository.findOne({ where: { email } });
    // Always return success-shaped response to avoid leaking account existence
    if (!user || user.isEmailVerified) return;

    await this.emailTokenRepository.delete({ userId: user.id });
    await this.issueEmailVerificationToken(user);
  }

  async verifyEmail(rawToken: string): Promise<void> {
    const tokenHash = crypto.createHash('sha256').update(rawToken).digest('hex');
    const record = await this.emailTokenRepository.findOne({ where: { tokenHash } });

    if (!record || record.usedAt || record.expiresAt < new Date()) {
      throw new BadRequestException('This verification link is invalid or has expired');
    }

    const user = await this.userRepository.findOne({ where: { id: record.userId } });
    if (!user) throw new NotFoundException('User not found');

    user.isEmailVerified = true;
    await this.userRepository.save(user);

    record.usedAt = new Date();
    await this.emailTokenRepository.save(record);

    await this.queueService.sendWelcomeEmail(user.email, user.firstName);
    this.logger.audit('email_verified', { userId: user.id });
  }

  // ════════════════════════════════════════════════════════════════════════
  // PASSWORD RESET
  // ════════════════════════════════════════════════════════════════════════

  async forgotPassword(email: string, ipAddress: string): Promise<void> {
    const user = await this.userRepository.findOne({ where: { email } });
    if (!user) return; // don't leak account existence

    // Rate-limit: max 3 active reset requests per hour
    const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000);
    const recentCount = await this.resetTokenRepository.count({
      where: { userId: user.id, createdAt: MoreThan(oneHourAgo) },
    });
    if (recentCount >= 3) {
      this.logger.warn('Password reset rate limit hit', { userId: user.id });
      return;
    }

    const rawToken  = crypto.randomBytes(32).toString('hex');
    const tokenHash = crypto.createHash('sha256').update(rawToken).digest('hex');
    const expiresAt = new Date(Date.now() + RESET_TOKEN_TTL_HOURS * 60 * 60 * 1000);

    await this.resetTokenRepository.save(
      this.resetTokenRepository.create({ userId: user.id, tokenHash, expiresAt, requestedIp: ipAddress }),
    );

    await this.queueService.sendPasswordResetEmail(user.email, user.firstName, rawToken);
    this.logger.audit('password_reset_requested', { userId: user.id, ip: ipAddress });
  }

  async resetPassword(rawToken: string, newPassword: string): Promise<void> {
    const tokenHash = crypto.createHash('sha256').update(rawToken).digest('hex');
    const record = await this.resetTokenRepository.findOne({ where: { tokenHash } });

    if (!record || record.usedAt || record.expiresAt < new Date()) {
      throw new BadRequestException('This reset link is invalid or has expired');
    }

    const user = await this.userRepository.findOne({ where: { id: record.userId } });
    if (!user) throw new NotFoundException('User not found');

    user.passwordHash = await bcrypt.hash(newPassword, this.BCRYPT_ROUNDS);
    await this.userRepository.save(user);

    record.usedAt = new Date();
    await this.resetTokenRepository.save(record);

    // Security: invalidate every existing session — force re-login everywhere
    await this.refreshTokenRepository.delete({ userId: user.id });

    this.logger.audit('password_reset_completed', { userId: user.id });
  }

  async changePassword(userId: string, dto: ChangePasswordDto): Promise<void> {
    const user = await this.userRepository.findOne({ where: { id: userId } });
    if (!user) throw new NotFoundException('User not found');

    const valid = await bcrypt.compare(dto.currentPassword, user.passwordHash);
    if (!valid) throw new UnauthorizedException('Current password is incorrect');

    user.passwordHash = await bcrypt.hash(dto.newPassword, this.BCRYPT_ROUNDS);
    await this.userRepository.save(user);

    this.logger.audit('password_changed', { userId: user.id });
  }

  // ════════════════════════════════════════════════════════════════════════
  // TOKEN GENERATION
  // ════════════════════════════════════════════════════════════════════════

  private async generateAndSaveTokens(user: User, meta: RequestMeta): Promise<AuthTokens> {
    const payload: JwtPayload = { sub: user.id, email: user.email, role: user.role };

    const [accessToken, refreshToken] = await Promise.all([
      this.jwtService.signAsync(payload, {
        secret   : this.configService.get<string>('jwt.accessSecret'),
        expiresIn: this.configService.get<string>('jwt.accessExpiresIn', '15m'),
      }),
      this.jwtService.signAsync(payload, {
        secret   : this.configService.get<string>('jwt.refreshSecret'),
        expiresIn: this.configService.get<string>('jwt.refreshExpiresIn', '7d'),
      }),
    ]);

    const tokenHash = await bcrypt.hash(refreshToken, 10);
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 7);

    const savedToken = await this.refreshTokenRepository.save(
      this.refreshTokenRepository.create({
        userId   : user.id,
        tokenHash,
        expiresAt,
        ipAddress: meta.ipAddress,
        userAgent: meta.userAgent,
      }),
    );

    // Track this as a device session
    await this.sessionService.createSession({
      userId        : user.id,
      refreshTokenId: savedToken.id,
      ipAddress     : meta.ipAddress,
      userAgent     : meta.userAgent,
    });

    return { accessToken, refreshToken, expiresIn: 900 };
  }

  private sanitize(user: User): Partial<User> {
    const { passwordHash, refreshTokens, ...rest } = user as any;
    return rest;
  }
}
