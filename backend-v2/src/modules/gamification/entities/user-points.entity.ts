import {
  Entity, PrimaryColumn, Column, UpdateDateColumn,
  ManyToOne, JoinColumn,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';

export enum UserLevel {
  NEWCOMER = 'newcomer',
  STUDENT = 'student',
  ACTIVE_STUDENT = 'active_student',
  CAMPUS_REGULAR = 'campus_regular',
  CAMPUS_STAR = 'campus_star',
  CAMPUS_LEGEND = 'campus_legend',
}

@Entity('user_points')
export class UserPoints {
  @PrimaryColumn({ name: 'user_id' })
  userId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: User;

  @Column({ name: 'total_points', default: 0 })
  totalPoints: number;

  @Column({ name: 'monthly_points', default: 0 })
  monthlyPoints: number;

  @Column({ type: 'enum', enum: UserLevel, default: UserLevel.NEWCOMER })
  level: UserLevel;

  @Column({ name: 'posts_created', default: 0 })
  postsCreated: number;

  @Column({ name: 'comments_made', default: 0 })
  commentsMade: number;

  @Column({ name: 'events_attended', default: 0 })
  eventsAttended: number;

  @Column({ name: 'helpful_answers', default: 0 })
  helpfulAnswers: number;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
