import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { NotFoundException, ConflictException } from '@nestjs/common';
import { UsersService } from './users.service';
import { User, UserRole } from './entities/user.entity';
import { UserFollow } from './entities/user-follow.entity';

const mockUser = (id = 'u1'): User => ({
  id, email: `${id}@test.com`, username: id, passwordHash: 'x',
  firstName: 'Test', lastName: 'User', role: UserRole.STUDENT,
  isActive: true, isVerified: false, isEmailVerified: false,
  twoFactorEnabled: false, reputationPoints: 0,
  followersCount: 0, followingCount: 0, postsCount: 0,
  refreshTokens: [], following: [], followers: [],
} as User);

const repo = () => ({
  findOne       : jest.fn(),
  find          : jest.fn(),
  findAndCount  : jest.fn(),
  create        : jest.fn(),
  save          : jest.fn(),
  update        : jest.fn(),
  increment     : jest.fn(),
  decrement     : jest.fn(),
  remove        : jest.fn(),
  createQueryBuilder: jest.fn(() => ({
    where: jest.fn().mockReturnThis(),
    andWhere: jest.fn().mockReturnThis(),
    select: jest.fn().mockReturnThis(),
    skip: jest.fn().mockReturnThis(),
    take: jest.fn().mockReturnThis(),
    getManyAndCount: jest.fn().mockResolvedValue([[], 0]),
  })),
});

describe('UsersService', () => {
  let service: UsersService;
  let userRepo: any;
  let followRepo: any;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        UsersService,
        { provide: getRepositoryToken(User),       useValue: repo() },
        { provide: getRepositoryToken(UserFollow), useValue: repo() },
      ],
    }).compile();

    service    = module.get<UsersService>(UsersService);
    userRepo   = module.get(getRepositoryToken(User));
    followRepo = module.get(getRepositoryToken(UserFollow));
  });

  afterEach(() => jest.clearAllMocks());

  describe('findById', () => {
    it('returns user when found', async () => {
      userRepo.findOne.mockResolvedValue(mockUser());
      const user = await service.findById('u1');
      expect(user.id).toBe('u1');
    });

    it('throws NotFoundException when user does not exist', async () => {
      userRepo.findOne.mockResolvedValue(null);
      await expect(service.findById('ghost')).rejects.toThrow(NotFoundException);
    });
  });

  describe('followUser', () => {
    it('throws ConflictException when trying to follow yourself', async () => {
      await expect(service.followUser('u1', 'u1')).rejects.toThrow(ConflictException);
    });

    it('throws ConflictException when already following', async () => {
      userRepo.findOne.mockResolvedValue(mockUser('u2'));
      followRepo.findOne.mockResolvedValue({ id: 'follow-1' });
      await expect(service.followUser('u1', 'u2')).rejects.toThrow(ConflictException);
    });

    it('creates follow relationship', async () => {
      userRepo.findOne.mockResolvedValue(mockUser('u2'));
      followRepo.findOne.mockResolvedValue(null);
      followRepo.create.mockReturnValue({});
      followRepo.save.mockResolvedValue({});
      userRepo.increment.mockResolvedValue({});

      await service.followUser('u1', 'u2');

      expect(followRepo.save).toHaveBeenCalledTimes(1);
      expect(userRepo.increment).toHaveBeenCalledTimes(2);
    });
  });

  describe('sanitizeUser', () => {
    it('strips passwordHash and refreshTokens', () => {
      const user = { ...mockUser(), passwordHash: 'secret', refreshTokens: [] } as any;
      const result = service.sanitizeUser(user);
      expect(result).not.toHaveProperty('passwordHash');
      expect(result).not.toHaveProperty('refreshTokens');
    });
  });
});
