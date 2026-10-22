import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn,
  UpdateDateColumn, OneToMany, Index,
} from 'typeorm';
import { Exclude } from 'class-transformer';
import { RefreshToken } from './refresh-token.entity';
import { UserFollow } from './user-follow.entity';

export enum UserRole {
  STUDENT = 'student',
  CLUB_LEADER = 'club_leader',
  LANDLORD = 'landlord',
  ADMIN = 'admin',
}

@Entity('users')
export class User {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Index({ unique: true })
  @Column({ unique: true, length: 255 })
  email: string;

  @Index({ unique: true })
  @Column({ unique: true, length: 50 })
  username: string;

  @Column({ name: 'password_hash', length: 255 })
  @Exclude()
  passwordHash: string;

  @Column({ name: 'first_name', length: 100 })
  firstName: string;

  @Column({ name: 'last_name', length: 100 })
  lastName: string;

  @Column({ name: 'avatar_url', nullable: true, type: 'text' })
  avatarUrl: string;

  @Column({ name: 'cover_image_url', nullable: true, type: 'text' })
  coverImageUrl: string;

  @Column({ type: 'text', nullable: true })
  bio: string;

  @Column({ nullable: true, length: 200 })
  university: string;

  @Column({ name: 'student_id', nullable: true, length: 50 })
  studentId: string;

  @Column({ name: 'course_of_study', nullable: true, length: 200 })
  courseOfStudy: string;

  @Column({ name: 'year_of_study', nullable: true, type: 'int' })
  yearOfStudy: number;

  @Column({ type: 'enum', enum: UserRole, default: UserRole.STUDENT })
  role: UserRole;

  @Column({ name: 'is_verified', default: false })
  isVerified: boolean;

  @Column({ name: 'is_email_verified', default: false })
  isEmailVerified: boolean;

  @Column({ name: 'is_active', default: true })
  isActive: boolean;

  @Column({ name: 'two_factor_enabled', default: false })
  twoFactorEnabled: boolean;

  @Column({ name: 'reputation_points', default: 0 })
  reputationPoints: number;

  @Column({ name: 'followers_count', default: 0 })
  followersCount: number;

  @Column({ name: 'following_count', default: 0 })
  followingCount: number;

  @Column({ name: 'posts_count', default: 0 })
  postsCount: number;

  @Column({ name: 'last_seen_at', nullable: true, type: 'timestamp' })
  lastSeenAt: Date;

  @Column({ name: 'google_id', nullable: true, unique: true })
  googleId: string;

  @OneToMany(() => RefreshToken, (token) => token.user, { cascade: true })
  @Exclude()
  refreshTokens: RefreshToken[];

  @OneToMany(() => UserFollow, (follow) => follow.follower)
  following: UserFollow[];

  @OneToMany(() => UserFollow, (follow) => follow.following)
  followers: UserFollow[];

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
