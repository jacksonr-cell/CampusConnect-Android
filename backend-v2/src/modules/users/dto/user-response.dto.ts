import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Expose } from 'class-transformer';
import { UserRole } from '../entities/user.entity';

export class UserResponseDto {
  @ApiProperty() @Expose() id: string;
  @ApiProperty() @Expose() email: string;
  @ApiProperty() @Expose() username: string;
  @ApiProperty() @Expose() firstName: string;
  @ApiProperty() @Expose() lastName: string;
  @ApiPropertyOptional() @Expose() avatarUrl: string;
  @ApiPropertyOptional() @Expose() coverImageUrl: string;
  @ApiPropertyOptional() @Expose() bio: string;
  @ApiPropertyOptional() @Expose() university: string;
  @ApiPropertyOptional() @Expose() studentId: string;
  @ApiPropertyOptional() @Expose() courseOfStudy: string;
  @ApiPropertyOptional() @Expose() yearOfStudy: number;
  @ApiProperty({ enum: UserRole }) @Expose() role: UserRole;
  @ApiProperty() @Expose() isVerified: boolean;
  @ApiProperty() @Expose() reputationPoints: number;
  @ApiProperty() @Expose() followersCount: number;
  @ApiProperty() @Expose() followingCount: number;
  @ApiProperty() @Expose() postsCount: number;
  @ApiPropertyOptional() @Expose() lastSeenAt: Date;
  @ApiProperty() @Expose() createdAt: Date;
}
