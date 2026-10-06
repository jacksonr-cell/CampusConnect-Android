import {
  Controller, Get, Post, Patch, Delete, Body, Param,
  Query, UseGuards, HttpCode, HttpStatus, ParseUUIDPipe,
} from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { ClubsService } from './clubs.service';
import { CreateClubDto } from './dto/create-club.dto';
import { UpdateClubDto } from './dto/update-club.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { PaginationDto } from '../../common/dto/pagination.dto';
import { User } from '../users/entities/user.entity';

@ApiTags('clubs')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('clubs')
export class ClubsController {
  constructor(private readonly clubsService: ClubsService) {}

  @Get()
  @ApiOperation({ summary: 'List all campus clubs' })
  async listClubs(@Query() pagination: PaginationDto) {
    return this.clubsService.findAll(pagination);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get club details' })
  async getClub(@Param('id', ParseUUIDPipe) id: string) {
    return this.clubsService.findById(id);
  }

  @Post()
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Create a new club' })
  async createClub(@CurrentUser() user: User, @Body() dto: CreateClubDto) {
    return this.clubsService.createClub(user.id, dto);
  }

  @Patch(':id')
  @ApiOperation({ summary: 'Update a club (leader only)' })
  async updateClub(
    @CurrentUser() user: User,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: UpdateClubDto,
  ) {
    return this.clubsService.updateClub(id, user.id, dto);
  }

  @Post(':id/join')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Join a club' })
  async joinClub(@CurrentUser() user: User, @Param('id', ParseUUIDPipe) id: string) {
    return this.clubsService.joinClub(id, user.id);
  }

  @Delete(':id/leave')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Leave a club' })
  async leaveClub(@CurrentUser() user: User, @Param('id', ParseUUIDPipe) id: string) {
    await this.clubsService.leaveClub(id, user.id);
    return { message: 'Left club successfully' };
  }
}
