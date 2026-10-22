import {
  Entity, PrimaryGeneratedColumn, Column, CreateDateColumn,
  UpdateDateColumn, ManyToOne, JoinColumn, Index,
} from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { Property } from './property.entity';

export enum BookingStatus {
  INQUIRY = 'inquiry',
  PENDING = 'pending',
  ACCEPTED = 'accepted',
  REJECTED = 'rejected',
  CANCELLED = 'cancelled',
  COMPLETED = 'completed',
}

@Entity('accommodation_bookings')
@Index(['propertyId'])
@Index(['studentId'])
export class AccommodationBooking {
  @PrimaryGeneratedColumn('uuid')
  id: string;

  @Column({ name: 'property_id' })
  propertyId: string;

  @ManyToOne(() => Property, (p) => p.bookings, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'property_id' })
  property: Property;

  @Column({ name: 'student_id' })
  studentId: string;

  @ManyToOne(() => User, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'student_id' })
  student: User;

  @Column({ type: 'enum', enum: BookingStatus, default: BookingStatus.INQUIRY })
  status: BookingStatus;

  @Column({ name: 'move_in_date', type: 'date', nullable: true })
  moveInDate: Date;

  @Column({ name: 'move_out_date', type: 'date', nullable: true })
  moveOutDate: Date;

  @Column({ type: 'text', nullable: true })
  message: string;

  @Column({ name: 'landlord_response', type: 'text', nullable: true })
  landlordResponse: string;

  @Column({ name: 'responded_at', nullable: true, type: 'timestamp' })
  respondedAt: Date;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
