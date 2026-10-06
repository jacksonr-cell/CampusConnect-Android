import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn,
  UpdateDateColumn, ManyToOne, JoinColumn, Index,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';

export enum ReportReason {
  SPAM               = 'spam',
  HARASSMENT         = 'harassment',
  HATE_SPEECH        = 'hate_speech',
  MISINFORMATION     = 'misinformation',
  INAPPROPRIATE      = 'inappropriate',
  SCAM               = 'scam',
  INTELLECTUAL_PROPERTY = 'intellectual_property',
  OTHER              = 'other',
}

export enum ReportStatus {
  PENDING   = 'pending',
  REVIEWED  = 'reviewed',
  ACTIONED  = 'actioned',
  DISMISSED = 'dismissed',
}

export enum ReportTargetType {
  POST    = 'post',
  COMMENT = 'comment',
  USER    = 'user',
  LISTING = 'listing',
  PROPERTY= 'property',
  EVENT   = 'event',
  MESSAGE = 'message',
}

@Entity('reports')
@Index(['status'])
@Index(['targetType', 'targetId'])
@Index(['reporterId'])
export class Report {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'reporter_id' })
  reporterId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'reporter_id' })
  reporter: User;

  @Column({ name: 'target_type', type: 'enum', enum: ReportTargetType })
  targetType: ReportTargetType;

  @Column({ name: 'target_id' })
  targetId: string;

  @Column({ type: 'enum', enum: ReportReason })
  reason: ReportReason;

  @Column({ type: 'text', nullable: true })
  description: string;

  @Column({ type: 'enum', enum: ReportStatus, default: ReportStatus.PENDING })
  status: ReportStatus;

  @Column({ name: 'reviewed_by', nullable: true })
  reviewedBy: string;

  @ManyToOne(() => User, { nullable: true, onDelete: 'SET NULL' })
  @JoinColumn({ name: 'reviewed_by' })
  reviewer: User;

  @Column({ name: 'moderator_note', nullable: true, type: 'text' })
  moderatorNote: string;

  @Column({ name: 'reviewed_at', nullable: true, type: 'timestamp' })
  reviewedAt: Date;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
