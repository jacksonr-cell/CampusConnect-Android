import { NotFoundException } from '@nestjs/common';
import { PostsService } from './posts.service';

describe('PostsService visibility', () => {
  const createQueryBuilder = () => ({
    andWhere: jest.fn().mockReturnThis(),
    leftJoinAndSelect: jest.fn().mockReturnThis(),
    where: jest.fn().mockReturnThis(),
    orderBy: jest.fn().mockReturnThis(),
    skip: jest.fn().mockReturnThis(),
    take: jest.fn().mockReturnThis(),
    getManyAndCount: jest.fn().mockResolvedValue([[], 0]),
    getOne: jest.fn().mockResolvedValue(null),
  });

  it('filters the feed by visibility and the requesting user', async () => {
    const query = createQueryBuilder();
    const service = new PostsService(
      { createQueryBuilder: jest.fn().mockReturnValue(query) } as any,
      {} as any,
      {} as any,
    );

    await service.getFeed('viewer-1', {} as any);

    const visibilityFilter = query.andWhere.mock.calls[0];
    expect(visibilityFilter[0]).toContain('user_follows');
    expect(visibilityFilter[0]).toContain('communities');
    expect(visibilityFilter[1]).toMatchObject({ requestingUserId: 'viewer-1' });
  });

  it('returns not found for a post hidden from the requesting user', async () => {
    const query = createQueryBuilder();
    const service = new PostsService(
      { createQueryBuilder: jest.fn().mockReturnValue(query) } as any,
      {} as any,
      {} as any,
    );

    await expect(service.getPostById('post-1', 'viewer-1')).rejects.toThrow(NotFoundException);

    expect(query.andWhere.mock.calls[1][0]).toContain('user_follows');
    expect(query.andWhere.mock.calls[1][0]).toContain('communities');
  });
});