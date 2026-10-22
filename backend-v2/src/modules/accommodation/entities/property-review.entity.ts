import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn,
  UpdateDateColumn, ManyToOne, JoinColumn, Index,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { Property } from './property.entity';

@Entity('property_reviews')
@Index(['propertyId', 'reviewerId'], { unique: true })
export class PropertyReview {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'property_id' })
  propertyId: string;

  @ManyToOne(() => Property, (p) => p.reviews, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'property_id' })
  property: Property;

  @Column({ name: 'reviewer_id' })
  reviewerId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'reviewer_id' })
  reviewer: User;

  @Column({ type: 'int' })
  rating: number;

  @Column({ type: 'text', nullable: true })
  content: string;

  @Column({ name: 'rating_cleanliness', type: 'int', nullable: true })
  ratingCleanliness: number;

  @Column({ name: 'rating_value', type: 'int', nullable: true })
  ratingValue: number;

  @Column({ name: 'rating_location', type: 'int', nullable: true })
  ratingLocation: number;

  @Column({ name: 'rating_landlord', type: 'int', nullable: true })
  ratingLandlord: number;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
