import { ApiPropertyOptional } from '@nestjs/swagger';
import {
  IsString, IsOptional, IsEnum, IsArray,
  MinLength, MaxLength, ArrayMaxSize,
} from 'class-validator';
import { PostType, PostVisibility } from '../entities/post.entity';

export class CreatePostDto {
  @ApiPropertyOptional({ description: 'Post text content' })
  @IsOptional()
  @IsString()
  @MinLength(1)
  @MaxLength(5000)
  content?: string;

  @ApiPropertyOptional({ enum: PostType, default: PostType.TEXT })
  @IsOptional()
  @IsEnum(PostType)
  postType?: PostType;

  @ApiPropertyOptional({ type: [String] })
  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  @ArrayMaxSize(10)
  mediaUrls?: string[];

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  communityId?: string;

  @ApiPropertyOptional({ enum: PostVisibility, default: PostVisibility.PUBLIC })
  @IsOptional()
  @IsEnum(PostVisibility)
  visibility?: PostVisibility;
}
