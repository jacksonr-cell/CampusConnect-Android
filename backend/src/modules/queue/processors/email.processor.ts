import { Processor, WorkerHost, OnWorkerEvent } from '@nestjs/bullmq';
import { Logger } from '@nestjs/common';
import { Job } from 'bullmq';
import { QUEUE_NAMES, JOB_NAMES } from '../constants/queue.constants';
import { MailService } from '../../mail/mail.service';

@Processor(QUEUE_NAMES.EMAIL, {
  concurrency: 5,
})
export class EmailProcessor extends WorkerHost {
  private readonly logger = new Logger(EmailProcessor.name);

  constructor(private readonly mailService: MailService) {
    super();
  }

  async process(job: Job): Promise<void> {
    this.logger.debug(`Processing email job: ${job.name} [${job.id}]`);

    switch (job.name) {
      case JOB_NAMES.SEND_VERIFICATION_EMAIL:
        await this.mailService.sendVerificationEmail(
          job.data.to,
          job.data.firstName,
          job.data.token,
        );
        break;

      case JOB_NAMES.SEND_PASSWORD_RESET:
        await this.mailService.sendPasswordResetEmail(
          job.data.to,
          job.data.firstName,
          job.data.token,
        );
        break;

      case JOB_NAMES.SEND_WELCOME_EMAIL:
        await this.mailService.sendWelcomeEmail(
          job.data.to,
          job.data.firstName,
        );
        break;

      case JOB_NAMES.SEND_NOTIFICATION_EMAIL:
        await this.mailService.sendNotificationEmail(
          job.data.to,
          job.data.subject,
          job.data.body,
        );
        break;

      case JOB_NAMES.SEND_BOOKING_UPDATE:
        await this.mailService.sendBookingUpdateEmail(
          job.data.to,
          job.data.firstName,
          job.data.propertyTitle,
          job.data.status,
          job.data.message,
        );
        break;

      case JOB_NAMES.SEND_EVENT_REMINDER:
        await this.mailService.sendEventReminderEmail(
          job.data.to,
          job.data.firstName,
          job.data.eventTitle,
          job.data.startTime,
          job.data.location,
        );
        break;

      default:
        this.logger.warn(`Unknown email job type: ${job.name}`);
    }
  }

  @OnWorkerEvent('completed')
  onCompleted(job: Job) {
    this.logger.debug(`Email job completed: ${job.name} [${job.id}]`);
  }

  @OnWorkerEvent('failed')
  onFailed(job: Job, error: Error) {
    this.logger.error(
      `Email job failed: ${job.name} [${job.id}] attempt ${job.attemptsMade}`,
      error.stack,
    );
  }
}
