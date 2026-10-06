import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import {
  IsString, IsEnum, IsNumber, IsOptional, IsBoolean,
  IsArray, Min, MaxLength, MinLength, ArrayMaxSize,
} from 'class-validator';
import { ListingCategory, ListingCondition } from '../entities/listing.entity';

export class CreateListingDto {
  @ApiProperty({ example: 'Engineering Mathematics Textbook — 4th Edition' })
  @IsString()
  @MinLength(3)
  @MaxLength(255)
  title: string;

  @ApiProperty({ example: 'Barely used, all chapters intact. No missing pages.' })
  @IsString()
  @MinLength(10)
  @MaxLength(3000)
  description: string;

  @ApiProperty({ enum: ListingCategory, example: ListingCategory.TEXTBOOKS })
  @IsEnum(ListingCategory)
  category: ListingCategory;

  @ApiPropertyOptional({ enum: ListingCondition, example: ListingCondition.LIKE_NEW })
  @IsOptional()
  @IsEnum(ListingCondition)
  condition?: ListingCondition;

  @ApiProperty({ example: 850 })
  @IsNumber()
  @Min(0)
  price: number;

  @ApiPropertyOptional({ example: 'KES' })
  @IsOptional()
  @IsString()
  @MaxLength(10)
  currency?: string;

  @ApiPropertyOptional({ default: false })
  @IsOptional()
  @IsBoolean()
  isNegotiable?: boolean;

  @ApiPropertyOptional({ type: [String], maxItems: 10 })
  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  @ArrayMaxSize(10)
  imageUrls?: string[];

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @MaxLength(200)
  university?: string;
}
