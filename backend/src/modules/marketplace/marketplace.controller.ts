import {
  Controller, Get, Post, Patch, Delete, Body, Param,
  Query, UseGuards, HttpCode, HttpStatus, ParseUUIDPipe,
} from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { MarketplaceService } from './marketplace.service';
import { CreateListingDto } from './dto/create-listing.dto';
import { UpdateListingDto } from './dto/update-listing.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { PaginationDto } from '../../common/dto/pagination.dto';
import { User } from '../users/entities/user.entity';

@ApiTags('marketplace')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('marketplace')
export class MarketplaceController {
  constructor(private readonly marketplaceService: MarketplaceService) {}

  @Get()
  @ApiOperation({ summary: 'Browse marketplace listings' })
  async listListings(
    @Query() pagination: PaginationDto,
    @Query('category') category?: string,
    @Query('maxPrice') maxPrice?: number,
    @Query('search') search?: string,
  ) {
    return this.marketplaceService.findAll({ category, maxPrice, search }, pagination);
  }

  @Get('my-listings')
  @ApiOperation({ summary: 'Get your own listings' })
  async getMyListings(@CurrentUser() user: User, @Query() pagination: PaginationDto) {
    return this.marketplaceService.getMyListings(user.id, pagination);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get listing details' })
  async getListing(@Param('id', ParseUUIDPipe) id: string) {
    return this.marketplaceService.findById(id);
  }

  @Post()
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Create a new listing' })
  async createListing(@CurrentUser() user: User, @Body() dto: CreateListingDto) {
    return this.marketplaceService.createListing(user.id, dto);
  }

  @Patch(':id')
  @ApiOperation({ summary: 'Update your listing' })
  async updateListing(
    @CurrentUser() user: User,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: UpdateListingDto,
  ) {
    return this.marketplaceService.updateListing(id, user.id, dto);
  }

  @Patch(':id/sold')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Mark listing as sold' })
  async markAsSold(@CurrentUser() user: User, @Param('id', ParseUUIDPipe) id: string) {
    await this.marketplaceService.markAsSold(id, user.id);
    return { message: 'Listing marked as sold' };
  }

  @Delete(':id')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Delete a listing' })
  async deleteListing(@CurrentUser() user: User, @Param('id', ParseUUIDPipe) id: string) {
    await this.marketplaceService.deleteListing(id, user.id);
    return { message: 'Listing removed' };
  }
}
