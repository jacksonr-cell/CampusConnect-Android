import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn,
  ManyToOne, JoinColumn, Index,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { Event } from './event.entity';

export enum AttendeeStatus {
  GOING = 'going',
  MAYBE = 'maybe',
  NOT_GOING = 'not_going',
}

@Entity('event_attendees')
@Index(['eventId', 'userId'], { unique: true })
export class EventAttendee {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'event_id' })
  eventId: string;

  @ManyToOne(() => Event, (e) => e.attendees, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'event_id' })
  event: Event;

  @Column({ name: 'user_id' })
  userId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: User;

  @Column({ type: 'enum', enum: AttendeeStatus, default: AttendeeStatus.GOING })
  status: AttendeeStatus;

  @Column({ name: 'checked_in_at', nullable: true, type: 'timestamp' })
  checkedInAt: Date;

  @Column({ name: 'qr_code', nullable: true, type: 'text' })
  qrCode: string;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}
