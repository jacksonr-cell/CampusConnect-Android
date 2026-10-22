import { ApiProperty } from '@nestjs/swagger';
import { IsEnum } from 'class-validator';
import { AttendeeStatus } from '../entities/event-attendee.entity';

export class RsvpDto {
  @ApiProperty({ enum: AttendeeStatus })
  @IsEnum(AttendeeStatus)
  status: AttendeeStatus;
}
