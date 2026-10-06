import {
  Controller, Get, Patch, Delete, Body, Param,
  Query, UseGuards, HttpCode, HttpStatus, ParseUUIDPipe,
} from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation, ApiQuery } from '@nestjs/swagger';
import { AdminUsersService } from './users/admin-users.service';
import { AdminStatsService } from './stats/admin-stats.service';
import { ReportsService } from '../reports/reports.service';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { PaginationDto } from '../../common/dto/pagination.dto';
import { User, UserRole } from '../users/entities/user.entity';
import { ApiProperty } from '@nestjs/swagger';

class SetActiveDto {
  @ApiProperty() isActive: boolean;
}
class ChangeRoleDto {
  @ApiProperty({ enum: UserRole }) role: UserRole;
}
class SetVerifiedDto {
  @ApiProperty() isVerified: boolean;
}

@ApiTags('admin')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles(UserRole.ADMIN)
@Controller('admin')
export class AdminController {
  constructor(
    private readonly adminUsersService  : AdminUsersService,
    private readonly adminStatsService  : AdminStatsService,
    private readonly reportsService     : ReportsService,
  ) {}

  // ── Dashboard overview ────────────────────────────────────────────────

  @Get('dashboard')
  @ApiOperation({ summary: '🛡️ Platform analytics dashboard — users, content, Nest, events' })
  async getDashboard() {
    return this.adminStatsService.getPlatformOverview();
  }

  @Get('dashboard/engagement')
  @ApiOperation({ summary: '🛡️ Engagement metrics — reactions, comments, property inquiries' })
  async getEngagement() {
    return this.adminStatsService.getEngagementMetrics();
  }

  @Get('dashboard/growth')
  @ApiOperation({ summary: '🛡️ User growth chart (last N days)' })
  @ApiQuery({ name: 'days', required: false, type: Number })
  async getGrowthChart(@Query('days') days?: number) {
    return this.adminStatsService.getUserGrowthChart(days || 30);
  }

  // ── User management ───────────────────────────────────────────────────

  @Get('users')
  @ApiOperation({ summary: '🛡️ List all users with filters' })
  @ApiQuery({ name: 'search',   required: false })
  @ApiQuery({ name: 'role',     required: false, enum: UserRole })
  @ApiQuery({ name: 'isActive', required: false, type: Boolean })
  async listUsers(
    @Query() pagination  : PaginationDto,
    @Query('search')   search?  : string,
    @Query('role')     role?    : UserRole,
    @Query('isActive') isActive?: boolean,
  ) {
    return this.adminUsersService.listUsers({ search, role, isActive }, pagination);
  }

  @Get('users/:id')
  @ApiOperation({ summary: '🛡️ Get full user detail' })
  async getUserDetail(@Param('id', ParseUUIDPipe) id: string) {
    return this.adminUsersService.getUserDetail(id);
  }

  @Patch('users/:id/active')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '🛡️ Activate or deactivate a user account' })
  async setActiveStatus(
    @CurrentUser() admin: User,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: SetActiveDto,
  ) {
    await this.adminUsersService.setActiveStatus(id, dto.isActive, admin.id);
    return { message: `User ${dto.isActive ? 'activated' : 'deactivated'}` };
  }

  @Patch('users/:id/role')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '🛡️ Change a user\'s role' })
  async changeRole(
    @CurrentUser() admin: User,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: ChangeRoleDto,
  ) {
    await this.adminUsersService.changeRole(id, dto.role, admin.id);
    return { message: `User role updated to ${dto.role}` };
  }

  @Patch('users/:id/verify')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '🛡️ Grant or revoke verification badge' })
  async setVerified(
    @CurrentUser() admin: User,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: SetVerifiedDto,
  ) {
    await this.adminUsersService.setVerified(id, dto.isVerified, admin.id);
    return { message: `User ${dto.isVerified ? 'verified' : 'unverified'}` };
  }

  @Delete('users/:id')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '🛡️ Soft-delete a user account (anonymises data)' })
  async deleteUser(
    @CurrentUser() admin: User,
    @Param('id', ParseUUIDPipe) id: string,
  ) {
    await this.adminUsersService.deleteUser(id, admin.id);
    return { message: 'User account deleted' };
  }

  // ── Moderation — Reports ──────────────────────────────────────────────

  @Get('reports')
  @ApiOperation({ summary: '🛡️ List pending content/user reports' })
  async getPendingReports(@Query() pagination: PaginationDto) {
    return this.reportsService.getPendingReports(pagination);
  }
}
