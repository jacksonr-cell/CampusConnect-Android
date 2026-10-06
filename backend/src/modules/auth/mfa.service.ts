import { Injectable, BadRequestException, ConflictException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { ConfigService } from '@nestjs/config';
import { authenticator } from 'otplib';
import * as qrcode from 'qrcode';
import * as bcrypt from 'bcrypt';
import * as crypto from 'crypto';
import { MfaSecret } from './entities/mfa-secret.entity';

const BACKUP_CODE_COUNT = 10;

@Injectable()
export class MfaService {
  constructor(
    @InjectRepository(MfaSecret)
    private readonly mfaRepository: Repository<MfaSecret>,
    private readonly configService: ConfigService,
  ) {
    authenticator.options = { window: 1 }; // ±30s tolerance
  }

  /** Step 1 of setup — generate a secret + QR code, not yet enabled. */
  async initiateSetup(userId: string, userEmail: string): Promise<{ qrCodeDataUrl: string; manualEntryKey: string }> {
    const existing = await this.mfaRepository.findOne({ where: { userId } });
    if (existing?.isEnabled) {
      throw new ConflictException('MFA is already enabled on this account');
    }

    const secret = authenticator.generateSecret();
    const appName = this.configService.get<string>('mail.appName', 'CampusConnect');
    const otpauthUrl = authenticator.keyuri(userEmail, appName, secret);
    const qrCodeDataUrl = await qrcode.toDataURL(otpauthUrl);

    if (existing) {
      existing.secret = secret;
      existing.isEnabled = false;
      existing.verifiedAt = null;
      await this.mfaRepository.save(existing);
    } else {
      await this.mfaRepository.save(
        this.mfaRepository.create({ userId, secret, isEnabled: false, backupCodes: [] }),
      );
    }

    return { qrCodeDataUrl, manualEntryKey: secret };
  }

  /** Step 2 of setup — verify the first TOTP code, then activate MFA. */
  async confirmSetup(userId: string, code: string): Promise<{ backupCodes: string[] }> {
    const record = await this.mfaRepository.findOne({ where: { userId } });
    if (!record) throw new BadRequestException('MFA setup was not initiated');

    const valid = authenticator.verify({ token: code, secret: record.secret });
    if (!valid) throw new BadRequestException('Invalid verification code');

    const backupCodes = this.generateBackupCodes();
    const hashedCodes = await Promise.all(backupCodes.map((c) => bcrypt.hash(c, 10)));

    record.isEnabled   = true;
    record.verifiedAt  = new Date();
    record.backupCodes = hashedCodes;
    await this.mfaRepository.save(record);

    return { backupCodes }; // plaintext shown once — user must save them
  }

  async disable(userId: string): Promise<void> {
    await this.mfaRepository.delete({ userId });
  }

  async isEnabled(userId: string): Promise<boolean> {
    const record = await this.mfaRepository.findOne({ where: { userId } });
    return record?.isEnabled ?? false;
  }

  /** Verifies a TOTP code OR a one-time backup code. */
  async verifyCode(userId: string, code: string): Promise<boolean> {
    const record = await this.mfaRepository.findOne({ where: { userId } });
    if (!record?.isEnabled) return false;

    // Try TOTP first
    if (authenticator.verify({ token: code, secret: record.secret })) {
      return true;
    }

    // Fall back to backup codes (single use)
    for (let i = 0; i < record.backupCodes.length; i++) {
      if (await bcrypt.compare(code, record.backupCodes[i])) {
        record.backupCodes.splice(i, 1); // consume the code
        await this.mfaRepository.save(record);
        return true;
      }
    }

    return false;
  }

  private generateBackupCodes(): string[] {
    return Array.from({ length: BACKUP_CODE_COUNT }, () =>
      crypto.randomBytes(5).toString('hex').toUpperCase(),
    );
  }
}
