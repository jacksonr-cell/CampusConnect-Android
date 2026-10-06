import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn,
  UpdateDateColumn, ManyToOne, OneToMany, JoinColumn, Index,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { Comment } from './comment.entity';
import { Reaction } from './reaction.entity';
import { Community } from './community.entity';

export enum PostType {
  TEXT = 'text',
  IMAGE = 'image',
  VIDEO = 'video',
  POLL = 'poll',
}

export enum PostVisibility {
  PUBLIC = 'public',
  FOLLOWERS = 'followers',
  COMMUNITY = 'community',
}

@Entity('posts')
@Index(['userId'])
@Index(['communityId'])
@Index(['createdAt'])
export class Post {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'user_id' })
  userId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE', eager: false })
  @JoinColumn({ name: 'user_id' })
  user: User;

  @Column({ type: 'text', nullable: true })
  content: string;

  @Column({ type: 'enum', enum: PostType, default: PostType.TEXT, name: 'post_type' })
  postType: PostType;

  @Column({ type: 'jsonb', nullable: true, name: 'media_urls', default: [] })
  mediaUrls: string[];

  @Column({ name: 'community_id', nullable: true })
  communityId: string;

  @ManyToOne(() => Community, { nullable: true, onDelete: 'SET NULL' })
  @JoinColumn({ name: 'community_id' })
  community: Community;

  @Column({ type: 'enum', enum: PostVisibility, default: PostVisibility.PUBLIC, name: 'visibility' })
  visibility: PostVisibility;

  @Column({ name: 'is_pinned', default: false })
  isPinned: boolean;

  @Column({ name: 'is_archived', default: false })
  isArchived: boolean;

  @Column({ name: 'likes_count', default: 0 })
  likesCount: number;

  @Column({ name: 'comments_count', default: 0 })
  commentsCount: number;

  @Column({ name: 'shares_count', default: 0 })
  sharesCount: number;

  @Column({ name: 'views_count', default: 0 })
  viewsCount: number;

  @OneToMany(() => Comment, (c) => c.post)
  comments: Comment[];

  @OneToMany(() => Reaction, (r) => r.post)
  reactions: Reaction[];

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
