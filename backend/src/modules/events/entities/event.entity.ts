import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn,
  UpdateDateColumn, ManyToOne, OneToMany, JoinColumn, Index,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { Club } from '../../clubs/entities/club.entity';
import { EventAttendee } from './event-attendee.entity';

export enum EventCategory {
  ACADEMIC = 'academic',
  SOCIAL = 'social',
  SPORTS = 'sports',
  CULTURE = 'culture',
  CAREER = 'career',
  TECH = 'tech',
  ENTERTAINMENT = 'entertainment',
  VOLUNTEER = 'volunteer',
  OTHER = 'other',
}

@Entity('events')
@Index(['organizerId'])
@Index(['startTime'])
@Index(['category'])
export class Event {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'organizer_id' })
  organizerId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE', eager: false })
  @JoinColumn({ name: 'organizer_id' })
  organizer: User;

  @Column({ name: 'club_id', nullable: true })
  clubId: string;

  @ManyToOne(() => Club, { nullable: true, onDelete: 'SET NULL' })
  @JoinColumn({ name: 'club_id' })
  club: Club;

  @Column({ length: 255 })
  title: string;

  @Column({ type: 'text' })
  description: string;

  @Column({ name: 'cover_image', nullable: true, type: 'text' })
  coverImage: string;

  @Column({ nullable: true, length: 500 })
  location: string;

  @Column({ nullable: true, type: 'decimal', precision: 10, scale: 8, name: 'venue_latitude' })
  venueLatitude: number;

  @Column({ nullable: true, type: 'decimal', precision: 11, scale: 8, name: 'venue_longitude' })
  venueLongitude: number;

  @Column({ name: 'start_time', type: 'timestamp' })
  startTime: Date;

  @Column({ name: 'end_time', type: 'timestamp', nullable: true })
  endTime: Date;

  @Column({ type: 'enum', enum: EventCategory, default: EventCategory.OTHER })
  category: EventCategory;

  @Column({ name: 'is_ticketed', default: false })
  isTicketed: boolean;

  @Column({ name: 'ticket_price', type: 'decimal', precision: 10, scale: 2, nullable: true })
  ticketPrice: number;

  @Column({ name: 'max_attendees', type: 'int', nullable: true })
  maxAttendees: number;

  @Column({ name: 'attendees_count', default: 0 })
  attendeesCount: number;

  @Column({ name: 'is_cancelled', default: false })
  isCancelled: boolean;

  @Column({ name: 'is_featured', default: false })
  isFeatured: boolean;

  @Column({ nullable: true, length: 200 })
  university: string;

  @OneToMany(() => EventAttendee, (a) => a.event)
  attendees: EventAttendee[];

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
