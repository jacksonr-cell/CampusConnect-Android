import {
  Controller, Get, Post, Patch, Delete, Body, Param,
  Query, UseGuards, HttpCode, HttpStatus, ParseUUIDPipe,
} from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { EventsService } from './events.service';
import { CreateEventDto } from './dto/create-event.dto';
import { UpdateEventDto } from './dto/update-event.dto';
import { RsvpDto } from './dto/rsvp.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { PaginationDto } from '../../common/dto/pagination.dto';
import { User } from '../users/entities/user.entity';

@ApiTags('events')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('events')
export class EventsController {
  constructor(private readonly eventsService: EventsService) {}

  @Get()
  @ApiOperation({ summary: 'List upcoming campus events' })
  async listEvents(@CurrentUser() user: User, @Query() pagination: PaginationDto) {
    return this.eventsService.findAll(user.id, pagination);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get event details' })
  async getEvent(@CurrentUser() user: User, @Param('id', ParseUUIDPipe) id: string) {
    return this.eventsService.findById(id, user.id);
  }

  @Get(':id/attendees')
  @ApiOperation({ summary: 'Get event attendees' })
  async getAttendees(
    @Param('id', ParseUUIDPipe) id: string,
    @Query() pagination: PaginationDto,
  ) {
    return this.eventsService.getAttendees(id, pagination);
  }

  @Post()
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Create a new event' })
  async createEvent(@CurrentUser() user: User, @Body() dto: CreateEventDto) {
    return this.eventsService.createEvent(user.id, dto);
  }

  @Patch(':id')
  @ApiOperation({ summary: 'Update an event (organiser only)' })
  async updateEvent(
    @CurrentUser() user: User,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: UpdateEventDto,
  ) {
    return this.eventsService.updateEvent(id, user.id, dto);
  }

  @Delete(':id/cancel')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Cancel an event (organiser only)' })
  async cancelEvent(@CurrentUser() user: User, @Param('id', ParseUUIDPipe) id: string) {
    await this.eventsService.cancelEvent(id, user.id);
    return { message: 'Event cancelled' };
  }

  @Post(':id/rsvp')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'RSVP to an event' })
  async rsvp(
    @CurrentUser() user: User,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: RsvpDto,
  ) {
    return this.eventsService.rsvp(id, user.id, dto.status);
  }
}
