import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { SearchService } from './search.service';
import { SearchController } from './search.controller';
import { User } from '../users/entities/user.entity';
import { Post } from '../posts/entities/post.entity';
import { Event } from '../events/entities/event.entity';
import { Club } from '../clubs/entities/club.entity';
import { Property } from '../accommodation/entities/property.entity';
import { MarketplaceListing } from '../marketplace/entities/listing.entity';

@Module({
  imports: [
    TypeOrmModule.forFeature([User, Post, Event, Club, Property, MarketplaceListing]),
  ],
  providers  : [SearchService],
  controllers: [SearchController],
  exports    : [SearchService],
})
export class SearchModule {}
