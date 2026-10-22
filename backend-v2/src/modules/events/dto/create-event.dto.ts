import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import {
  IsString, IsEnum, IsOptional, IsBoolean, IsNumber,
  IsDateString, IsInt, Min, MaxLength, MinLength,
} from 'class-validator';
import { EventCategory } from '../entities/event.entity';

export class CreateEventDto {
  @ApiProperty({ example: 'Annual Tech Symposium 2025' })
  @IsString()
  @MinLength(5)
  @MaxLength(255)
  title: string;

  @ApiProperty({ example: 'Join us for a day of tech talks, workshops and networking...' })
  @IsString()
  @MinLength(20)
  description: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  coverImage?: string;

  @ApiPropertyOptional({ example: 'Kabarak University Main Auditorium' })
  @IsOptional()
  @IsString()
  @MaxLength(500)
  location?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  venueLatitude?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsNumber()
  venueLongitude?: number;

  @ApiProperty({ example: '2025-06-15T09:00:00.000Z' })
  @IsDateString()
  startTime: string;

  @ApiPropertyOptional({ example: '2025-06-15T17:00:00.000Z' })
  @IsOptional()
  @IsDateString()
  endTime?: string;

  @ApiProperty({ enum: EventCategory, example: EventCategory.TECH })
  @IsEnum(EventCategory)
  category: EventCategory;

  @ApiPropertyOptional({ default: false })
  @IsOptional()
  @IsBoolean()
  isTicketed?: boolean;

  @ApiPropertyOptional({ example: 500 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  ticketPrice?: number;

  @ApiPropertyOptional({ example: 200 })
  @IsOptional()
  @IsInt()
  @Min(1)
  maxAttendees?: number;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  clubId?: string;

  @ApiPropertyOptional({ example: 'Kabarak University' })
  @IsOptional()
  @IsString()
  @MaxLength(200)
  university?: string;
}
