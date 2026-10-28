import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as nodemailer from 'nodemailer';
import {
  verificationEmailTemplate,
  passwordResetTemplate,
  welcomeEmailTemplate,
  bookingUpdateTemplate,
  eventReminderTemplate,
} from './templates/email.templates';

@Injectable()
export class MailService {
  private readonly logger     = new Logger(MailService.name);
  private readonly transporter: nodemailer.Transporter;
  private readonly from       : string;
  private readonly appUrl     : string;

  constructor(private readonly configService: ConfigService) {
    this.from   = configService.get<string>('mail.from');
    this.appUrl = configService.get<string>('mail.appUrl');

    this.transporter = nodemailer.createTransport({
      host   : configService.get<string>('mail.host'),
      port   : configService.get<number>('mail.port'),
      secure : configService.get<boolean>('mail.secure'),
      auth   : {
        user: configService.get<string>('mail.user'),
        pass: configService.get<string>('mail.pass'),
      },
    });
  }

  async sendVerificationEmail(to: string, firstName: string, token: string): Promise<void> {
    await this.send({
      to,
      subject: 'Verify your CampusConnect email',
      html   : verificationEmailTemplate(firstName, token, this.appUrl),
    });
  }

  async sendPasswordResetEmail(to: string, firstName: string, token: string): Promise<void> {
    await this.send({
      to,
      subject: 'Reset your CampusConnect password',
      html   : passwordResetTemplate(firstName, token, this.appUrl),
    });
  }

  async sendWelcomeEmail(to: string, firstName: string): Promise<void> {
    await this.send({
      to,
      subject: 'Welcome to CampusConnect 🎉',
      html   : welcomeEmailTemplate(firstName, this.appUrl),
    });
  }

  async sendNotificationEmail(to: string, subject: string, body: string): Promise<void> {
    await this.send({ to, subject, html: body });
  }

  async sendBookingUpdateEmail(
    to: string,
    firstName: string,
    propertyTitle: string,
    status: string,
    message?: string,
  ): Promise<void> {
    await this.send({
      to,
      subject: `Booking Update — ${propertyTitle}`,
      html   : bookingUpdateTemplate(firstName, propertyTitle, status, message, this.appUrl),
    });
  }

  async sendEventReminderEmail(
    to: string,
    firstName: string,
    eventTitle: string,
    startTime: Date,
    location?: string,
  ): Promise<void> {
    await this.send({
      to,
      subject: `Reminder: ${eventTitle}`,
      html   : eventReminderTemplate(firstName, eventTitle, startTime, location, this.appUrl),
    });
  }

  private async send(options: { to: string; subject: string; html: string }): Promise<void> {
    if (!this.configService.get<boolean>('mail.enabled', true)) {
      this.logger.warn(`Email disabled for this deployment; skipped: ${options.subject}`);
      return;
    }
    try {
      await this.transporter.sendMail({
        from   : this.from,
        ...options,
      });
      this.logger.debug(`Email sent to ${options.to}: ${options.subject}`);
    } catch (error) {
      this.logger.error(`Failed to send email to ${options.to}`, (error as Error).stack);
      throw error;
    }
  }

  /** Verify SMTP connection — used in health checks. */
  async verifyConnection(): Promise<boolean> {
    if (!this.configService.get<boolean>('mail.enabled', true)) return false;
    try {
      await this.transporter.verify();
      return true;
    } catch {
      return false;
    }
  }
}
