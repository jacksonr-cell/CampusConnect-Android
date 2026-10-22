import {
  Controller, Get, Query, UseGuards,
} from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation, ApiQuery } from '@nestjs/swagger';
import { GamificationService } from './gamification.service';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { User } from '../users/entities/user.entity';

@ApiTags('gamification')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('gamification')
export class GamificationController {
  constructor(private readonly gamificationService: GamificationService) {}

  @Get('me/points')
  @ApiOperation({ summary: 'Get own reputation points and level' })
  async getMyPoints(@CurrentUser() user: User) {
    return this.gamificationService.getUserPoints(user.id);
  }

  @Get('me/badges')
  @ApiOperation({ summary: 'Get the full badge catalog with your earned/locked status for each' })
  async getMyBadges(@CurrentUser() user: User) {
    return this.gamificationService.getMyBadgesFull(user.id);
  }

  @Get('leaderboard')
  @ApiOperation({ summary: 'All-time campus leaderboard' })
  @ApiQuery({ name: 'limit', required: false, type: Number })
  async getLeaderboard(@Query('limit') limit?: number) {
    return this.gamificationService.getLeaderboard(limit || 20);
  }

  @Get('leaderboard/monthly')
  @ApiOperation({ summary: 'Monthly campus leaderboard' })
  async getMonthlyLeaderboard(@Query('limit') limit?: number) {
    return this.gamificationService.getMonthlyLeaderboard(limit || 20);
  }
}
