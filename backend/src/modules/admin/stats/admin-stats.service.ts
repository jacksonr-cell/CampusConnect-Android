import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, MoreThan } from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { Post } from '../../posts/entities/post.entity';
import { Property } from '../../accommodation/entities/property.entity';
import { AccommodationBooking } from '../../accommodation/entities/booking.entity';
import { Event } from '../../events/entities/event.entity';
import { Club } from '../../clubs/entities/club.entity';
import { MarketplaceListing } from '../../marketplace/entities/listing.entity';
import { Report } from '../../reports/entities/report.entity';

@Injectable()
export class AdminStatsService {
  constructor(
    @InjectRepository(User)                 private readonly userRepo: Repository<User>,
    @InjectRepository(Post)                 private readonly postRepo: Repository<Post>,
    @InjectRepository(Property)             private readonly propertyRepo: Repository<Property>,
    @InjectRepository(AccommodationBooking) private readonly bookingRepo: Repository<AccommodationBooking>,
    @InjectRepository(Event)                private readonly eventRepo: Repository<Event>,
    @InjectRepository(Club)                 private readonly clubRepo: Repository<Club>,
    @InjectRepository(MarketplaceListing)   private readonly listingRepo: Repository<MarketplaceListing>,
    @InjectRepository(Report)               private readonly reportRepo: Repository<Report>,
  ) {}

  async getPlatformOverview() {
    const now       = new Date();
    const today     = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const thisWeek  = new Date(today.getTime() - 7  * 24 * 60 * 60 * 1000);
    const thisMonth = new Date(today.getTime() - 30 * 24 * 60 * 60 * 1000);

    const [
      totalUsers, activeToday, activeThisWeek, activeThisMonth, newThisMonth,
      totalPosts, postsThisMonth,
      totalProperties, availableProperties,
      totalBookings, pendingBookings,
      totalEvents, upcomingEvents,
      totalClubs,
      activeListings,
      pendingReports,
    ] = await Promise.all([
      this.userRepo.count({ where: { isActive: true } }),
      this.userRepo.count({ where: { isActive: true, lastSeenAt: MoreThan(today) } }),
      this.userRepo.count({ where: { isActive: true, lastSeenAt: MoreThan(thisWeek) } }),
      this.userRepo.count({ where: { isActive: true, lastSeenAt: MoreThan(thisMonth) } }),
      this.userRepo.count({ where: { isActive: true, createdAt: MoreThan(thisMonth) } }),
      this.postRepo.count({ where: { isArchived: false } }),
      this.postRepo.count({ where: { isArchived: false, createdAt: MoreThan(thisMonth) } }),
      this.propertyRepo.count(),
      this.propertyRepo.count({ where: { isAvailable: true } }),
      this.bookingRepo.count(),
      this.bookingRepo.count({ where: { status: 'pending' as any } }),
      this.eventRepo.count({ where: { isCancelled: false } }),
      this.eventRepo.count({ where: { isCancelled: false, startTime: MoreThan(now) } }),
      this.clubRepo.count({ where: { isActive: true } }),
      this.listingRepo.count({ where: { status: 'active' as any } }),
      this.reportRepo.count({ where: { status: 'pending' as any } }),
    ]);

    return {
      generatedAt: now.toISOString(),
      users: {
        total: totalUsers, activeToday, activeThisWeek, activeThisMonth, newThisMonth,
        retentionRatePercent: totalUsers > 0
          ? Math.round((activeThisMonth / totalUsers) * 100) : 0,
      },
      content: {
        totalPosts, postsThisMonth,
        avgPostsPerActiveUser: activeThisMonth > 0
          ? Math.round(postsThisMonth / activeThisMonth * 10) / 10 : 0,
      },
      kabarakNest: {
        totalProperties, availableProperties,
        occupancyRate: totalProperties > 0
          ? Math.round(((totalProperties - availableProperties) / totalProperties) * 100) : 0,
        totalBookings, pendingBookings,
      },
      events : { totalEvents, upcomingEvents },
      clubs  : { totalClubs },
      marketplace: { activeListings },
      moderation : { pendingReports },
    };
  }

  async getUserGrowthChart(days = 30): Promise<Array<{ date: string; newUsers: number; totalUsers: number }>> {
    const results = await this.userRepo
      .createQueryBuilder('u')
      .select("DATE(u.created_at)", 'date')
      .addSelect('COUNT(*)', 'newUsers')
      .where("u.created_at >= NOW() - INTERVAL ':days days'", { days })
      .groupBy("DATE(u.created_at)")
      .orderBy("DATE(u.created_at)", 'ASC')
      .getRawMany();

    const runningTotal = await this.userRepo.count({
      where: { createdAt: MoreThan(new Date(Date.now() - days * 24 * 60 * 60 * 1000)) },
    });

    return results.map((r, i) => ({
      date      : r.date,
      newUsers  : parseInt(r.newUsers),
      totalUsers: runningTotal + parseInt(r.newUsers) * (i + 1),
    }));
  }

  async getEngagementMetrics() {
    const [
      totalReactions,
      totalComments,
      totalBookmarks,
    ] = await Promise.all([
      this.postRepo
        .createQueryBuilder('p')
        .select('SUM(p.likes_count)', 'total')
        .getRawOne()
        .then(r => parseInt(r?.total ?? '0')),
      this.postRepo
        .createQueryBuilder('p')
        .select('SUM(p.comments_count)', 'total')
        .getRawOne()
        .then(r => parseInt(r?.total ?? '0')),
      this.propertyRepo
        .createQueryBuilder('p')
        .select('SUM(p.inquiries_count)', 'total')
        .getRawOne()
        .then(r => parseInt(r?.total ?? '0')),
    ]);

    return { totalReactions, totalComments, totalPropertyInquiries: totalBookmarks };
  }
}
