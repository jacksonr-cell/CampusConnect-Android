import {
  Injectable, NotFoundException, ForbiddenException, ConflictException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Property } from './entities/property.entity';
import { AccommodationBooking, BookingStatus } from './entities/booking.entity';
import { PropertyReview } from './entities/property-review.entity';
import { SavedListing } from './entities/saved-listing.entity';
import { PaginationDto, paginate, PaginatedResult } from '../../common/dto/pagination.dto';

@Injectable()
export class AccommodationService {
  constructor(
    @InjectRepository(Property) private readonly propertyRepository: Repository<Property>,
    @InjectRepository(AccommodationBooking) private readonly bookingRepository: Repository<AccommodationBooking>,
    @InjectRepository(PropertyReview) private readonly reviewRepository: Repository<PropertyReview>,
    @InjectRepository(SavedListing) private readonly savedRepository: Repository<SavedListing>,
  ) {}

  async findAll(filters: any, pagination: PaginationDto): Promise<PaginatedResult<Property>> {
    const { page = 1, limit = 20 } = pagination;
    const qb = this.propertyRepository
      .createQueryBuilder('p')
      .leftJoinAndSelect('p.images', 'img', 'img.isPrimary = true')
      .leftJoinAndSelect('p.landlord', 'landlord')
      .where('p.isAvailable = :available', { available: true });

    if (filters?.city) qb.andWhere('LOWER(p.city) = LOWER(:city)', { city: filters.city });
    if (filters?.propertyType) qb.andWhere('p.propertyType = :type', { type: filters.propertyType });
    if (filters?.minPrice) qb.andWhere('p.pricePerMonth >= :min', { min: filters.minPrice });
    if (filters?.maxPrice) qb.andWhere('p.pricePerMonth <= :max', { max: filters.maxPrice });

    const [props, total] = await qb
      .orderBy('p.isFeatured', 'DESC')
      .addOrderBy('p.ratingAverage', 'DESC')
      .addOrderBy('p.createdAt', 'DESC')
      .skip((page - 1) * limit)
      .take(limit)
      .getManyAndCount();

    return paginate(props, total, page, limit);
  }

  async findById(id: string): Promise<Property> {
    const property = await this.propertyRepository.findOne({
      where: { id },
      relations: ['images', 'amenities', 'landlord'],
    });
    if (!property) throw new NotFoundException('Property not found');
    await this.propertyRepository.increment({ id }, 'viewsCount', 1);
    return property;
  }

  async createProperty(landlordId: string, dto: any): Promise<Property> {
    return this.propertyRepository.save(
      this.propertyRepository.create({ ...dto, landlordId }),
    );
  }

  async updateProperty(id: string, landlordId: string, dto: any): Promise<Property> {
    const property = await this.propertyRepository.findOne({ where: { id } });
    if (!property) throw new NotFoundException('Property not found');
    if (property.landlordId !== landlordId) throw new ForbiddenException();
    Object.assign(property, dto);
    return this.propertyRepository.save(property);
  }

  async deleteProperty(id: string, landlordId: string): Promise<void> {
    const property = await this.propertyRepository.findOne({ where: { id } });
    if (!property) throw new NotFoundException('Property not found');
    if (property.landlordId !== landlordId) throw new ForbiddenException();
    await this.propertyRepository.remove(property);
  }

  async createInquiry(propertyId: string, studentId: string, dto: any): Promise<AccommodationBooking> {
    const property = await this.propertyRepository.findOne({ where: { id: propertyId } });
    if (!property) throw new NotFoundException('Property not found');
    await this.propertyRepository.increment({ id: propertyId }, 'inquiriesCount', 1);
    return this.bookingRepository.save(
      this.bookingRepository.create({ propertyId, studentId, status: BookingStatus.INQUIRY, ...dto }),
    );
  }

  async saveProperty(userId: string, propertyId: string): Promise<void> {
    if (!(await this.propertyRepository.findOne({ where: { id: propertyId } }))) {
      throw new NotFoundException('Property not found');
    }
    if (await this.savedRepository.findOne({ where: { userId, propertyId } })) {
      throw new ConflictException('Property already saved');
    }
    await this.savedRepository.save(this.savedRepository.create({ userId, propertyId }));
  }

  async unsaveProperty(userId: string, propertyId: string): Promise<void> {
    const saved = await this.savedRepository.findOne({ where: { userId, propertyId } });
    if (!saved) throw new NotFoundException('Not in saved listings');
    await this.savedRepository.remove(saved);
  }

  async getSavedProperties(userId: string, pagination: PaginationDto): Promise<PaginatedResult<Property>> {
    const { page = 1, limit = 20 } = pagination;
    const [saved, total] = await this.savedRepository.findAndCount({
      where: { userId },
      relations: ['property', 'property.images'],
      skip: (page - 1) * limit,
      take: limit,
      order: { savedAt: 'DESC' },
    });
    return paginate(saved.map((s) => s.property), total, page, limit);
  }

  async addReview(propertyId: string, reviewerId: string, dto: any): Promise<PropertyReview> {
    const property = await this.propertyRepository.findOne({ where: { id: propertyId } });
    if (!property) throw new NotFoundException('Property not found');
    if (await this.reviewRepository.findOne({ where: { propertyId, reviewerId } })) {
      throw new ConflictException('You have already reviewed this property');
    }
    const review = await this.reviewRepository.save(
      this.reviewRepository.create({ propertyId, reviewerId, ...dto }),
    );
    // Recalculate average rating
    const reviews = await this.reviewRepository.find({ where: { propertyId } });
    const avg = reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length;
    await this.propertyRepository.update(propertyId, {
      ratingAverage: Math.round(avg * 10) / 10,
      reviewsCount: reviews.length,
    });
    return review;
  }

  async getReviews(propertyId: string, pagination: PaginationDto): Promise<PaginatedResult<PropertyReview>> {
    const { page = 1, limit = 20 } = pagination;
    const [reviews, total] = await this.reviewRepository.findAndCount({
      where: { propertyId },
      relations: ['reviewer'],
      order: { createdAt: 'DESC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return paginate(reviews, total, page, limit);
  }
}
