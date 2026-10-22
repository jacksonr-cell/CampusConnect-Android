import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { UserPoints, UserLevel } from './entities/user-points.entity';
import { Badge } from './entities/badge.entity';
import { UserBadge } from './entities/user-badge.entity';

const LEVEL_THRESHOLDS: Record<UserLevel, number> = {
  [UserLevel.NEWCOMER]: 0,
  [UserLevel.STUDENT]: 100,
  [UserLevel.ACTIVE_STUDENT]: 500,
  [UserLevel.CAMPUS_REGULAR]: 1500,
  [UserLevel.CAMPUS_STAR]: 5000,
  [UserLevel.CAMPUS_LEGEND]: 15000,
};

/** A catalog badge with the requesting user's earn-state merged in. */
export interface BadgeWithEarnedStatus extends Badge {
  isEarned: boolean;
  earnedAt: Date | null;
}

@Injectable()
export class GamificationService {
  constructor(
    @InjectRepository(UserPoints) private readonly pointsRepository: Repository<UserPoints>,
    @InjectRepository(Badge) private readonly badgeRepository: Repository<Badge>,
    @InjectRepository(UserBadge) private readonly userBadgeRepository: Repository<UserBadge>,
  ) {}

  async awardPoints(userId: string, points: number): Promise<UserPoints> {
    let record = await this.pointsRepository.findOne({ where: { userId } });
    if (!record) {
      record = this.pointsRepository.create({ userId, totalPoints: 0, monthlyPoints: 0 });
    }
    record.totalPoints += points;
    record.monthlyPoints += points;
    record.level = this.calculateLevel(record.totalPoints);
    return this.pointsRepository.save(record);
  }

  async getUserPoints(userId: string): Promise<UserPoints | null> {
    return this.pointsRepository.findOne({ where: { userId }, relations: ['user'] });
  }

  /** @deprecated use getMyBadgesFull for the complete catalog with earned/locked state. */
  async getUserBadges(userId: string): Promise<UserBadge[]> {
    return this.userBadgeRepository.find({
      where: { userId },
      relations: ['badge'],
      order: { earnedAt: 'DESC' },
    });
  }

  /**
   * Full badge catalog (every active badge, earned or not) with the
   * requesting user's earn status merged in — closes the gap where the
   * app previously could only see badges already earned and had no way
   * to render "locked" badges, since no catalog endpoint existed.
   */
  async getMyBadgesFull(userId: string): Promise<BadgeWithEarnedStatus[]> {
    const [allBadges, earned] = await Promise.all([
      this.badgeRepository.find({
        where: { isActive: true },
        order: { category: 'ASC', pointsReward: 'ASC' },
      }),
      this.userBadgeRepository.find({ where: { userId } }),
    ]);

    const earnedMap = new Map(earned.map((ub) => [ub.badgeId, ub.earnedAt]));

    return allBadges.map((badge) => ({
      ...badge,
      isEarned: earnedMap.has(badge.id),
      earnedAt: earnedMap.get(badge.id) ?? null,
    })) as BadgeWithEarnedStatus[];
  }

  async awardBadge(userId: string, badgeId: string): Promise<UserBadge | null> {
    if (await this.userBadgeRepository.findOne({ where: { userId, badgeId } })) return null;
    const badge = await this.badgeRepository.findOne({ where: { id: badgeId } });
    if (!badge) return null;
    const userBadge = await this.userBadgeRepository.save(
      this.userBadgeRepository.create({ userId, badgeId }),
    );
    if (badge.pointsReward > 0) await this.awardPoints(userId, badge.pointsReward);
    return userBadge;
  }

  async getLeaderboard(limit = 20): Promise<UserPoints[]> {
    return this.pointsRepository.find({
      relations: ['user'],
      order: { totalPoints: 'DESC' },
      take: limit,
    });
  }

  async getMonthlyLeaderboard(limit = 20): Promise<UserPoints[]> {
    return this.pointsRepository.find({
      relations: ['user'],
      order: { monthlyPoints: 'DESC' },
      take: limit,
    });
  }

  private calculateLevel(totalPoints: number): UserLevel {
    const levels = Object.entries(LEVEL_THRESHOLDS).reverse() as [UserLevel, number][];
    for (const [level, threshold] of levels) {
      if (totalPoints >= threshold) return level;
    }
    return UserLevel.NEWCOMER;
  }
}
