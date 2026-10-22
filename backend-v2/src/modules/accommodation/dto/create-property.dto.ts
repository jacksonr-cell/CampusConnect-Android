import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import {
  IsString, IsEnum, IsNumber, IsOptional,
  IsDateString, IsInt, Min, Max, MinLength, MaxLength,
} from 'class-validator';
import { PropertyType } from '../entities/property.entity';

export class CreatePropertyDto {
  @ApiProperty({ example: 'Cosy studio near Kabarak Gate' })
  @IsString()
  @MinLength(10)
  @MaxLength(255)
  title: string;

  @ApiProperty({ example: 'Spacious self-contained studio with 24-hour security...' })
  @IsString()
  @MinLength(20)
  description: string;

  @ApiProperty({ enum: PropertyType, example: PropertyType.STUDIO })
  @IsEnum(PropertyType)
  propertyType: PropertyType;

  @ApiProperty({ example: '123 University Road, Nakuru' })
  @IsString()
  @MaxLength(500)
  address: string;

  @ApiProperty({ example: 'Nakuru' })
  @IsString()
  @MaxLength(100)
  city: string;

  @ApiPropertyOptional({ example: 'Lanet' })
  @IsOptional()
  @IsString()
  @MaxLength(100)
  neighborhood?: string;

  @ApiPropertyOptional({ example: 'Rift Valley' })
  @IsOptional()
  @IsString()
  @MaxLength(100)
  county?: string;

  @ApiPropertyOptional({ example: -0.2833 })
  @IsOptional()
  @IsNumber()
  @Min(-90)
  @Max(90)
  latitude?: number;

  @ApiPropertyOptional({ example: 36.0833 })
  @IsOptional()
  @IsNumber()
  @Min(-180)
  @Max(180)
  longitude?: number;

  @ApiProperty({ example: 8500 })
  @IsNumber()
  @Min(0)
  pricePerMonth: number;

  @ApiPropertyOptional({ example: 'KES' })
  @IsOptional()
  @IsString()
  @MaxLength(10)
  currency?: string;

  @ApiPropertyOptional({ example: 17000 })
  @IsOptional()
  @IsNumber()
  @Min(0)
  deposit?: number;

  @ApiPropertyOptional({ example: '2025-02-01' })
  @IsOptional()
  @IsDateString()
  availableFrom?: string;

  @ApiPropertyOptional({ example: '2025-12-31' })
  @IsOptional()
  @IsDateString()
  availableTo?: string;

  @ApiPropertyOptional({ example: 1, minimum: 1 })
  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(10)
  bedrooms?: number;

  @ApiPropertyOptional({ example: 1, minimum: 1 })
  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(10)
  bathrooms?: number;

  @ApiPropertyOptional({ example: 2, minimum: 1 })
  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(20)
  maxOccupants?: number;
}
