import { Injectable, ConflictException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Report, ReportStatus } from './entities/report.entity';
import { CreateReportDto } from './dto/create-report.dto';
import { PaginationDto, paginate } from '../../common/dto/pagination.dto';

@Injectable()
export class ReportsService {
  constructor(
    @InjectRepository(Report)
    private readonly reportRepository: Repository<Report>,
  ) {}

  async createReport(reporterId: string, dto: CreateReportDto): Promise<Report> {
    const existing = await this.reportRepository.findOne({
      where: { reporterId, targetType: dto.targetType, targetId: dto.targetId, status: ReportStatus.PENDING },
    });
    if (existing) throw new ConflictException('You have already reported this content');

    return this.reportRepository.save(
      this.reportRepository.create({ reporterId, ...dto }),
    );
  }

  async getPendingReports(pagination: PaginationDto) {
    const { page = 1, limit = 20 } = pagination;
    const [reports, total] = await this.reportRepository.findAndCount({
      where    : { status: ReportStatus.PENDING },
      relations: ['reporter'],
      order    : { createdAt: 'ASC' },
      skip     : (page - 1) * limit,
      take     : limit,
    });
    return paginate(reports, total, page, limit);
  }

  async reviewReport(
    reportId: string,
    reviewerId: string,
    status: ReportStatus,
    moderatorNote?: string,
  ): Promise<Report> {
    const report = await this.reportRepository.findOne({ where: { id: reportId } });
    if (!report) throw new Error('Report not found');
    report.status        = status;
    report.reviewedBy    = reviewerId;
    report.moderatorNote = moderatorNote;
    report.reviewedAt    = new Date();
    return this.reportRepository.save(report);
  }
}
