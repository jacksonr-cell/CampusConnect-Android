import {
  Controller, Post, Get, Patch, Body, Param,
  Query, UseGuards, HttpCode, HttpStatus, ParseUUIDPipe,
} from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { ReportsService } from './reports.service';
import { CreateReportDto } from './dto/create-report.dto';
import { ReviewReportDto } from './dto/review-report.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { PaginationDto } from '../../common/dto/pagination.dto';
import { User, UserRole } from '../users/entities/user.entity';

@ApiTags('reports')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('reports')
export class ReportsController {
  constructor(private readonly reportsService: ReportsService) {}

  @Post()
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Submit a content or user report' })
  async createReport(@CurrentUser() user: User, @Body() dto: CreateReportDto) {
    return this.reportsService.createReport(user.id, dto);
  }

  @Get('pending')
  @Roles(UserRole.ADMIN)
  @ApiOperation({ summary: '🛡️ Get all pending reports (Admin only)' })
  async getPendingReports(@Query() pagination: PaginationDto) {
    return this.reportsService.getPendingReports(pagination);
  }

  @Patch(':id/review')
  @Roles(UserRole.ADMIN)
  @ApiOperation({ summary: '🛡️ Action or dismiss a report (Admin only)' })
  async reviewReport(
    @CurrentUser() user: User,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: ReviewReportDto,
  ) {
    return this.reportsService.reviewReport(id, user.id, dto.status, dto.moderatorNote);
  }
}
