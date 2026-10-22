import { Module } from '@nestjs/common';
import { BullModule } from '@nestjs/bullmq';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { QueueService } from './queue.service';
import { EmailProcessor } from './processors/email.processor';
import { NotificationProcessor } from './processors/notification.processor';
import { CleanupProcessor } from './processors/cleanup.processor';
import { QUEUE_NAMES } from './constants/queue.constants';
import { MailModule } from '../mail/mail.module';
import { WebSocketModule } from '../websocket/websocket.module';
import { RefreshToken } from '../users/entities/refresh-token.entity';
import { Notification } from '../notifications/entities/notification.entity';

@Module({
  imports: [
    BullModule.forRootAsync({
      imports    : [ConfigModule],
      useFactory : (configService: ConfigService) => ({
        connection: {
          host    : configService.get<string>('redis.host', 'localhost'),
          port    : configService.get<number>('redis.port', 6379),
          username: configService.get<string>('redis.username'),
          password: configService.get<string>('redis.password'),
          db      : configService.get<number>('redis.db', 0),
          tls     : configService.get('redis.tls'),
        },
        defaultJobOptions: {
          attempts         : 3,
          backoff          : { type: 'exponential', delay: 2000 },
          removeOnComplete : { count: 100 },
          removeOnFail     : { count: 500 },
        },
      }),
      inject: [ConfigService],
    }),
    BullModule.registerQueue(
      { name: QUEUE_NAMES.EMAIL },
      { name: QUEUE_NAMES.NOTIFICATIONS },
      { name: QUEUE_NAMES.MEDIA },
      { name: QUEUE_NAMES.CLEANUP },
    ),
    TypeOrmModule.forFeature([RefreshToken, Notification]),
    MailModule,
    WebSocketModule,
  ],
  providers : [QueueService, EmailProcessor, NotificationProcessor, CleanupProcessor],
  exports   : [QueueService, BullModule],
})
export class QueueModule {}
