import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import {
  IsString, IsArray, IsUUID, MinLength, MaxLength, ArrayMinSize,
} from 'class-validator';

export class CreateGroupDto {
  @ApiProperty({ example: 'Study Group — Algorithms' })
  @IsString()
  @MinLength(3)
  @MaxLength(150)
  name: string;

  @ApiProperty({ type: [String], description: 'Array of participant user UUIDs' })
  @IsArray()
  @IsUUID('4', { each: true })
  @ArrayMinSize(2)
  participantIds: string[];

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  avatarUrl?: string;
}
