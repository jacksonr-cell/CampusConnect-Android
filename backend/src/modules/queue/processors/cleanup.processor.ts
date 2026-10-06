import { Processor, WorkerHost } from '@nestjs/bullmq';
import { Logger } from '@nestjs/common';
import { Job } from 'bullmq';
import { InjectRepository } from '@nestjs/typeorm';
import { LessThan, Repository } from 'typeorm';
import { QUEUE_NAMES, JOB_NAMES } from '../constants/queue.constants';
import { RefreshToken } from '../../users/entities/refresh-token.entity';
import { Notification } from '../../notifications/entities/notification.entity';

@Processor(QUEUE_NAMES.CLEANUP, { concurrency: 1 })
export class CleanupProcessor extends WorkerHost {
  private readonly logger = new Logger(CleanupProcessor.name);

  constructor(
    @InjectRepository(RefreshToken)
    private readonly refreshTokenRepository: Repository<RefreshToken>,
    @InjectRepository(Notification)
    private readonly notificationRepository: Repository<Notification>,
  ) {
    super();
  }

  async process(job: Job): Promise<void> {
    switch (job.name) {
      case JOB_NAMES.PURGE_EXPIRED_TOKENS:
        await this.purgeExpiredTokens();
        break;
      case JOB_NAMES.PURGE_OLD_NOTIFICATIONS:
        await this.purgeOldNotifications();
        break;
      default:
        this.logger.warn(`Unknown cleanup job: ${job.name}`);
    }
  }

  private async purgeExpiredTokens() {
    const result = await this.refreshTokenRepository.delete({
      expiresAt: LessThan(new Date()),
    });
    this.logger.log(`Purged ${result.affected} expired refresh tokens`);
  }

  private async purgeOldNotifications() {
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() - 90);
    const result = await this.notificationRepository.delete({
      createdAt: LessThan(cutoff),
      isRead   : true,
    });
    this.logger.log(`Purged ${result.affected} old read notifications`);
  }
}
