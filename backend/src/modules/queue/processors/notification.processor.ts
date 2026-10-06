import { Processor, WorkerHost, OnWorkerEvent } from '@nestjs/bullmq';
import { Logger } from '@nestjs/common';
import { Job } from 'bullmq';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { QUEUE_NAMES, JOB_NAMES } from '../constants/queue.constants';
import { Notification, NotificationType } from '../../notifications/entities/notification.entity';
import { CampusConnectGateway } from '../../websocket/websocket.gateway';

@Processor(QUEUE_NAMES.NOTIFICATIONS, { concurrency: 10 })
export class NotificationProcessor extends WorkerHost {
  private readonly logger = new Logger(NotificationProcessor.name);

  constructor(
    @InjectRepository(Notification)
    private readonly notificationRepository: Repository<Notification>,
    private readonly gateway: CampusConnectGateway,
  ) {
    super();
  }

  async process(job: Job): Promise<void> {
    switch (job.name) {
      case JOB_NAMES.IN_APP_NOTIFICATION:
        await this.createInAppNotification(job.data);
        break;

      case JOB_NAMES.BULK_NOTIFICATION:
        await this.createBulkNotifications(job.data);
        break;

      case JOB_NAMES.PUSH_NOTIFICATION:
        // TODO: Integrate FCM / APNs push delivery
        this.logger.debug(`Push notification queued for user: ${job.data.userId}`);
        break;

      default:
        this.logger.warn(`Unknown notification job: ${job.name}`);
    }
  }

  private async createInAppNotification(data: {
    userId: string;
    actorId?: string;
    type: NotificationType;
    title: string;
    body: string;
    data?: Record<string, any>;
  }) {
    const notification = await this.notificationRepository.save(
      this.notificationRepository.create(data),
    );
    this.pushRealtime(notification);
  }

  private async createBulkNotifications(data: {
    userIds: string[];
    type: NotificationType;
    title: string;
    body: string;
    data?: Record<string, any>;
  }) {
    const { userIds, ...rest } = data;
    const notifications = userIds.map((userId) =>
      this.notificationRepository.create({ userId, ...rest }),
    );
    const saved = await this.notificationRepository.save(notifications, { chunk: 500 });
    saved.forEach((n) => this.pushRealtime(n));
    this.logger.debug(`Bulk notification created for ${userIds.length} users`);
  }

  /** Best-effort real-time push — the notification is already durably saved either way. */
  private pushRealtime(notification: Notification) {
    try {
      this.gateway.emitToUser(notification.userId, 'notification', {
        id: notification.id,
        type: notification.type,
        title: notification.title,
        body: notification.body,
        data: notification.data,
        createdAt: notification.createdAt,
      });
    } catch (error) {
      this.logger.warn(
        `Failed to push real-time notification to user ${notification.userId}: ${(error as Error).message}`,
      );
    }
  }

  @OnWorkerEvent('failed')
  onFailed(job: Job, error: Error) {
    this.logger.error(`Notification job failed: ${job.name} [${job.id}]`, error.stack);
  }
}
