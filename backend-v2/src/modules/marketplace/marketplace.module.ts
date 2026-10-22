import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { MarketplaceService } from './marketplace.service';
import { MarketplaceController } from './marketplace.controller';
import { MarketplaceListing } from './entities/listing.entity';

@Module({
  imports: [TypeOrmModule.forFeature([MarketplaceListing])],
  controllers: [MarketplaceController],
  providers: [MarketplaceService],
  exports: [MarketplaceService, TypeOrmModule],
})
export class MarketplaceModule {}
