import {
  Injectable, NotFoundException, ConflictException, ForbiddenException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Club } from './entities/club.entity';
import { ClubMember, ClubMemberRole } from './entities/club-member.entity';
import { CreateClubDto } from './dto/create-club.dto';
import { UpdateClubDto } from './dto/update-club.dto';
import { PaginationDto, paginate, PaginatedResult } from '../../common/dto/pagination.dto';

@Injectable()
export class ClubsService {
  constructor(
    @InjectRepository(Club) private readonly clubRepository: Repository<Club>,
    @InjectRepository(ClubMember) private readonly memberRepository: Repository<ClubMember>,
  ) {}

  async findAll(pagination: PaginationDto): Promise<PaginatedResult<Club>> {
    const { page = 1, limit = 20 } = pagination;
    const [clubs, total] = await this.clubRepository.findAndCount({
      where: { isActive: true },
      order: { membersCount: 'DESC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return paginate(clubs, total, page, limit);
  }

  async findById(id: string): Promise<Club> {
    const club = await this.clubRepository.findOne({
      where: { id, isActive: true },
      relations: ['creator'],
    });
    if (!club) throw new NotFoundException('Club not found');
    return club;
  }

  async createClub(createdBy: string, dto: CreateClubDto): Promise<Club> {
    const club = await this.clubRepository.save(
      this.clubRepository.create({ ...dto, createdBy }),
    );
    await this.memberRepository.save(
      this.memberRepository.create({ clubId: club.id, userId: createdBy, role: ClubMemberRole.LEADER }),
    );
    await this.clubRepository.increment({ id: club.id }, 'membersCount', 1);
    return club;
  }

  async updateClub(id: string, userId: string, dto: UpdateClubDto): Promise<Club> {
    const club = await this.clubRepository.findOne({ where: { id } });
    if (!club) throw new NotFoundException('Club not found');

    const membership = await this.memberRepository.findOne({ where: { clubId: id, userId } });
    if (!membership || membership.role === ClubMemberRole.MEMBER) {
      throw new ForbiddenException('Only club leaders and moderators can update the club');
    }

    Object.assign(club, dto);
    return this.clubRepository.save(club);
  }

  async joinClub(clubId: string, userId: string): Promise<ClubMember> {
    if (!(await this.clubRepository.findOne({ where: { id: clubId, isActive: true } }))) {
      throw new NotFoundException('Club not found');
    }
    if (await this.memberRepository.findOne({ where: { clubId, userId } })) {
      throw new ConflictException('Already a member of this club');
    }
    const member = await this.memberRepository.save(
      this.memberRepository.create({ clubId, userId }),
    );
    await this.clubRepository.increment({ id: clubId }, 'membersCount', 1);
    return member;
  }

  async leaveClub(clubId: string, userId: string): Promise<void> {
    const member = await this.memberRepository.findOne({ where: { clubId, userId } });
    if (!member) throw new NotFoundException('Not a member of this club');
    if (member.role === ClubMemberRole.LEADER) {
      throw new ForbiddenException('Club leaders cannot leave — transfer leadership first');
    }
    await this.memberRepository.remove(member);
    await this.clubRepository.decrement({ id: clubId }, 'membersCount', 1);
  }

  async getMembers(clubId: string, pagination: PaginationDto): Promise<PaginatedResult<ClubMember>> {
    const { page = 1, limit = 20 } = pagination;
    const [members, total] = await this.memberRepository.findAndCount({
      where: { clubId },
      relations: ['user'],
      order: { role: 'ASC', joinedAt: 'ASC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return paginate(members, total, page, limit);
  }
}
