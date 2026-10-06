import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn,
  UpdateDateColumn, ManyToOne, OneToMany, JoinColumn, Index,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { ClubMember } from './club-member.entity';

export enum ClubCategory {
  ACADEMIC = 'academic',
  SPORTS = 'sports',
  ARTS = 'arts',
  TECH = 'tech',
  SOCIAL = 'social',
  RELIGIOUS = 'religious',
  CAREER = 'career',
  VOLUNTEER = 'volunteer',
  OTHER = 'other',
}

@Entity('clubs')
@Index(['university'])
export class Club {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ length: 150 })
  name: string;

  @Column({ type: 'text', nullable: true })
  description: string;

  @Column({ type: 'enum', enum: ClubCategory, default: ClubCategory.OTHER })
  category: ClubCategory;

  @Column({ name: 'logo_url', nullable: true, type: 'text' })
  logoUrl: string;

  @Column({ name: 'banner_url', nullable: true, type: 'text' })
  bannerUrl: string;

  @Column({ nullable: true, length: 200 })
  university: string;

  @Column({ name: 'is_verified', default: false })
  isVerified: boolean;

  @Column({ name: 'is_active', default: true })
  isActive: boolean;

  @Column({ name: 'members_count', default: 0 })
  membersCount: number;

  @Column({ name: 'created_by' })
  createdBy: string;

  @ManyToOne(() => User, { onDelete: 'SET NULL', nullable: true })
  @JoinColumn({ name: 'created_by' })
  creator: User;

  @OneToMany(() => ClubMember, (m) => m.club)
  members: ClubMember[];

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
