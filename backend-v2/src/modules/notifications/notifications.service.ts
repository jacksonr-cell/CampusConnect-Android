import { Injectable, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Notification, NotificationType } from './entities/notification.entity';
import { PaginationDto, paginate, PaginatedResult } from '../../common/dto/pagination.dto';
import { CampusConnectGateway } from '../websocket/websocket.gateway';

export interface CreateNotificationDto {
  userId: string;
  actorId?: string;
  type: NotificationType;
  title: string;
  body: string;
  data?: Record<string, any>;
}

@Injectable()
export class NotificationsService {
  private readonly logger = new Logger(NotificationsService.name);

  constructor(
    @InjectRepository(Notification)
    private readonly notificationRepository: Repository<Notification>,
    private readonly gateway: CampusConnectGateway,
  ) {}

  async create(dto: CreateNotificationDto): Promise<Notification> {
    const notification = await this.notificationRepository.save(
      this.notificationRepository.create(dto),
    );

    // Push in real time to the recipient if they're currently connected —
    // this is what makes notifications "live" rather than pull-only via
    // REST. Delivery is best-effort: if the socket emit fails for any
    // reason, the notification still exists and will show up next time
    // the client polls GET /notifications, so a failure here is logged,
    // not thrown.
    try {
      this.gateway.emitToUser(dto.userId, 'notification', {
        id: notification.id,
        type: notification.type,
        title: notification.title,
        body: notification.body,
        data: notification.data,
        createdAt: notification.createdAt,
      });
    } catch (error) {
      this.logger.warn(
        `Failed to push real-time notification to user ${dto.userId}: ${(error as Error).message}`,
      );
    }

    return notification;
  }

  async getUserNotifications(
    userId: string,
    pagination: PaginationDto,
  ): Promise<PaginatedResult<Notification>> {
    const { page = 1, limit = 20 } = pagination;
    const [notifications, total] = await this.notificationRepository.findAndCount({
      where: { userId },
      relations: ['actor'],
      order: { createdAt: 'DESC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return paginate(notifications, total, page, limit);
  }

  async markAsRead(notificationId: string, userId: string): Promise<void> {
    await this.notificationRepository.update(
      { id: notificationId, userId },
      { isRead: true, readAt: new Date() },
    );
  }

  async markAllAsRead(userId: string): Promise<void> {
    await this.notificationRepository.update(
      { userId, isRead: false },
      { isRead: true, readAt: new Date() },
    );
  }

  async getUnreadCount(userId: string): Promise<number> {
    return this.notificationRepository.count({ where: { userId, isRead: false } });
  }
}

