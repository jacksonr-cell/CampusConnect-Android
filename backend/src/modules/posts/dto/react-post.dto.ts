import { ApiProperty } from '@nestjs/swagger';
import { IsEnum } from 'class-validator';
import { ReactionType } from '../entities/reaction.entity';

export class ReactPostDto {
  @ApiProperty({ enum: ReactionType })
  @IsEnum(ReactionType)
  reactionType: ReactionType;
}
