import {
  Controller, Get, Patch, Delete, Post, Body, Param,
  Query, UseGuards, HttpCode, HttpStatus, ParseUUIDPipe,
} from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation, ApiQuery } from '@nestjs/swagger';
import { UsersService } from './users.service';
import { UpdateProfileDto } from './dto/update-profile.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { PaginationDto } from '../../common/dto/pagination.dto';
import { User } from './entities/user.entity';

@ApiTags('users')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('users')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Get('profile')
  @ApiOperation({ summary: 'Get own profile' })
  async getMyProfile(@CurrentUser() user: User) {
    return this.usersService.sanitizeUser(user);
  }

  @Patch('profile')
  @ApiOperation({ summary: 'Update own profile' })
  async updateProfile(@CurrentUser() user: User, @Body() dto: UpdateProfileDto) {
    const updated = await this.usersService.updateProfile(user.id, dto);
    return this.usersService.sanitizeUser(updated);
  }

  @Get('search')
  @ApiOperation({ summary: 'Search for students' })
  @ApiQuery({ name: 'q', required: true })
  async searchUsers(@Query('q') query: string, @Query() pagination: PaginationDto) {
    return this.usersService.searchUsers(query, pagination);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get user profile by ID' })
  async getUserById(@Param('id', ParseUUIDPipe) id: string) {
    const user = await this.usersService.findById(id);
    return this.usersService.sanitizeUser(user);
  }

  @Get('by-username/:username')
  @ApiOperation({ summary: 'Get user profile by username' })
  async getUserByUsername(@Param('username') username: string) {
    const user = await this.usersService.findByUsername(username);
    return this.usersService.sanitizeUser(user);
  }

  @Post(':id/follow')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Follow a user' })
  async followUser(@CurrentUser() user: User, @Param('id', ParseUUIDPipe) id: string) {
    await this.usersService.followUser(user.id, id);
    return { message: 'User followed successfully' };
  }

  @Delete(':id/follow')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Unfollow a user' })
  async unfollowUser(@CurrentUser() user: User, @Param('id', ParseUUIDPipe) id: string) {
    await this.usersService.unfollowUser(user.id, id);
    return { message: 'User unfollowed successfully' };
  }

  @Get(':id/followers')
  @ApiOperation({ summary: "Get a user's followers" })
  async getFollowers(@Param('id', ParseUUIDPipe) id: string, @Query() pagination: PaginationDto) {
    return this.usersService.getFollowers(id, pagination);
  }

  @Get(':id/following')
  @ApiOperation({ summary: 'Get accounts a user follows' })
  async getFollowing(@Param('id', ParseUUIDPipe) id: string, @Query() pagination: PaginationDto) {
    return this.usersService.getFollowing(id, pagination);
  }
}
