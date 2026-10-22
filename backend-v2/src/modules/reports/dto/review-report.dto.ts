import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsEnum, IsOptional, IsString, MaxLength } from 'class-validator';
import { ReportStatus } from '../entities/report.entity';

export class ReviewReportDto {
  @ApiProperty({ enum: [ReportStatus.ACTIONED, ReportStatus.DISMISSED] })
  @IsEnum([ReportStatus.ACTIONED, ReportStatus.DISMISSED])
  status: ReportStatus.ACTIONED | ReportStatus.DISMISSED;

  @ApiPropertyOptional({ maxLength: 2000 })
  @IsOptional()
  @IsString()
  @MaxLength(2000)
  moderatorNote?: string;
}
