import { Injectable, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { User } from '../users/entities/user.entity';
import { Post } from '../posts/entities/post.entity';
import { Event } from '../events/entities/event.entity';
import { Club } from '../clubs/entities/club.entity';
import { Property } from '../accommodation/entities/property.entity';
import { MarketplaceListing } from '../marketplace/entities/listing.entity';
import { SearchQueryDto, SearchEntity } from './dto/search-query.dto';

export interface SearchResult {
  entity : string;
  id     : string;
  title  : string;
  subtitle?: string;
  imageUrl?: string;
  meta?  : Record<string, any>;
}

export interface GlobalSearchResult {
  query    : string;
  total    : number;
  results  : {
    users        ?: SearchResult[];
    posts        ?: SearchResult[];
    events       ?: SearchResult[];
    clubs        ?: SearchResult[];
    accommodation?: SearchResult[];
    marketplace  ?: SearchResult[];
  };
  took: number; // milliseconds
}

@Injectable()
export class SearchService {
  private readonly logger = new Logger(SearchService.name);

  constructor(
    @InjectRepository(User)               private readonly userRepo: Repository<User>,
    @InjectRepository(Post)               private readonly postRepo: Repository<Post>,
    @InjectRepository(Event)              private readonly eventRepo: Repository<Event>,
    @InjectRepository(Club)               private readonly clubRepo: Repository<Club>,
    @InjectRepository(Property)           private readonly propertyRepo: Repository<Property>,
    @InjectRepository(MarketplaceListing) private readonly listingRepo: Repository<MarketplaceListing>,
  ) {}

  async search(dto: SearchQueryDto): Promise<GlobalSearchResult> {
    const start  = Date.now();
    const { q, entity = SearchEntity.ALL, page = 1, limit = 20 } = dto;
    const skip   = (page - 1) * limit;
    const query  = q.trim().toLowerCase();

    const results: GlobalSearchResult['results'] = {};
    let total = 0;

    const shouldSearch = (e: SearchEntity) =>
      entity === SearchEntity.ALL || entity === e;

    await Promise.all([
      shouldSearch(SearchEntity.USERS)  && this.searchUsers(query, skip, limit).then(r => { results.users         = r.items; total += r.count; }),
      shouldSearch(SearchEntity.POSTS)  && this.searchPosts(query, skip, limit).then(r => { results.posts         = r.items; total += r.count; }),
      shouldSearch(SearchEntity.EVENTS) && this.searchEvents(query, skip, limit).then(r => { results.events        = r.items; total += r.count; }),
      shouldSearch(SearchEntity.CLUBS)  && this.searchClubs(query, skip, limit).then(r => { results.clubs         = r.items; total += r.count; }),
      shouldSearch(SearchEntity.ACCOMMODATION) && this.searchAccommodation(query, skip, limit).then(r => { results.accommodation = r.items; total += r.count; }),
      shouldSearch(SearchEntity.MARKETPLACE)   && this.searchMarketplace(query, skip, limit).then(r => { results.marketplace   = r.items; total += r.count; }),
    ]);

    const took = Date.now() - start;
    this.logger.debug(`Search "${q}" (${entity}) → ${total} hits in ${took}ms`);

    return { query: q, total, results, took };
  }

  // ── Per-entity search methods ─────────────────────────────────────────────

  private async searchUsers(q: string, skip: number, limit: number) {
    const [rows, count] = await this.userRepo
      .createQueryBuilder('u')
      .where('u.isActive = true')
      .andWhere(
        `(LOWER(u.username) LIKE :q
          OR LOWER(u.first_name) LIKE :q
          OR LOWER(u.last_name) LIKE :q
          OR LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE :q)`,
        { q: `%${q}%` },
      )
      .select(['u.id','u.username','u.first_name','u.last_name','u.avatar_url','u.university','u.is_verified','u.role'])
      .skip(skip).take(limit)
      .getManyAndCount();

    return {
      count,
      items: rows.map(u => ({
        entity  : 'user',
        id      : u.id,
        title   : `${u.firstName} ${u.lastName}`,
        subtitle: `@${u.username}${u.university ? ` · ${u.university}` : ''}`,
        imageUrl: u.avatarUrl,
        meta    : { isVerified: u.isVerified, role: u.role },
      })),
    };
  }

  private async searchPosts(q: string, skip: number, limit: number) {
    const [rows, count] = await this.postRepo
      .createQueryBuilder('p')
      .leftJoinAndSelect('p.user', 'u')
      .where('p.isArchived = false')
      .andWhere('p.visibility = :vis', { vis: 'public' })
      .andWhere('LOWER(p.content) LIKE :q', { q: `%${q}%` })
      .select(['p.id','p.content','p.created_at','p.likes_count','p.comments_count','u.id','u.username','u.avatar_url'])
      .skip(skip).take(limit)
      .getManyAndCount();

    return {
      count,
      items: rows.map(p => ({
        entity  : 'post',
        id      : p.id,
        title   : (p.content ?? '').slice(0, 120),
        subtitle: `by @${p.user?.username} · ${p.likesCount} likes`,
        meta    : { createdAt: p.createdAt },
      })),
    };
  }

  private async searchEvents(q: string, skip: number, limit: number) {
    const [rows, count] = await this.eventRepo
      .createQueryBuilder('e')
      .where('e.isCancelled = false')
      .andWhere('(LOWER(e.title) LIKE :q OR LOWER(e.description) LIKE :q)', { q: `%${q}%` })
      .skip(skip).take(limit)
      .getManyAndCount();

    return {
      count,
      items: rows.map(e => ({
        entity  : 'event',
        id      : e.id,
        title   : e.title,
        subtitle: `${e.category} · ${new Date(e.startTime).toLocaleDateString('en-KE')}`,
        imageUrl: e.coverImage,
        meta    : { startTime: e.startTime, attendeesCount: e.attendeesCount },
      })),
    };
  }

  private async searchClubs(q: string, skip: number, limit: number) {
    const [rows, count] = await this.clubRepo
      .createQueryBuilder('c')
      .where('c.isActive = true')
      .andWhere('(LOWER(c.name) LIKE :q OR LOWER(c.description) LIKE :q)', { q: `%${q}%` })
      .skip(skip).take(limit)
      .getManyAndCount();

    return {
      count,
      items: rows.map(c => ({
        entity  : 'club',
        id      : c.id,
        title   : c.name,
        subtitle: `${c.category} · ${c.membersCount} members`,
        imageUrl: c.logoUrl,
        meta    : { isVerified: c.isVerified, category: c.category },
      })),
    };
  }

  private async searchAccommodation(q: string, skip: number, limit: number) {
    const [rows, count] = await this.propertyRepo
      .createQueryBuilder('p')
      .leftJoinAndSelect('p.images', 'img', 'img.isPrimary = true')
      .where('p.isAvailable = true')
      .andWhere('(LOWER(p.title) LIKE :q OR LOWER(p.city) LIKE :q OR LOWER(p.neighborhood) LIKE :q OR LOWER(p.description) LIKE :q)', { q: `%${q}%` })
      .skip(skip).take(limit)
      .getManyAndCount();

    return {
      count,
      items: rows.map(p => ({
        entity  : 'accommodation',
        id      : p.id,
        title   : p.title,
        subtitle: `${p.city}${p.neighborhood ? `, ${p.neighborhood}` : ''} · KES ${Number(p.pricePerMonth).toLocaleString()}/mo`,
        imageUrl: p.images?.[0]?.imageUrl,
        meta    : { propertyType: p.propertyType, pricePerMonth: p.pricePerMonth, isVerified: p.isVerified },
      })),
    };
  }

  private async searchMarketplace(q: string, skip: number, limit: number) {
    const [rows, count] = await this.listingRepo
      .createQueryBuilder('l')
      .where("l.status = 'active'")
      .andWhere('(LOWER(l.title) LIKE :q OR LOWER(l.description) LIKE :q)', { q: `%${q}%` })
      .skip(skip).take(limit)
      .getManyAndCount();

    return {
      count,
      items: rows.map(l => ({
        entity  : 'marketplace',
        id      : l.id,
        title   : l.title,
        subtitle: `KES ${Number(l.price).toLocaleString()} · ${l.category}`,
        imageUrl: (l.imageUrls as string[])?.[0],
        meta    : { price: l.price, category: l.category, condition: l.condition },
      })),
    };
  }
}
