import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AdminController } from './admin.controller';
import { AdminUsersService } from './users/admin-users.service';
import { AdminStatsService } from './stats/admin-stats.service';
import { User } from '../users/entities/user.entity';
import { Post } from '../posts/entities/post.entity';
import { Property } from '../accommodation/entities/property.entity';
import { AccommodationBooking } from '../accommodation/entities/booking.entity';
import { Event } from '../events/entities/event.entity';
import { Club } from '../clubs/entities/club.entity';
import { MarketplaceListing } from '../marketplace/entities/listing.entity';
import { Report } from '../reports/entities/report.entity';
import { ReportsModule } from '../reports/reports.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      User, Post, Property, AccommodationBooking,
      Event, Club, MarketplaceListing, Report,
    ]),
    ReportsModule,
  ],
  providers  : [AdminUsersService, AdminStatsService],
  controllers: [AdminController],
  exports    : [AdminStatsService],
})
export class AdminModule {}
