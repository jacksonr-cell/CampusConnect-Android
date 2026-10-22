import {
  Injectable, NotFoundException, ConflictException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { User } from './entities/user.entity';
import { UserFollow } from './entities/user-follow.entity';
import { UpdateProfileDto } from './dto/update-profile.dto';
import { PaginationDto, paginate, PaginatedResult } from '../../common/dto/pagination.dto';

@Injectable()
export class UsersService {
  constructor(
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
    @InjectRepository(UserFollow)
    private readonly userFollowRepository: Repository<UserFollow>,
  ) {}

  async findById(id: string): Promise<User> {
    const user = await this.userRepository.findOne({ where: { id, isActive: true } });
    if (!user) throw new NotFoundException('User not found');
    return user;
  }

  async findByEmail(email: string): Promise<User | null> {
    return this.userRepository.findOne({ where: { email } });
  }

  async findByUsername(username: string): Promise<User> {
    const user = await this.userRepository.findOne({ where: { username, isActive: true } });
    if (!user) throw new NotFoundException('User not found');
    return user;
  }

  async updateProfile(userId: string, dto: UpdateProfileDto): Promise<User> {
    const user = await this.findById(userId);
    Object.assign(user, dto);
    return this.userRepository.save(user);
  }

  async searchUsers(query: string, pagination: PaginationDto): Promise<PaginatedResult<User>> {
    const { page = 1, limit = 20 } = pagination;
    const skip = (page - 1) * limit;

    const [users, total] = await this.userRepository
      .createQueryBuilder('user')
      .where('user.isActive = :active', { active: true })
      .andWhere(
        '(LOWER(user.username) LIKE LOWER(:q) OR LOWER(user.firstName) LIKE LOWER(:q) OR LOWER(user.lastName) LIKE LOWER(:q))',
        { q: `%${query}%` },
      )
      .select([
        'user.id', 'user.username', 'user.firstName', 'user.lastName',
        'user.avatarUrl', 'user.isVerified', 'user.university', 'user.role',
      ])
      .skip(skip)
      .take(limit)
      .getManyAndCount();

    return paginate(users, total, page, limit);
  }

  async followUser(followerId: string, followingId: string): Promise<void> {
    if (followerId === followingId) throw new ConflictException('You cannot follow yourself');
    await this.findById(followingId);
    const existing = await this.userFollowRepository.findOne({ where: { followerId, followingId } });
    if (existing) throw new ConflictException('Already following this user');
    await this.userFollowRepository.save(this.userFollowRepository.create({ followerId, followingId }));
    await this.userRepository.increment({ id: followerId }, 'followingCount', 1);
    await this.userRepository.increment({ id: followingId }, 'followersCount', 1);
  }

  async unfollowUser(followerId: string, followingId: string): Promise<void> {
    const follow = await this.userFollowRepository.findOne({ where: { followerId, followingId } });
    if (!follow) throw new NotFoundException('Not following this user');
    await this.userFollowRepository.remove(follow);
    await this.userRepository.decrement({ id: followerId }, 'followingCount', 1);
    await this.userRepository.decrement({ id: followingId }, 'followersCount', 1);
  }

  async getFollowers(userId: string, pagination: PaginationDto): Promise<PaginatedResult<User>> {
    const { page = 1, limit = 20 } = pagination;
    const [follows, total] = await this.userFollowRepository.findAndCount({
      where: { followingId: userId },
      relations: ['follower'],
      skip: (page - 1) * limit,
      take: limit,
    });
    return paginate(follows.map((f) => f.follower), total, page, limit);
  }

  async getFollowing(userId: string, pagination: PaginationDto): Promise<PaginatedResult<User>> {
    const { page = 1, limit = 20 } = pagination;
    const [follows, total] = await this.userFollowRepository.findAndCount({
      where: { followerId: userId },
      relations: ['following'],
      skip: (page - 1) * limit,
      take: limit,
    });
    return paginate(follows.map((f) => f.following), total, page, limit);
  }

  sanitizeUser(user: User): Partial<User> {
    const { passwordHash, refreshTokens, ...sanitized } = user as any;
    return sanitized;
  }
}
