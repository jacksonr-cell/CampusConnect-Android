import {
  Controller, Get, Post, Delete, Body, Param,
  Query, UseGuards, HttpCode, HttpStatus, ParseUUIDPipe,
} from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { PostsService } from './posts.service';
import { CreatePostDto } from './dto/create-post.dto';
import { CreateCommentDto } from './dto/create-comment.dto';
import { ReactPostDto } from './dto/react-post.dto';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { CurrentUser } from '../../common/decorators/current-user.decorator';
import { PaginationDto } from '../../common/dto/pagination.dto';
import { User } from '../users/entities/user.entity';

@ApiTags('posts')
@ApiBearerAuth('access-token')
@UseGuards(JwtAuthGuard)
@Controller('posts')
export class PostsController {
  constructor(private readonly postsService: PostsService) {}

  @Post()
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Create a new post' })
  async createPost(@CurrentUser() user: User, @Body() dto: CreatePostDto) {
    return this.postsService.createPost(user.id, dto);
  }

  @Get('feed')
  @ApiOperation({ summary: 'Get personalised social feed — includes reaction breakdown & your own reaction per post' })
  async getFeed(@CurrentUser() user: User, @Query() pagination: PaginationDto) {
    return this.postsService.getFeed(user.id, pagination);
  }

  @Get('user/:userId')
  @ApiOperation({ summary: "Get a specific user's posts" })
  async getPostsByUser(
    @CurrentUser() user: User,
    @Param('userId', ParseUUIDPipe) userId: string,
    @Query() pagination: PaginationDto,
  ) {
    return this.postsService.getPostsByUser(userId, user.id, pagination);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get a post by ID' })
  async getPost(@CurrentUser() user: User, @Param('id', ParseUUIDPipe) id: string) {
    return this.postsService.getPostById(id, user.id);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Delete a post' })
  async deletePost(@CurrentUser() user: User, @Param('id', ParseUUIDPipe) id: string) {
    await this.postsService.deletePost(id, user);
    return { message: 'Post deleted successfully' };
  }

  @Post(':id/react')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'React to a post (toggles if same type)' })
  async reactToPost(
    @CurrentUser() user: User,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() body: ReactPostDto,
  ) {
    await this.postsService.reactToPost(id, user.id, body.reactionType);
    return { message: 'Reaction updated' };
  }

  @Get(':id/comments')
  @ApiOperation({ summary: 'Get top-level comments on a post' })
  async getComments(
    @CurrentUser() user: User,
    @Param('id', ParseUUIDPipe) id: string,
    @Query() pagination: PaginationDto,
  ) {
    return this.postsService.getComments(id, user.id, pagination);
  }

  @Post(':id/comments')
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Add a comment to a post' })
  async addComment(
    @CurrentUser() user: User,
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: CreateCommentDto,
  ) {
    return this.postsService.addComment(id, user.id, dto);
  }

  @Delete(':postId/comments/:commentId')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Delete a comment' })
  async deleteComment(
    @CurrentUser() user: User,
    @Param('postId', ParseUUIDPipe) postId: string,
    @Param('commentId', ParseUUIDPipe) commentId: string,
  ) {
    await this.postsService.deleteComment(commentId, user);
    return { message: 'Comment deleted successfully' };
  }
}
