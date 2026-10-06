import {
  Entity, PrimaryGeneratedColumn, Column, ManyToOne, JoinColumn,
} from 'typeorm';
import { Property } from './property.entity';

export enum AmenityCategory {
  UTILITIES = 'utilities',
  SECURITY = 'security',
  CONNECTIVITY = 'connectivity',
  APPLIANCES = 'appliances',
  TRANSPORT = 'transport',
  OTHER = 'other',
}

@Entity('property_amenities')
export class PropertyAmenity {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'property_id' })
  propertyId: string;

  @ManyToOne(() => Property, (p) => p.amenities, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'property_id' })
  property: Property;

  @Column({ name: 'amenity_name', length: 100 })
  amenityName: string;

  @Column({ type: 'enum', enum: AmenityCategory, default: AmenityCategory.OTHER })
  category: AmenityCategory;
}
