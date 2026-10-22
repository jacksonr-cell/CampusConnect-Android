import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { MarketplaceListing, ListingStatus } from './entities/listing.entity';
import { CreateListingDto } from './dto/create-listing.dto';
import { PaginationDto, paginate, PaginatedResult } from '../../common/dto/pagination.dto';

@Injectable()
export class MarketplaceService {
  constructor(
    @InjectRepository(MarketplaceListing)
    private readonly listingRepository: Repository<MarketplaceListing>,
  ) {}

  async findAll(filters: any, pagination: PaginationDto): Promise<PaginatedResult<MarketplaceListing>> {
    const { page = 1, limit = 20 } = pagination;
    const qb = this.listingRepository
      .createQueryBuilder('l')
      .leftJoinAndSelect('l.seller', 'seller')
      .where('l.status = :status', { status: ListingStatus.ACTIVE });

    if (filters?.category) qb.andWhere('l.category = :cat', { cat: filters.category });
    if (filters?.maxPrice) qb.andWhere('l.price <= :max', { max: filters.maxPrice });
    if (filters?.search) {
      qb.andWhere(
        '(LOWER(l.title) LIKE LOWER(:q) OR LOWER(l.description) LIKE LOWER(:q))',
        { q: `%${filters.search}%` },
      );
    }

    const [listings, total] = await qb
      .orderBy('l.isFeatured', 'DESC')
      .addOrderBy('l.createdAt', 'DESC')
      .skip((page - 1) * limit)
      .take(limit)
      .getManyAndCount();

    return paginate(listings, total, page, limit);
  }

  async findById(id: string): Promise<MarketplaceListing> {
    const listing = await this.listingRepository.findOne({
      where: { id, status: ListingStatus.ACTIVE },
      relations: ['seller'],
    });
    if (!listing) throw new NotFoundException('Listing not found');
    await this.listingRepository.increment({ id }, 'viewsCount', 1);
    return listing;
  }

  async createListing(sellerId: string, dto: CreateListingDto): Promise<MarketplaceListing> {
    return this.listingRepository.save(
      this.listingRepository.create({ ...dto, sellerId }),
    );
  }

  async updateListing(id: string, sellerId: string, dto: any): Promise<MarketplaceListing> {
    const listing = await this.listingRepository.findOne({ where: { id } });
    if (!listing) throw new NotFoundException('Listing not found');
    if (listing.sellerId !== sellerId) throw new ForbiddenException();
    Object.assign(listing, dto);
    return this.listingRepository.save(listing);
  }

  async markAsSold(id: string, sellerId: string): Promise<void> {
    const listing = await this.listingRepository.findOne({ where: { id } });
    if (!listing) throw new NotFoundException('Listing not found');
    if (listing.sellerId !== sellerId) throw new ForbiddenException();
    listing.status = ListingStatus.SOLD;
    await this.listingRepository.save(listing);
  }

  async deleteListing(id: string, sellerId: string): Promise<void> {
    const listing = await this.listingRepository.findOne({ where: { id } });
    if (!listing) throw new NotFoundException('Listing not found');
    if (listing.sellerId !== sellerId) throw new ForbiddenException();
    listing.status = ListingStatus.DELETED;
    await this.listingRepository.save(listing);
  }

  async getMyListings(sellerId: string, pagination: PaginationDto): Promise<PaginatedResult<MarketplaceListing>> {
    const { page = 1, limit = 20 } = pagination;
    const [listings, total] = await this.listingRepository.findAndCount({
      where: { sellerId },
      order: { createdAt: 'DESC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return paginate(listings, total, page, limit);
  }
}
