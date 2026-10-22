import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { User, UserRole } from '../../users/entities/user.entity';
import { PaginationDto, paginate } from '../../../common/dto/pagination.dto';
import { AppLogger } from '../../../common/logger/logger.service';

@Injectable()
export class AdminUsersService {
  constructor(
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
    private readonly logger: AppLogger,
  ) {
    this.logger.setContext('AdminUsersService');
  }

  async listUsers(filters: {
    search?  : string;
    role?    : UserRole;
    isActive?: boolean;
  }, pagination: PaginationDto) {
    const { page = 1, limit = 20 } = pagination;
    const qb = this.userRepository.createQueryBuilder('u');

    if (filters.search) {
      qb.where(
        '(LOWER(u.email) LIKE :q OR LOWER(u.username) LIKE :q OR LOWER(CONCAT(u.first_name,\' \',u.last_name)) LIKE :q)',
        { q: `%${filters.search.toLowerCase()}%` },
      );
    }
    if (filters.role !== undefined)     qb.andWhere('u.role = :role', { role: filters.role });
    if (filters.isActive !== undefined) qb.andWhere('u.isActive = :active', { active: filters.isActive });

    const [users, total] = await qb
      .orderBy('u.created_at', 'DESC')
      .skip((page - 1) * limit)
      .take(limit)
      .getManyAndCount();

    return paginate(users, total, page, limit);
  }

  async getUserDetail(userId: string): Promise<User> {
    const user = await this.userRepository.findOne({ where: { id: userId } });
    if (!user) throw new NotFoundException('User not found');
    return user;
  }

  async setActiveStatus(userId: string, isActive: boolean, adminId: string): Promise<void> {
    const user = await this.userRepository.findOne({ where: { id: userId } });
    if (!user) throw new NotFoundException('User not found');
    user.isActive = isActive;
    await this.userRepository.save(user);
    this.logger.audit(isActive ? 'admin_user_activated' : 'admin_user_deactivated', {
      targetUserId: userId, adminId,
    });
  }

  async setVerified(userId: string, isVerified: boolean, adminId: string): Promise<void> {
    const user = await this.userRepository.findOne({ where: { id: userId } });
    if (!user) throw new NotFoundException('User not found');
    user.isVerified = isVerified;
    await this.userRepository.save(user);
    this.logger.audit('admin_user_verification_changed', {
      targetUserId: userId, isVerified, adminId,
    });
  }

  async changeRole(userId: string, role: UserRole, adminId: string): Promise<void> {
    const user = await this.userRepository.findOne({ where: { id: userId } });
    if (!user) throw new NotFoundException('User not found');
    const previousRole = user.role;
    user.role = role;
    await this.userRepository.save(user);
    this.logger.audit('admin_role_changed', {
      targetUserId: userId, previousRole, newRole: role, adminId,
    });
  }

  async deleteUser(userId: string, adminId: string): Promise<void> {
    const user = await this.userRepository.findOne({ where: { id: userId } });
    if (!user) throw new NotFoundException('User not found');
    // Soft delete — deactivate rather than hard delete to preserve data integrity
    user.isActive = false;
    user.email    = `deleted_${userId}@campusconnect.deleted`;
    user.username = `deleted_${userId}`;
    await this.userRepository.save(user);
    this.logger.audit('admin_user_deleted', { targetUserId: userId, adminId });
  }
}
