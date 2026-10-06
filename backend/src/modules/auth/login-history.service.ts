import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, MoreThan } from 'typeorm';
import { LoginHistory, LoginStatus } from './entities/login-history.entity';
import { parseUserAgent } from '../../common/utils/user-agent.util';
import { AppLogger } from '../../common/logger/logger.service';

export interface RecordLoginParams {
  userId?        : string;
  emailAttempted?: string;
  status         : LoginStatus;
  ipAddress      : string;
  userAgent      : string;
}

@Injectable()
export class LoginHistoryService {
  constructor(
    @InjectRepository(LoginHistory)
    private readonly historyRepository: Repository<LoginHistory>,
    private readonly logger: AppLogger,
  ) {
    this.logger.setContext('LoginHistoryService');
  }

  async record(params: RecordLoginParams): Promise<LoginHistory> {
    const { device, browser, os } = parseUserAgent(params.userAgent);

    let isSuspicious = false;
    let suspiciousReason: string | undefined;

    if (params.status === LoginStatus.SUCCESS && params.userId) {
      const check = await this.detectSuspiciousActivity(params.userId, params.ipAddress);
      isSuspicious     = check.isSuspicious;
      suspiciousReason = check.reason;
    }

    const entry = await this.historyRepository.save(
      this.historyRepository.create({
        userId        : params.userId,
        emailAttempted: params.emailAttempted,
        status        : params.status,
        ipAddress     : params.ipAddress,
        userAgent     : params.userAgent,
        device, browser, os,
        isSuspicious,
        suspiciousReason,
      }),
    );

    if (isSuspicious) {
      this.logger.audit('suspicious_login', {
        userId: params.userId,
        reason: suspiciousReason,
        ip    : params.ipAddress,
      });
    }

    return entry;
  }

  /**
   * Flags a login as suspicious when:
   *  - This is the user's first login from this IP address, AND
   *  - The user has a prior successful login history (i.e. not their very first login ever)
   *  - More than 5 failed attempts occurred in the last 15 minutes for this account
   */
  private async detectSuspiciousActivity(
    userId: string,
    ipAddress: string,
  ): Promise<{ isSuspicious: boolean; reason?: string }> {
    const priorSuccessfulLogins = await this.historyRepository.count({
      where: { userId, status: LoginStatus.SUCCESS },
    });

    if (priorSuccessfulLogins > 0) {
      const seenFromThisIp = await this.historyRepository.findOne({
        where: { userId, ipAddress, status: LoginStatus.SUCCESS },
      });
      if (!seenFromThisIp) {
        return { isSuspicious: true, reason: 'New IP address for this account' };
      }
    }

    const fifteenMinAgo = new Date(Date.now() - 15 * 60 * 1000);
    const recentFailures = await this.historyRepository.count({
      where: {
        userId,
        status   : LoginStatus.FAILED_PASSWORD,
        createdAt: MoreThan(fifteenMinAgo),
      },
    });
    if (recentFailures >= 5) {
      return { isSuspicious: true, reason: 'Multiple failed login attempts before success' };
    }

    return { isSuspicious: false };
  }

  async getRecentHistory(userId: string, limit = 20): Promise<LoginHistory[]> {
    return this.historyRepository.find({
      where   : { userId },
      order   : { createdAt: 'DESC' },
      take    : limit,
    });
  }

  /** Used by brute-force protection — counts recent failures for an email. */
  async countRecentFailuresForEmail(email: string, windowMinutes = 15): Promise<number> {
    const since = new Date(Date.now() - windowMinutes * 60 * 1000);
    return this.historyRepository.count({
      where: {
        emailAttempted: email,
        status        : LoginStatus.FAILED_PASSWORD,
        createdAt     : MoreThan(since),
      },
    });
  }
}
