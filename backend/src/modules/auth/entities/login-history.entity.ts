import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn,
  ManyToOne, JoinColumn, Index,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';

export enum LoginStatus {
  SUCCESS           = 'success',
  FAILED_PASSWORD   = 'failed_password',
  FAILED_NOT_FOUND  = 'failed_not_found',
  FAILED_INACTIVE   = 'failed_inactive',
  FAILED_MFA        = 'failed_mfa',
}

@Entity('login_history')
@Index(['userId'])
@Index(['createdAt'])
export class LoginHistory {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'user_id', nullable: true })
  userId: string;

  @ManyToOne(() => User, { nullable: true, onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: User;

  @Column({ name: 'email_attempted', nullable: true, length: 255 })
  emailAttempted: string;

  @Column({ type: 'enum', enum: LoginStatus })
  status: LoginStatus;

  @Column({ name: 'ip_address', nullable: true, length: 45 })
  ipAddress: string;

  @Column({ name: 'user_agent', nullable: true, type: 'text' })
  userAgent: string;

  @Column({ nullable: true, length: 100 })
  device: string;

  @Column({ nullable: true, length: 100 })
  browser: string;

  @Column({ nullable: true, length: 100 })
  os: string;

  @Column({ nullable: true, length: 100 })
  city: string;

  @Column({ nullable: true, length: 100 })
  country: string;

  @Column({ name: 'is_suspicious', default: false })
  isSuspicious: boolean;

  @Column({ name: 'suspicious_reason', nullable: true, length: 255 })
  suspiciousReason: string;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}
