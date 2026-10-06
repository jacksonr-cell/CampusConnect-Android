import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { UserSession } from './entities/user-session.entity';
import { RefreshToken } from '../users/entities/refresh-token.entity';
import { parseUserAgent, formatDeviceName } from '../../common/utils/user-agent.util';

export interface CreateSessionParams {
  userId        : string;
  refreshTokenId: string;
  ipAddress     : string;
  userAgent     : string;
}

@Injectable()
export class SessionService {
  constructor(
    @InjectRepository(UserSession)
    private readonly sessionRepository: Repository<UserSession>,
    @InjectRepository(RefreshToken)
    private readonly refreshTokenRepository: Repository<RefreshToken>,
  ) {}

  async createSession(params: CreateSessionParams): Promise<UserSession> {
    const parsed = parseUserAgent(params.userAgent);

    // Mark all other sessions as not-current
    await this.sessionRepository.update(
      { userId: params.userId, isCurrent: true },
      { isCurrent: false },
    );

    return this.sessionRepository.save(
      this.sessionRepository.create({
        userId        : params.userId,
        refreshTokenId: params.refreshTokenId,
        deviceName    : formatDeviceName(parsed),
        browser       : parsed.browser,
        os            : parsed.os,
        ipAddress     : params.ipAddress,
        isCurrent     : true,
        lastActiveAt  : new Date(),
      }),
    );
  }

  async listActiveSessions(userId: string): Promise<UserSession[]> {
    return this.sessionRepository.find({
      where  : { userId, revokedAt: null as any },
      order  : { lastActiveAt: 'DESC' },
    });
  }

  async revokeSession(userId: string, sessionId: string): Promise<void> {
    const session = await this.sessionRepository.findOne({
      where: { id: sessionId, userId },
    });
    if (!session) throw new NotFoundException('Session not found');
    if (session.isCurrent) {
      throw new ForbiddenException(
        'Cannot revoke your current session here — use logout instead',
      );
    }

    session.revokedAt = new Date();
    await this.sessionRepository.save(session);

    // Revoke the underlying refresh token too
    if (session.refreshTokenId) {
      await this.refreshTokenRepository.delete({ id: session.refreshTokenId });
    }
  }

  /** Revokes every session except the current one — "log out other devices". */
  async revokeAllOtherSessions(userId: string, currentSessionId: string): Promise<number> {
    const sessions = await this.sessionRepository.find({
      where: { userId, revokedAt: null as any },
    });

    const toRevoke = sessions.filter((s) => s.id !== currentSessionId);
    const tokenIds = toRevoke.map((s) => s.refreshTokenId).filter(Boolean);

    await Promise.all(
      toRevoke.map((s) =>
        this.sessionRepository.update(s.id, { revokedAt: new Date() }),
      ),
    );

    if (tokenIds.length > 0) {
      await this.refreshTokenRepository.delete(tokenIds as string[]);
    }

    return toRevoke.length;
  }

  async touchSession(refreshTokenId: string): Promise<void> {
    await this.sessionRepository.update(
      { refreshTokenId },
      { lastActiveAt: new Date() },
    );
  }
}
