import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsString, IsEnum, IsOptional, MaxLength, MinLength } from 'class-validator';
import { ClubCategory } from '../entities/club.entity';

export class CreateClubDto {
  @ApiProperty({ example: 'Kabarak Developers Club' })
  @IsString()
  @MinLength(3)
  @MaxLength(150)
  name: string;

  @ApiPropertyOptional({ example: 'A community for tech enthusiasts and developers on campus.' })
  @IsOptional()
  @IsString()
  @MaxLength(2000)
  description?: string;

  @ApiProperty({ enum: ClubCategory, example: ClubCategory.TECH })
  @IsEnum(ClubCategory)
  category: ClubCategory;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  @MaxLength(200)
  university?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  logoUrl?: string;

  @ApiPropertyOptional()
  @IsOptional()
  @IsString()
  bannerUrl?: string;
}
