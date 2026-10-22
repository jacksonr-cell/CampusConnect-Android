import { Injectable, Logger } from '@nestjs/common';
import { InjectQueue } from '@nestjs/bullmq';
import { Queue } from 'bullmq';
import { QUEUE_NAMES, JOB_NAMES, QUEUE_DEFAULTS } from './constants/queue.constants';
import { NotificationType } from '../notifications/entities/notification.entity';

@Injectable()
export class QueueService {
  private readonly logger = new Logger(QueueService.name);

  constructor(
    @InjectQueue(QUEUE_NAMES.EMAIL)
    private readonly emailQueue: Queue,
    @InjectQueue(QUEUE_NAMES.NOTIFICATIONS)
    private readonly notificationQueue: Queue,
    @InjectQueue(QUEUE_NAMES.MEDIA)
    private readonly mediaQueue: Queue,
    @InjectQueue(QUEUE_NAMES.CLEANUP)
    private readonly cleanupQueue: Queue,
  ) {}

  // ── Email jobs ────────────────────────────────────────────────────────────

  async sendVerificationEmail(to: string, firstName: string, token: string) {
    return this.emailQueue.add(
      JOB_NAMES.SEND_VERIFICATION_EMAIL,
      { to, firstName, token },
      { ...QUEUE_DEFAULTS, priority: 1 },
    );
  }

  async sendPasswordResetEmail(to: string, firstName: string, token: string) {
    return this.emailQueue.add(
      JOB_NAMES.SEND_PASSWORD_RESET,
      { to, firstName, token },
      { ...QUEUE_DEFAULTS, priority: 1 },
    );
  }

  async sendWelcomeEmail(to: string, firstName: string) {
    return this.emailQueue.add(
      JOB_NAMES.SEND_WELCOME_EMAIL,
      { to, firstName },
      { ...QUEUE_DEFAULTS, delay: 2000 },
    );
  }

  async sendNotificationEmail(to: string, subject: string, body: string) {
    return this.emailQueue.add(
      JOB_NAMES.SEND_NOTIFICATION_EMAIL,
      { to, subject, body },
      QUEUE_DEFAULTS,
    );
  }

  async sendBookingUpdateEmail(
    to: string,
    firstName: string,
    propertyTitle: string,
    status: string,
    message?: string,
  ) {
    return this.emailQueue.add(
      JOB_NAMES.SEND_BOOKING_UPDATE,
      { to, firstName, propertyTitle, status, message },
      { ...QUEUE_DEFAULTS, priority: 2 },
    );
  }

  async sendEventReminderEmail(
    to: string,
    firstName: string,
    eventTitle: string,
    startTime: Date,
    location?: string,
  ) {
    return this.emailQueue.add(
      JOB_NAMES.SEND_EVENT_REMINDER,
      { to, firstName, eventTitle, startTime, location },
      QUEUE_DEFAULTS,
    );
  }

  // ── Notification jobs ─────────────────────────────────────────────────────

  async createInAppNotification(data: {
    userId   : string;
    actorId? : string;
    type     : NotificationType;
    title    : string;
    body     : string;
    data?    : Record<string, any>;
  }) {
    return this.notificationQueue.add(JOB_NAMES.IN_APP_NOTIFICATION, data, QUEUE_DEFAULTS);
  }

  async createBulkNotification(data: {
    userIds : string[];
    type    : NotificationType;
    title   : string;
    body    : string;
    data?   : Record<string, any>;
  }) {
    return this.notificationQueue.add(JOB_NAMES.BULK_NOTIFICATION, data, {
      ...QUEUE_DEFAULTS,
      attempts: 2,
    });
  }

  async sendPushNotification(userId: string, title: string, body: string, data?: Record<string, any>) {
    return this.notificationQueue.add(
      JOB_NAMES.PUSH_NOTIFICATION,
      { userId, title, body, data },
      QUEUE_DEFAULTS,
    );
  }

  // ── Media jobs ────────────────────────────────────────────────────────────

  async processAvatar(userId: string, imageUrl: string) {
    return this.mediaQueue.add(
      JOB_NAMES.PROCESS_AVATAR,
      { userId, imageUrl },
      QUEUE_DEFAULTS,
    );
  }

  async processPropertyImage(propertyId: string, imageUrl: string) {
    return this.mediaQueue.add(
      JOB_NAMES.PROCESS_PROPERTY_IMAGE,
      { propertyId, imageUrl },
      QUEUE_DEFAULTS,
    );
  }

  // ── Cleanup jobs ──────────────────────────────────────────────────────────

  async scheduleCleanup() {
    await Promise.all([
      this.cleanupQueue.add(JOB_NAMES.PURGE_EXPIRED_TOKENS, {}, {
        repeat : { pattern: '0 3 * * *' }, // daily at 3 AM
        jobId  : 'purge-tokens-recurring',
      }),
      this.cleanupQueue.add(JOB_NAMES.PURGE_OLD_NOTIFICATIONS, {}, {
        repeat : { pattern: '0 4 * * 0' }, // weekly Sunday at 4 AM
        jobId  : 'purge-notifications-recurring',
      }),
    ]);
    this.logger.log('Recurring cleanup jobs scheduled');
  }

  // ── Queue stats ───────────────────────────────────────────────────────────

  async getQueueStats() {
    const [emailCounts, notifCounts, mediaCounts] = await Promise.all([
      this.emailQueue.getJobCounts(),
      this.notificationQueue.getJobCounts(),
      this.mediaQueue.getJobCounts(),
    ]);
    return {
      email        : emailCounts,
      notifications: notifCounts,
      media        : mediaCounts,
    };
  }
}
