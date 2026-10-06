import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AccommodationService } from './accommodation.service';
import { AccommodationController } from './accommodation.controller';
import { Property } from './entities/property.entity';
import { PropertyImage } from './entities/property-image.entity';
import { PropertyAmenity } from './entities/property-amenity.entity';
import { PropertyReview } from './entities/property-review.entity';
import { AccommodationBooking } from './entities/booking.entity';
import { SavedListing } from './entities/saved-listing.entity';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      Property, PropertyImage, PropertyAmenity,
      PropertyReview, AccommodationBooking, SavedListing,
    ]),
  ],
  controllers: [AccommodationController],
  providers: [AccommodationService],
  exports: [AccommodationService, TypeOrmModule],
})
export class AccommodationModule {}
