import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn,
  UpdateDateColumn, ManyToOne, OneToMany, JoinColumn, Index,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { PropertyImage } from './property-image.entity';
import { PropertyAmenity } from './property-amenity.entity';
import { PropertyReview } from './property-review.entity';
import { AccommodationBooking } from './booking.entity';

export enum PropertyType {
  HOSTEL = 'hostel',
  APARTMENT = 'apartment',
  STUDIO = 'studio',
  SHARED_ROOM = 'shared_room',
  PRIVATE_ROOM = 'private_room',
  BEDSITTER = 'bedsitter',
}

@Entity('properties')
@Index(['landlordId'])
@Index(['city'])
@Index(['isAvailable'])
export class Property {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'landlord_id' })
  landlordId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE', eager: false })
  @JoinColumn({ name: 'landlord_id' })
  landlord: User;

  @Column({ length: 255 })
  title: string;

  @Column({ type: 'text' })
  description: string;

  @Column({ type: 'enum', enum: PropertyType, name: 'property_type' })
  propertyType: PropertyType;

  @Column({ length: 500 })
  address: string;

  @Column({ length: 100 })
  city: string;

  @Column({ nullable: true, length: 100 })
  neighborhood: string;

  @Column({ nullable: true, length: 100 })
  county: string;

  @Column({ type: 'decimal', precision: 10, scale: 8, nullable: true })
  latitude: number;

  @Column({ type: 'decimal', precision: 11, scale: 8, nullable: true })
  longitude: number;

  @Column({ name: 'price_per_month', type: 'decimal', precision: 10, scale: 2 })
  pricePerMonth: number;

  @Column({ length: 10, default: 'KES' })
  currency: string;

  @Column({ type: 'decimal', precision: 10, scale: 2, nullable: true })
  deposit: number;

  @Column({ name: 'available_from', type: 'date', nullable: true })
  availableFrom: Date;

  @Column({ name: 'available_to', type: 'date', nullable: true })
  availableTo: Date;

  @Column({ name: 'is_available', default: true })
  isAvailable: boolean;

  @Column({ name: 'is_verified', default: false })
  isVerified: boolean;

  @Column({ name: 'is_featured', default: false })
  isFeatured: boolean;

  @Column({ name: 'bedrooms', type: 'int', default: 1 })
  bedrooms: number;

  @Column({ name: 'bathrooms', type: 'int', default: 1 })
  bathrooms: number;

  @Column({ name: 'max_occupants', type: 'int', default: 1 })
  maxOccupants: number;

  @Column({ name: 'rating_average', type: 'decimal', precision: 3, scale: 2, default: 0 })
  ratingAverage: number;

  @Column({ name: 'reviews_count', default: 0 })
  reviewsCount: number;

  @Column({ name: 'views_count', default: 0 })
  viewsCount: number;

  @Column({ name: 'inquiries_count', default: 0 })
  inquiriesCount: number;

  @OneToMany(() => PropertyImage, (img) => img.property, { cascade: true })
  images: PropertyImage[];

  @OneToMany(() => PropertyAmenity, (a) => a.property, { cascade: true })
  amenities: PropertyAmenity[];

  @OneToMany(() => PropertyReview, (r) => r.property)
  reviews: PropertyReview[];

  @OneToMany(() => AccommodationBooking, (b) => b.property)
  bookings: AccommodationBooking[];

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
