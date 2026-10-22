import {
  Entity, PrimaryColumn, Column, CreateDateColumn, UpdateDateColumn,
  ManyToOne, JoinColumn,
} from 'typeorm';
import { Exclude } from 'class-transformer';
import { User } from '../../users/entities/user.entity';

@Entity('mfa_secrets')
export class MfaSecret {
  @PrimaryColumn({ name: 'user_id' })
  userId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: User;

  /** AES-256-GCM encrypted in production. Keep MFA_ENCRYPTION_KEY stable for the lifetime of these records. */
  @Column({ name: 'secret', length: 255 })
  @Exclude()
  secret: string;

  @Column({ name: 'is_enabled', default: false })
  isEnabled: boolean;

  /** One-time backup codes, hashed. Consumed on use. */
  @Column({ name: 'backup_codes', type: 'jsonb', default: [] })
  @Exclude()
  backupCodes: string[];

  @Column({ name: 'verified_at', nullable: true, type: 'timestamp' })
  verifiedAt: Date;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
