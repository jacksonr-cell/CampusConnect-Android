import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn,
  UpdateDateColumn, ManyToOne, JoinColumn, Index,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';

@Entity('user_sessions')
@Index(['userId'])
export class UserSession {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'user_id' })
  userId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: User;

  /** Links to the refresh_tokens row backing this session. */
  @Column({ name: 'refresh_token_id', nullable: true })
  refreshTokenId: string;

  @Column({ name: 'device_name', nullable: true, length: 150 })
  deviceName: string;

  @Column({ nullable: true, length: 100 })
  browser: string;

  @Column({ nullable: true, length: 100 })
  os: string;

  @Column({ name: 'ip_address', nullable: true, length: 45 })
  ipAddress: string;

  @Column({ nullable: true, length: 100 })
  city: string;

  @Column({ nullable: true, length: 100 })
  country: string;

  @Column({ name: 'is_current', default: false })
  isCurrent: boolean;

  @Column({ name: 'last_active_at', type: 'timestamp' })
  lastActiveAt: Date;

  @Column({ name: 'revoked_at', nullable: true, type: 'timestamp' })
  revokedAt: Date;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
