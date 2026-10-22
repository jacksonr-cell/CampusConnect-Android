import {
  Controller, Get, Post, Patch, Delete, Body, Param,
  Query, UseGuards, HttpCode, HttpStatus, ParseUUIDPipe,
} from '@nestjs/common';
import {
  ApiTags, ApiBearerAuth, ApiOperation,
  ApiQuery, ApiParam,
} from '@nestjs/swagger';
import { AccommodationService } from './accommodation.service';
import { CreatePropertyDto } from './dto/create-property.dto';
import { UpdatePropertyDto } from './dto/update-property.dto';
import { CreateInquiryDto } from './dto/create-inquiry.dto';
import { CreateReviewDto } from './dto/create-review.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { PaginationDto } from '../../common/dto/pagination.dto';
import { User, UserRole } from '../users/entities/user.entity';

@ApiTags('accommodation')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('accommodation')
export class AccommodationController {
  constructor(private readonly accommodationService: AccommodationService) {}

  // ── Public-ish listing browsing (any authenticated user) ─────────────

  @Get('properties')
  @ApiOperation({ summary: 'Browse available properties — Kabarak Nest' })
  @ApiQuery({ name: 'city', required: false })
  @ApiQuery({ name: 'type', required: false })
  @ApiQuery({ name: 'minPrice', required: false, type: Number })
  @ApiQuery({ name: 'maxPrice', required: false, type: Number })
  async listProperties(
    @Query() pagination: PaginationDto,
    @Query('city') city?: string,
    @Query('type') propertyType?: string,
    @Query('minPrice') minPrice?: number,
    @Query('maxPrice') maxPrice?: number,
  ) {
    return this.accommodationService.findAll(
      { city, propertyType, minPrice, maxPrice },
      pagination,
    );
  }

  @Get('properties/:id')
  @ApiOperation({ summary: 'Get full property details' })
  @ApiParam({ name: 'id', type: 'string', format: 'uuid' })
  async getProperty(@Param('id', ParseUUIDPipe) id: string) {
    return this.accommodationService.findById(id);
  }

  @Get('properties/:id/reviews')
  @ApiOperation({ summary: 'Get reviews for a property' })
  async getReviews(
    @Param('id', ParseUUIDPipe) id: string,
    @Query() pagination: PaginationDto,
  ) {
    return this.accommodationService.getReviews(id, pagination);
  }

  // ── Landlord-only: listing management ────────────────────────────────

  @Post('properties')
  @HttpCode(HttpStatus.CREATED)
  @Roles(UserRole.LANDLORD, UserRole.ADMIN)
  @ApiOperation({ summary: '🏠 Create a new listing (Landlord / Admin only)' })
  async createProperty(
    @CurrentUser() user: User,
    @Body() dto: CreatePropertyDto,
  ) {
    return this.accommodationService.createProperty(user.id, dto);
  }

  @Patch('properties/:id')
  @Roles(UserRole.LANDLORD, UserRole.ADMIN)
  @ApiOperation({ summary: '🏠 Update your listing (Landlord / Admin only)' })
  async updateProperty(
    @CurrentUser() user: User,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: UpdatePropertyDto,
  ) {
    return this.accommodationService.updateProperty(id, user.id, dto);
  }

  @Delete('properties/:id')
  @HttpCode(HttpStatus.OK)
  @Roles(UserRole.LANDLORD, UserRole.ADMIN)
  @ApiOperation({ summary: '🏠 Delete your listing (Landlord / Admin only)' })
  async deleteProperty(
    @CurrentUser() user: User,
    @Param('id', ParseUUIDPipe) id: string,
  ) {
    await this.accommodationService.deleteProperty(id, user.id);
    return { message: 'Property deleted' };
  }

  // ── Student actions ───────────────────────────────────────────────────

  @Post('properties/:id/inquire')
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Send an inquiry / booking request' })
  async inquire(
    @CurrentUser() user: User,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: CreateInquiryDto,
  ) {
    return this.accommodationService.createInquiry(id, user.id, dto);
  }

  @Post('properties/:id/save')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Save property to favourites' })
  async saveProperty(
    @CurrentUser() user: User,
    @Param('id', ParseUUIDPipe) id: string,
  ) {
    await this.accommodationService.saveProperty(user.id, id);
    return { message: 'Property saved' };
  }

  @Delete('properties/:id/save')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Remove property from favourites' })
  async unsaveProperty(
    @CurrentUser() user: User,
    @Param('id', ParseUUIDPipe) id: string,
  ) {
    await this.accommodationService.unsaveProperty(user.id, id);
    return { message: 'Property removed from saved' };
  }

  @Get('saved')
  @ApiOperation({ summary: 'Get your saved / favourite properties' })
  async getSaved(@CurrentUser() user: User, @Query() pagination: PaginationDto) {
    return this.accommodationService.getSavedProperties(user.id, pagination);
  }

  @Post('properties/:id/reviews')
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Submit a review (must have lived there)' })
  async addReview(
    @CurrentUser() user: User,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: CreateReviewDto,
  ) {
    return this.accommodationService.addReview(id, user.id, dto);
  }
}
