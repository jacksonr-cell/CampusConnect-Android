import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn,
  ManyToOne, JoinColumn, Index,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { Club } from './club.entity';

export enum ClubMemberRole {
  MEMBER = 'member',
  MODERATOR = 'moderator',
  LEADER = 'leader',
}

@Entity('club_members')
@Index(['clubId', 'userId'], { unique: true })
export class ClubMember {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'club_id' })
  clubId: string;

  @ManyToOne(() => Club, (c) => c.members, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'club_id' })
  club: Club;

  @Column({ name: 'user_id' })
  userId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: User;

  @Column({ type: 'enum', enum: ClubMemberRole, default: ClubMemberRole.MEMBER })
  role: ClubMemberRole;

  @CreateDateColumn({ name: 'joined_at' })
  joinedAt: Date;
}
