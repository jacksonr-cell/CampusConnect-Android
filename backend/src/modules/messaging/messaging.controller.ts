import {
  Controller, Get, Post, Body, Param,
  Query, UseGuards, HttpCode, HttpStatus, ParseUUIDPipe,
} from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { MessagingService } from './messaging.service';
import { SendMessageDto } from './dto/send-message.dto';
import { CreateGroupDto } from './dto/create-group.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { PaginationDto } from '../../common/dto/pagination.dto';
import { User } from '../users/entities/user.entity';

@ApiTags('messaging')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('messaging')
export class MessagingController {
  constructor(private readonly messagingService: MessagingService) {}

  @Get('conversations')
  @ApiOperation({ summary: 'Get all conversations' })
  async getConversations(@CurrentUser() user: User, @Query() pagination: PaginationDto) {
    return this.messagingService.getConversations(user.id, pagination);
  }

  @Post('conversations/direct/:userId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Get or create a direct conversation' })
  async getOrCreateDirect(
    @CurrentUser() user: User,
    @Param('userId', ParseUUIDPipe) userId: string,
  ) {
    return this.messagingService.getOrCreateDirect(user.id, userId);
  }

  @Post('conversations/group')
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Create a group conversation' })
  async createGroup(@CurrentUser() user: User, @Body() dto: CreateGroupDto) {
    return this.messagingService.createGroup(user.id, dto);
  }

  @Get('conversations/:id/messages')
  @ApiOperation({ summary: 'Get messages in a conversation' })
  async getMessages(
    @CurrentUser() user: User,
    @Param('id', ParseUUIDPipe) id: string,
    @Query() pagination: PaginationDto,
  ) {
    return this.messagingService.getMessages(id, user.id, pagination);
  }

  @Post('conversations/:id/messages')
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Send a message' })
  async sendMessage(
    @CurrentUser() user: User,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: SendMessageDto,
  ) {
    return this.messagingService.sendMessage(id, user.id, dto);
  }

  @Post('conversations/:id/read')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Mark conversation as read' })
  async markAsRead(@CurrentUser() user: User, @Param('id', ParseUUIDPipe) id: string) {
    await this.messagingService.markAsRead(id, user.id);
    return { message: 'Marked as read' };
  }
}
