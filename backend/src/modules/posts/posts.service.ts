import {
  Injectable, NotFoundException, ForbiddenException, BadRequestException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, Repository, SelectQueryBuilder } from 'typeorm';
import { Post, PostVisibility } from './entities/post.entity';
import { Comment } from './entities/comment.entity';
import { Reaction, ReactionType } from './entities/reaction.entity';
import { User, UserRole } from '../users/entities/user.entity';
import { CreatePostDto } from './dto/create-post.dto';
import { CreateCommentDto } from './dto/create-comment.dto';
import { PaginationDto, paginate, PaginatedResult } from '../../common/dto/pagination.dto';

/**
 * Post entity enriched with per-request reaction data that isn't a stored
 * column — a breakdown of reaction counts by type, and the requesting
 * user's own reaction (if any). Computed in the service layer rather than
 * stored, so it's always correct for whoever is asking.
 */
export interface EnrichedPost extends Post {
  reactionsBreakdown: Partial<Record<ReactionType, number>>;
  myReaction: ReactionType | null;
}

@Injectable()
export class PostsService {
  constructor(
    @InjectRepository(Post) private readonly postRepository: Repository<Post>,
    @InjectRepository(Comment) private readonly commentRepository: Repository<Comment>,
    @InjectRepository(Reaction) private readonly reactionRepository: Repository<Reaction>,
  ) {}

  async createPost(userId: string, dto: CreatePostDto): Promise<Post> {
    if (!dto.content && (!dto.mediaUrls || dto.mediaUrls.length === 0)) {
      throw new BadRequestException('Post must have content or media');
    }
    const post = await this.postRepository.save(
      this.postRepository.create({ ...dto, userId }),
    );
    return this.postRepository.findOne({ where: { id: post.id }, relations: ['user'] });
  }

  async getFeed(userId: string, pagination: PaginationDto): Promise<PaginatedResult<EnrichedPost>> {
    const { page = 1, limit = 20 } = pagination;
    const query = this.postRepository
      .createQueryBuilder('post')
      .leftJoinAndSelect('post.user', 'user')
      .where('post.isArchived = :archived', { archived: false });
    this.applyVisibilityFilter(query, userId);
    const [posts, total] = await query
      .orderBy('post.createdAt', 'DESC')
      .skip((page - 1) * limit)
      .take(limit)
      .getManyAndCount();

    const enriched = await this.enrichWithReactions(posts, userId);
    return paginate(enriched, total, page, limit);
  }

  async getPostById(id: string, userId: string): Promise<EnrichedPost> {
    const post = await this.findReadablePost(id, userId);
    await this.postRepository.increment({ id }, 'viewsCount', 1);

    const [enrichedPost] = await this.enrichWithReactions([post], userId);
    return enrichedPost;
  }

  /**
   * Posts by a specific user — previously the Android client had to
   * approximate this by scanning global feed pages and filtering
   * client-side, since no dedicated endpoint existed. This is that endpoint.
   */
  async getPostsByUser(
    targetUserId: string,
    requestingUserId: string,
    pagination: PaginationDto,
  ): Promise<PaginatedResult<EnrichedPost>> {
    const { page = 1, limit = 20 } = pagination;
    const query = this.postRepository
      .createQueryBuilder('post')
      .leftJoinAndSelect('post.user', 'user')
      .where('post.userId = :targetUserId', { targetUserId })
      .andWhere('post.isArchived = :archived', { archived: false });
    this.applyVisibilityFilter(query, requestingUserId);
    const [posts, total] = await query
      .orderBy('post.createdAt', 'DESC')
      .skip((page - 1) * limit)
      .take(limit)
      .getManyAndCount();

    const enriched = await this.enrichWithReactions(posts, requestingUserId);
    return paginate(enriched, total, page, limit);
  }

  async deletePost(postId: string, user: User): Promise<void> {
    const post = await this.postRepository.findOne({ where: { id: postId } });
    if (!post) throw new NotFoundException('Post not found');
    if (post.userId !== user.id && user.role !== UserRole.ADMIN) {
      throw new ForbiddenException('You can only delete your own posts');
    }
    await this.postRepository.remove(post);
  }

  async reactToPost(postId: string, userId: string, reactionType: ReactionType): Promise<void> {
    await this.findReadablePost(postId, userId);

    const existing = await this.reactionRepository.findOne({ where: { postId, userId } });

    if (existing) {
      if (existing.reactionType === reactionType) {
        await this.reactionRepository.remove(existing);
        await this.postRepository.decrement({ id: postId }, 'likesCount', 1);
      } else {
        existing.reactionType = reactionType;
        await this.reactionRepository.save(existing);
      }
    } else {
      await this.reactionRepository.save(
        this.reactionRepository.create({ postId, userId, reactionType }),
      );
      await this.postRepository.increment({ id: postId }, 'likesCount', 1);
    }
  }

  async addComment(postId: string, userId: string, dto: CreateCommentDto): Promise<Comment> {
    await this.findReadablePost(postId, userId);
    const comment = await this.commentRepository.save(
      this.commentRepository.create({ postId, userId, ...dto }),
    );
    await this.postRepository.increment({ id: postId }, 'commentsCount', 1);
    return this.commentRepository.findOne({ where: { id: comment.id }, relations: ['user'] });
  }

  async getComments(postId: string, userId: string, pagination: PaginationDto): Promise<PaginatedResult<Comment>> {
    const { page = 1, limit = 20 } = pagination;
    await this.findReadablePost(postId, userId);
    const [comments, total] = await this.commentRepository.findAndCount({
      where: { postId, parentCommentId: null },
      relations: ['user'],
      order: { createdAt: 'ASC' },
      skip: (page - 1) * limit,
      take: limit,
    });
    return paginate(comments, total, page, limit);
  }

  async deleteComment(commentId: string, user: User): Promise<void> {
    const comment = await this.commentRepository.findOne({ where: { id: commentId } });
    if (!comment) throw new NotFoundException('Comment not found');
    if (comment.userId !== user.id && user.role !== UserRole.ADMIN) {
      throw new ForbiddenException('You can only delete your own comments');
    }
    await this.commentRepository.remove(comment);
    await this.postRepository.decrement({ id: comment.postId }, 'commentsCount', 1);
  }

  /**
   * Attaches a per-type reaction breakdown and the requesting user's own
   * reaction to each post, using exactly 2 extra queries total regardless
   * of how many posts are in the page (not N+1) — one grouped count query
   * across all post ids, and one lookup of the requester's own reactions
   * across the same ids.
   */
  private async enrichWithReactions(posts: Post[], requestingUserId: string): Promise<EnrichedPost[]> {
    if (posts.length === 0) return [];
    const postIds = posts.map((p) => p.id);

    const [breakdownRows, myReactions] = await Promise.all([
      this.reactionRepository
        .createQueryBuilder('r')
        .select('r.postId', 'postId')
        .addSelect('r.reactionType', 'reactionType')
        .addSelect('COUNT(*)', 'count')
        .where('r.postId IN (:...postIds)', { postIds })
        .groupBy('r.postId')
        .addGroupBy('r.reactionType')
        .getRawMany<{ postId: string; reactionType: ReactionType; count: string }>(),
      this.reactionRepository.find({
        where: { postId: In(postIds), userId: requestingUserId },
      }),
    ]);

    const breakdownMap = new Map<string, Partial<Record<ReactionType, number>>>();
    for (const row of breakdownRows) {
      const forPost = breakdownMap.get(row.postId) ?? {};
      forPost[row.reactionType] = parseInt(row.count, 10);
      breakdownMap.set(row.postId, forPost);
    }

    const myReactionMap = new Map(myReactions.map((r) => [r.postId, r.reactionType]));

    return posts.map((post) => ({
      ...post,
      reactionsBreakdown: breakdownMap.get(post.id) ?? {},
      myReaction: myReactionMap.get(post.id) ?? null,
    })) as EnrichedPost[];
  }

  private applyVisibilityFilter(query: SelectQueryBuilder<Post>, requestingUserId: string) {
    return query.andWhere(
      `(
        post.visibility = :publicVisibility
        OR post.userId = :requestingUserId
        OR (
          post.visibility = :followersVisibility
          AND EXISTS (
            SELECT 1 FROM user_follows AS follow
            WHERE follow.following_id = post.user_id
              AND follow.follower_id = :requestingUserId
          )
        )
        OR (
          post.visibility = :communityVisibility
          AND EXISTS (
            SELECT 1 FROM communities AS community
            WHERE community.id = post.community_id
              AND community.is_public = true
          )
        )
      )`,
      {
        publicVisibility: PostVisibility.PUBLIC,
        followersVisibility: PostVisibility.FOLLOWERS,
        communityVisibility: PostVisibility.COMMUNITY,
        requestingUserId,
      },
    );
  }

  private async findReadablePost(postId: string, requestingUserId: string): Promise<Post> {
    const query = this.postRepository
      .createQueryBuilder('post')
      .leftJoinAndSelect('post.user', 'user')
      .where('post.id = :postId', { postId })
      .andWhere('post.isArchived = :archived', { archived: false });
    this.applyVisibilityFilter(query, requestingUserId);

    const post = await query.getOne();
    if (!post) throw new NotFoundException('Post not found');
    return post;
  }
}
