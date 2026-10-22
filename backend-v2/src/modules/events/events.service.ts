import {
  Injectable, NotFoundException, ForbiddenException, ConflictException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, Repository } from 'typeorm';
import { Event } from './entities/event.entity';
import { EventAttendee, AttendeeStatus } from './entities/event-attendee.entity';
import { CreateEventDto } from './dto/create-event.dto';
import { UpdateEventDto } from './dto/update-event.dto';
import { PaginationDto, paginate, PaginatedResult } from '../../common/dto/pagination.dto';

/** Event entity enriched with the requesting user's own RSVP status. */
export interface EnrichedEvent extends Event {
  myStatus: AttendeeStatus | null;
}

@Injectable()
export class EventsService {
  constructor(
    @InjectRepository(Event) private readonly eventRepository: Repository<Event>,
    @InjectRepository(EventAttendee) private readonly attendeeRepository: Repository<EventAttendee>,
  ) {}

  async findAll(userId: string, pagination: PaginationDto): Promise<PaginatedResult<EnrichedEvent>> {
    const { page = 1, limit = 20 } = pagination;
    const [events, total] = await this.eventRepository.findAndCount({
      where: { isCancelled: false },
      relations: ['organizer', 'club'],
      order: { startTime: 'ASC' },
      skip: (page - 1) * limit,
      take: limit,
    });

    const enriched = await this.enrichWithMyStatus(events, userId);
    return paginate(enriched, total, page, limit);
  }

  async findById(id: string, userId: string): Promise<EnrichedEvent> {
    const event = await this.eventRepository.findOne({
      where: { id },
      relations: ['organizer', 'club'],
    });
    if (!event) throw new NotFoundException('Event not found');

    const [enriched] = await this.enrichWithMyStatus([event], userId);
    return enriched;
  }

  async createEvent(organizerId: string, dto: CreateEventDto): Promise<Event> {
    return this.eventRepository.save(
      this.eventRepository.create({ ...dto, organizerId }),
    );
  }

  async updateEvent(id: string, organizerId: string, dto: UpdateEventDto): Promise<Event> {
    const event = await this.eventRepository.findOne({ where: { id } });
    if (!event) throw new NotFoundException('Event not found');
    if (event.organizerId !== organizerId) throw new ForbiddenException('Not your event');
    Object.assign(event, dto);
    return this.eventRepository.save(event);
  }

  async cancelEvent(id: string, organizerId: string): Promise<void> {
    const event = await this.eventRepository.findOne({ where: { id } });
    if (!event) throw new NotFoundException('Event not found');
    if (event.organizerId !== organizerId) throw new ForbiddenException('Not your event');
    event.isCancelled = true;
    await this.eventRepository.save(event);
  }

  async rsvp(eventId: string, userId: string, status: AttendeeStatus): Promise<EventAttendee> {
    const event = await this.eventRepository.findOne({ where: { id: eventId } });
    if (!event) throw new NotFoundException('Event not found');
    if (event.isCancelled) throw new ForbiddenException('This event has been cancelled');
    if (event.maxAttendees && event.attendeesCount >= event.maxAttendees) {
      throw new ConflictException('This event is fully booked');
    }

    const existing = await this.attendeeRepository.findOne({ where: { eventId, userId } });
    if (existing) {
      const wasGoing = existing.status === AttendeeStatus.GOING;
      const nowGoing = status === AttendeeStatus.GOING;
      existing.status = status;
      if (!wasGoing && nowGoing) await this.eventRepository.increment({ id: eventId }, 'attendeesCount', 1);
      if (wasGoing && !nowGoing) await this.eventRepository.decrement({ id: eventId }, 'attendeesCount', 1);
      return this.attendeeRepository.save(existing);
    }

    const attendee = this.attendeeRepository.create({ eventId, userId, status });
    if (status === AttendeeStatus.GOING) {
      await this.eventRepository.increment({ id: eventId }, 'attendeesCount', 1);
    }
    return this.attendeeRepository.save(attendee);
  }

  async getAttendees(eventId: string, pagination: PaginationDto): Promise<PaginatedResult<EventAttendee>> {
    const { page = 1, limit = 20 } = pagination;
    const [attendees, total] = await this.attendeeRepository.findAndCount({
      where: { eventId, status: AttendeeStatus.GOING },
      relations: ['user'],
      skip: (page - 1) * limit,
      take: limit,
    });
    return paginate(attendees, total, page, limit);
  }

  /** One extra query total (not N+1) — fetches the requester's attendee rows across all event ids at once. */
  private async enrichWithMyStatus(events: Event[], userId: string): Promise<EnrichedEvent[]> {
    if (events.length === 0) return [];
    const eventIds = events.map((e) => e.id);

    const myAttendances = await this.attendeeRepository.find({
      where: { eventId: In(eventIds), userId },
    });
    const statusMap = new Map(myAttendances.map((a) => [a.eventId, a.status]));

    return events.map((event) => ({
      ...event,
      myStatus: statusMap.get(event.id) ?? null,
    })) as EnrichedEvent[];
  }
}
