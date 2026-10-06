import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn,
  UpdateDateColumn, ManyToOne, JoinColumn, Index,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';

export enum ListingCategory {
  TEXTBOOKS = 'textbooks',
  ELECTRONICS = 'electronics',
  FURNITURE = 'furniture',
  CLOTHING = 'clothing',
  ROOM_TAKEOVER = 'room_takeover',
  SERVICES = 'services',
  FOOD = 'food',
  OTHER = 'other',
}

export enum ListingCondition {
  NEW = 'new',
  LIKE_NEW = 'like_new',
  GOOD = 'good',
  FAIR = 'fair',
  POOR = 'poor',
}

export enum ListingStatus {
  ACTIVE = 'active',
  SOLD = 'sold',
  RESERVED = 'reserved',
  DELETED = 'deleted',
}

@Entity('marketplace_listings')
@Index(['sellerId'])
@Index(['category'])
@Index(['status'])
export class MarketplaceListing {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'seller_id' })
  sellerId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE', eager: false })
  @JoinColumn({ name: 'seller_id' })
  seller: User;

  @Column({ length: 255 })
  title: string;

  @Column({ type: 'text' })
  description: string;

  @Column({ type: 'enum', enum: ListingCategory, default: ListingCategory.OTHER })
  category: ListingCategory;

  @Column({ type: 'enum', enum: ListingCondition, nullable: true })
  condition: ListingCondition;

  @Column({ type: 'decimal', precision: 10, scale: 2 })
  price: number;

  @Column({ length: 10, default: 'KES' })
  currency: string;

  @Column({ name: 'is_negotiable', default: false })
  isNegotiable: boolean;

  @Column({ type: 'jsonb', nullable: true, name: 'image_urls', default: [] })
  imageUrls: string[];

  @Column({ type: 'enum', enum: ListingStatus, default: ListingStatus.ACTIVE })
  status: ListingStatus;

  @Column({ nullable: true, length: 200 })
  university: string;

  @Column({ name: 'views_count', default: 0 })
  viewsCount: number;

  @Column({ name: 'is_featured', default: false })
  isFeatured: boolean;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
