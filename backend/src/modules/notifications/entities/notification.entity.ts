import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn,
  ManyToOne, JoinColumn, Index,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';

export enum NotificationType {
  POST_LIKE = 'post_like',
  POST_COMMENT = 'post_comment',
  COMMENT_REPLY = 'comment_reply',
  FOLLOW = 'follow',
  EVENT_REMINDER = 'event_reminder',
  EVENT_INVITE = 'event_invite',
  BOOKING_UPDATE = 'booking_update',
  BOOKING_INQUIRY = 'booking_inquiry',
  MESSAGE = 'message',
  CLUB_INVITE = 'club_invite',
  MARKETPLACE_INTEREST = 'marketplace_interest',
  BADGE_EARNED = 'badge_earned',
  SYSTEM = 'system',
}

@Entity('notifications')
@Index(['userId'])
@Index(['isRead'])
@Index(['createdAt'])
export class Notification {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'user_id' })
  userId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: User;

  @Column({ name: 'actor_id', nullable: true })
  actorId: string;

  @ManyToOne(() => User, { nullable: true, onDelete: 'SET NULL' })
  @JoinColumn({ name: 'actor_id' })
  actor: User;

  @Column({ type: 'enum', enum: NotificationType })
  type: NotificationType;

  @Column({ length: 255 })
  title: string;

  @Column({ type: 'text' })
  body: string;

  @Column({ type: 'jsonb', nullable: true })
  data: Record<string, any>;

  @Column({ name: 'is_read', default: false })
  isRead: boolean;

  @Column({ name: 'read_at', nullable: true, type: 'timestamp' })
  readAt: Date;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}
