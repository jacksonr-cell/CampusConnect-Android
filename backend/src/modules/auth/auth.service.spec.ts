import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { ConflictException, UnauthorizedException, ForbiddenException } from '@nestjs/common';
import * as bcrypt from 'bcrypt';
import { AuthService } from './auth.service';
import { MfaService } from './mfa.service';
import { LoginHistoryService } from './login-history.service';
import { SessionService } from './session.service';
import { QueueService } from '../queue/queue.service';
import { AppLogger } from '../../common/logger/logger.service';
import { User, UserRole } from '../users/entities/user.entity';
import { RefreshToken } from '../users/entities/refresh-token.entity';
import { EmailVerificationToken } from './entities/email-verification-token.entity';
import { PasswordResetToken } from './entities/password-reset-token.entity';

const mockUser = (overrides: Partial<User> = {}): User => ({
  id              : 'user-uuid-1',
  email           : 'jane@kabarak.ac.ke',
  username        : 'janedoe',
  passwordHash    : '$2b$12$hashedpassword',
  firstName       : 'Jane',
  lastName        : 'Doe',
  role            : UserRole.STUDENT,
  isActive        : true,
  isVerified      : false,
  isEmailVerified : false,
  twoFactorEnabled: false,
  reputationPoints: 0,
  followersCount  : 0,
  followingCount  : 0,
  postsCount      : 0,
  refreshTokens   : [],
  following       : [],
  followers       : [],
  ...overrides,
} as User);

const mockRepo = () => ({
  findOne  : jest.fn(),
  find     : jest.fn(),
  create   : jest.fn(),
  save     : jest.fn(),
  delete   : jest.fn(),
  update   : jest.fn(),
  count    : jest.fn(),
});

const reqMeta = { ipAddress: '127.0.0.1', userAgent: 'jest-test-agent' };

describe('AuthService', () => {
  let service   : AuthService;
  let userRepo  : any;
  let tokenRepo : any;
  let loginHistory: jest.Mocked<LoginHistoryService>;
  let mfaService  : jest.Mocked<MfaService>;
  let queueService  : jest.Mocked<QueueService>;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AuthService,
        { provide: getRepositoryToken(User),                    useValue: mockRepo() },
        { provide: getRepositoryToken(RefreshToken),            useValue: mockRepo() },
        { provide: getRepositoryToken(EmailVerificationToken),  useValue: mockRepo() },
        { provide: getRepositoryToken(PasswordResetToken),      useValue: mockRepo() },
        {
          provide : JwtService,
          useValue: { signAsync: jest.fn().mockResolvedValue('mock.jwt.token'), verifyAsync: jest.fn() },
        },
        {
          provide : ConfigService,
          useValue: {
            get: jest.fn((key: string) => ({
              'jwt.accessSecret'    : 'test-access-secret',
              'jwt.refreshSecret'   : 'test-refresh-secret',
              'jwt.accessExpiresIn' : '15m',
              'jwt.refreshExpiresIn': '7d',
            }[key])),
          },
        },
        {
          provide : MfaService,
          useValue: { isEnabled: jest.fn().mockResolvedValue(false), verifyCode: jest.fn() },
        },
        {
          provide : LoginHistoryService,
          useValue: {
            record: jest.fn(),
            countRecentFailuresForEmail: jest.fn().mockResolvedValue(0),
          },
        },
        {
          provide : SessionService,
          useValue: { createSession: jest.fn(), touchSession: jest.fn() },
        },
        {
          provide : QueueService,
          useValue: {
            sendVerificationEmail : jest.fn(),
            sendPasswordResetEmail: jest.fn(),
            sendWelcomeEmail       : jest.fn(),
          },
        },
        {
          provide : AppLogger,
          useValue: { setContext: jest.fn().mockReturnThis(), log: jest.fn(), warn: jest.fn(), error: jest.fn(), audit: jest.fn() },
        },
      ],
    }).compile();

    service       = module.get(AuthService);
    userRepo      = module.get(getRepositoryToken(User));
    tokenRepo     = module.get(getRepositoryToken(RefreshToken));
    loginHistory  = module.get(LoginHistoryService);
    mfaService    = module.get(MfaService);
    queueService  = module.get(QueueService);
  });

  afterEach(() => jest.clearAllMocks());

  // ── register ─────────────────────────────────────────────────────────────
  describe('register', () => {
    const dto = {
      email    : 'jane@kabarak.ac.ke',
      username : 'janedoe',
      password : 'SecurePass@123',
      firstName: 'Jane',
      lastName : 'Doe',
    };

    it('creates a new user, issues a verification email, and returns tokens', async () => {
      userRepo.findOne.mockResolvedValue(null);
      const user = mockUser();
      userRepo.create.mockReturnValue(user);
      userRepo.save.mockResolvedValue(user);
      tokenRepo.create.mockReturnValue({});
      tokenRepo.save.mockResolvedValue({ id: 'token-1' });

      const result = await service.register(dto as any, reqMeta);

      expect(userRepo.save).toHaveBeenCalled();
      expect(queueService.sendVerificationEmail).toHaveBeenCalledWith(
        user.email, user.firstName, expect.any(String),
      );
      expect((result as any).tokens.accessToken).toBe('mock.jwt.token');
      expect((result as any).user).not.toHaveProperty('passwordHash');
    });

    it('throws ConflictException when email already exists', async () => {
      userRepo.findOne.mockResolvedValue(mockUser());
      await expect(service.register(dto as any, reqMeta)).rejects.toThrow(ConflictException);
    });
  });

  // ── login ─────────────────────────────────────────────────────────────────
  describe('login', () => {
    it('returns tokens for valid credentials with MFA disabled', async () => {
      const user = mockUser();
      userRepo.findOne.mockResolvedValue(user);
      userRepo.save.mockResolvedValue(user);
      tokenRepo.create.mockReturnValue({});
      tokenRepo.save.mockResolvedValue({ id: 'token-1' });
      jest.spyOn(bcrypt, 'compare').mockImplementation(async () => true);

      const result = await service.login({ email: user.email, password: 'correct' } as any, reqMeta);

      expect((result as any).tokens.accessToken).toBeDefined();
      expect(loginHistory.record).toHaveBeenCalled();
    });

    it('returns an MFA challenge when MFA is enabled', async () => {
      const user = mockUser();
      userRepo.findOne.mockResolvedValue(user);
      jest.spyOn(bcrypt, 'compare').mockImplementation(async () => true);
      mfaService.isEnabled.mockResolvedValue(true);

      const result = await service.login({ email: user.email, password: 'correct' } as any, reqMeta);

      expect((result as any).mfaRequired).toBe(true);
      expect((result as any).mfaToken).toBe('mock.jwt.token');
    });

    it('throws UnauthorizedException for wrong password', async () => {
      userRepo.findOne.mockResolvedValue(mockUser());
      jest.spyOn(bcrypt, 'compare').mockImplementation(async () => false);

      await expect(
        service.login({ email: 'jane@kabarak.ac.ke', password: 'wrong' } as any, reqMeta),
      ).rejects.toThrow(UnauthorizedException);
    });

    it('throws UnauthorizedException for unknown email', async () => {
      userRepo.findOne.mockResolvedValue(null);
      await expect(
        service.login({ email: 'ghost@nowhere.com', password: 'any' } as any, reqMeta),
      ).rejects.toThrow(UnauthorizedException);
    });

    it('throws UnauthorizedException for inactive account', async () => {
      userRepo.findOne.mockResolvedValue(mockUser({ isActive: false }));
      await expect(
        service.login({ email: 'jane@kabarak.ac.ke', password: 'any' } as any, reqMeta),
      ).rejects.toThrow(UnauthorizedException);
    });

    it('throws ForbiddenException after too many failed attempts', async () => {
      loginHistory.countRecentFailuresForEmail.mockResolvedValue(5);
      await expect(
        service.login({ email: 'jane@kabarak.ac.ke', password: 'any' } as any, reqMeta),
      ).rejects.toThrow(ForbiddenException);
    });
  });

  // ── logout ────────────────────────────────────────────────────────────────
  describe('logout', () => {
    it('deletes all refresh tokens for the user', async () => {
      tokenRepo.delete.mockResolvedValue({ affected: 2, raw: {} });
      await service.logout('user-uuid-1');
      expect(tokenRepo.delete).toHaveBeenCalledWith({ userId: 'user-uuid-1' });
    });
  });

  // ── password reset ───────────────────────────────────────────────────────
  describe('changePassword', () => {
    it('throws UnauthorizedException for wrong current password', async () => {
      userRepo.findOne.mockResolvedValue(mockUser());
      jest.spyOn(bcrypt, 'compare').mockImplementation(async () => false);

      await expect(
        service.changePassword('user-uuid-1', { currentPassword: 'wrong', newPassword: 'NewPass@123' }),
      ).rejects.toThrow(UnauthorizedException);
    });

    it('updates password hash on success', async () => {
      const user = mockUser();
      userRepo.findOne.mockResolvedValue(user);
      userRepo.save.mockResolvedValue(user);
      jest.spyOn(bcrypt, 'compare').mockImplementation(async () => true);

      await service.changePassword('user-uuid-1', { currentPassword: 'correct', newPassword: 'NewPass@123' });

      expect(userRepo.save).toHaveBeenCalled();
    });
  });
});
