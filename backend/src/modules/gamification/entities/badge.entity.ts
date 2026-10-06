import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn,
} from 'typeorm';

export enum BadgeCategory {
  SOCIAL = 'social',
  ACADEMIC = 'academic',
  ACCOMMODATION = 'accommodation',
  EVENTS = 'events',
  MARKETPLACE = 'marketplace',
  SPECIAL = 'special',
}

@Entity('badges')
export class Badge {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ unique: true, length: 100 })
  name: string;

  @Column({ length: 255 })
  description: string;

  @Column({ name: 'icon_url', nullable: true, type: 'text' })
  iconUrl: string;

  @Column({ type: 'enum', enum: BadgeCategory, default: BadgeCategory.SPECIAL })
  category: BadgeCategory;

  @Column({ name: 'points_reward', default: 0 })
  pointsReward: number;

  @Column({ name: 'criteria_type', length: 100 })
  criteriaType: string;

  @Column({ name: 'criteria_value', type: 'int', default: 1 })
  criteriaValue: number;

  @Column({ name: 'is_active', default: true })
  isActive: boolean;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}
