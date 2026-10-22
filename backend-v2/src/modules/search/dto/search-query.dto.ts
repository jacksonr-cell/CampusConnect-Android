import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsString, IsOptional, IsEnum, MinLength, IsInt, Min, Max } from 'class-validator';
import { Type } from 'class-transformer';

export enum SearchEntity {
  ALL            = 'all',
  USERS          = 'users',
  POSTS          = 'posts',
  EVENTS         = 'events',
  CLUBS          = 'clubs',
  ACCOMMODATION  = 'accommodation',
  MARKETPLACE    = 'marketplace',
}

export class SearchQueryDto {
  @ApiProperty({ description: 'Search query string', minLength: 2 })
  @IsString()
  @MinLength(2)
  q: string;

  @ApiPropertyOptional({ enum: SearchEntity, default: SearchEntity.ALL })
  @IsOptional()
  @IsEnum(SearchEntity)
  entity?: SearchEntity = SearchEntity.ALL;

  @ApiPropertyOptional({ default: 1, minimum: 1 })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  page?: number = 1;

  @ApiPropertyOptional({ default: 20, minimum: 1, maximum: 50 })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(50)
  limit?: number = 20;
}
