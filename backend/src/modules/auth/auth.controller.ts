import {
  Controller, Post, Get, Patch, Delete, Body, Param,
  UseGuards, HttpCode, HttpStatus, ParseUUIDPipe,
} from '@nestjs/common';
import {
  ApiTags, ApiBearerAuth, ApiOperation, ApiResponse as SwaggerResponse,
} from '@nestjs/swagger';
import { ThrottlerGuard } from '@nestjs/throttler';
import { AuthGuard } from '@nestjs/passport';
import { AuthService } from './auth.service';
import { MfaService } from './mfa.service';
import { LoginHistoryService } from './login-history.service';
import { SessionService } from './session.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';
import { VerifyEmailDto } from './dto/verify-email.dto';
import { ResendVerificationDto } from './dto/resend-verification.dto';
import { ForgotPasswordDto } from './dto/forgot-password.dto';
import { ResetPasswordDto } from './dto/reset-password.dto';
import { ChangePasswordDto } from './dto/change-password.dto';
import { VerifyMfaSetupDto, VerifyMfaLoginDto, DisableMfaDto } from './dto/mfa.dto';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { ReqMeta, RequestMeta } from '../../common/decorators/request-meta.decorator';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { User } from '../users/entities/user.entity';
import * as bcrypt from 'bcrypt';

@ApiTags('auth')
@Controller('auth')
export class AuthController {
  constructor(
    private readonly authService   : AuthService,
    private readonly mfaService    : MfaService,
    private readonly loginHistory  : LoginHistoryService,
    private readonly sessionService: SessionService,
  ) {}

  // ── Registration & login ──────────────────────────────────────────────

  @Post('register')
  @HttpCode(HttpStatus.CREATED)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Create a new student account — sends a verification email' })
  @SwaggerResponse({ status: 201, description: 'Account created successfully' })
  @SwaggerResponse({ status: 409, description: 'Email or username already taken' })
  async register(@Body() dto: RegisterDto, @ReqMeta() meta: RequestMeta) {
    return this.authService.register(dto, meta);
  }

  @Post('login')
  @HttpCode(HttpStatus.OK)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Sign in — returns tokens, or an MFA challenge if 2FA is enabled' })
  @SwaggerResponse({ status: 200, description: 'Login successful, or { mfaRequired: true, mfaToken }' })
  @SwaggerResponse({ status: 401, description: 'Invalid credentials' })
  @SwaggerResponse({ status: 403, description: 'Too many failed attempts — temporarily locked' })
  async login(@Body() dto: LoginDto, @ReqMeta() meta: RequestMeta) {
    return this.authService.login(dto, meta);
  }

  @Post('mfa/verify-login')
  @HttpCode(HttpStatus.OK)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Complete login with a TOTP or backup code after an MFA challenge' })
  async verifyMfaLogin(@Body() dto: VerifyMfaLoginDto, @ReqMeta() meta: RequestMeta) {
    return this.authService.verifyMfaAndLogin(dto.mfaToken, dto.code, meta);
  }

  @Post('logout')
  @HttpCode(HttpStatus.OK)
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Sign out of this device only' })
  async logoutCurrentDevice(@CurrentUser() user: User, @Body('refreshToken') refreshToken: string) {
    if (refreshToken) {
      await this.authService.logoutCurrentDevice(user.id, refreshToken);
    }
    return { message: 'Signed out of this device' };
  }

  @Post('logout-all')
  @HttpCode(HttpStatus.OK)
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Sign out of ALL devices — revokes every refresh token' })
  async logoutAllDevices(@CurrentUser() user: User) {
    await this.authService.logout(user.id);
    return { message: 'Signed out of all devices' };
  }

  @Post('refresh')
  @HttpCode(HttpStatus.OK)
  @UseGuards(AuthGuard('jwt-refresh'))
  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Rotate refresh token — issues a new access + refresh pair' })
  async refresh(
    @CurrentUser() user: User & { refreshToken: string },
    @ReqMeta() meta: RequestMeta,
  ) {
    return this.authService.refreshTokens(user.id, user.refreshToken, meta);
  }

  @Get('me')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Get the currently authenticated user' })
  async getMe(@CurrentUser() user: User) {
    const { passwordHash, refreshTokens, ...sanitized } = user as any;
    return sanitized;
  }

  // ── Email verification ────────────────────────────────────────────────

  @Post('verify-email')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Verify email address using the token from the verification email' })
  async verifyEmail(@Body() dto: VerifyEmailDto) {
    await this.authService.verifyEmail(dto.token);
    return { message: 'Email verified successfully' };
  }

  @Post('resend-verification')
  @HttpCode(HttpStatus.OK)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Resend the email verification link' })
  async resendVerification(@Body() dto: ResendVerificationDto) {
    await this.authService.resendVerificationEmail(dto.email);
    return { message: 'If an account exists with this email, a verification link has been sent' };
  }

  // ── Password reset ────────────────────────────────────────────────────

  @Post('forgot-password')
  @HttpCode(HttpStatus.OK)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Request a password reset email' })
  async forgotPassword(@Body() dto: ForgotPasswordDto, @ReqMeta() meta: RequestMeta) {
    await this.authService.forgotPassword(dto.email, meta.ipAddress);
    return { message: 'If an account exists with this email, a reset link has been sent' };
  }

  @Post('reset-password')
  @HttpCode(HttpStatus.OK)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Reset password using the token from the reset email' })
  async resetPassword(@Body() dto: ResetPasswordDto) {
    await this.authService.resetPassword(dto.token, dto.newPassword);
    return { message: 'Password reset successfully — please sign in with your new password' };
  }

  @Patch('change-password')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Change password while authenticated (requires current password)' })
  async changePassword(@CurrentUser() user: User, @Body() dto: ChangePasswordDto) {
    await this.authService.changePassword(user.id, dto);
    return { message: 'Password changed successfully' };
  }

  // ── Multi-Factor Authentication ───────────────────────────────────────

  @Post('mfa/setup')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Start MFA setup — returns a QR code to scan in an authenticator app' })
  async setupMfa(@CurrentUser() user: User) {
    return this.mfaService.initiateSetup(user.id, user.email);
  }

  @Post('mfa/confirm')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Confirm MFA setup with a code from the authenticator app — returns backup codes' })
  async confirmMfa(@CurrentUser() user: User, @Body() dto: VerifyMfaSetupDto) {
    return this.mfaService.confirmSetup(user.id, dto.code);
  }

  @Delete('mfa')
  @HttpCode(HttpStatus.OK)
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Disable MFA on this account' })
  async disableMfa(@CurrentUser() user: User, @Body() dto: DisableMfaDto) {
    const valid = await bcrypt.compare(dto.password, user.passwordHash);
    if (!valid) {
      return { message: 'Incorrect password', success: false };
    }
    await this.mfaService.disable(user.id);
    return { message: 'MFA disabled' };
  }

  // ── Login history & sessions ──────────────────────────────────────────

  @Get('login-history')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'View recent login attempts on this account' })
  async getLoginHistory(@CurrentUser() user: User) {
    return this.loginHistory.getRecentHistory(user.id);
  }

  @Get('sessions')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'List active sessions / devices signed into this account' })
  async getSessions(@CurrentUser() user: User) {
    return this.sessionService.listActiveSessions(user.id);
  }

  @Delete('sessions/:sessionId')
  @HttpCode(HttpStatus.OK)
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth('access-token')
  @ApiOperation({ summary: 'Revoke a specific session (sign out a device remotely)' })
  async revokeSession(
    @CurrentUser() user: User,
    @Param('sessionId', ParseUUIDPipe) sessionId: string,
  ) {
    await this.sessionService.revokeSession(user.id, sessionId);
    return { message: 'Session revoked' };
  }
}
