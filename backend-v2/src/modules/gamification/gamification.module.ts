import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { GamificationService } from './gamification.service';
import { GamificationController } from './gamification.controller';
import { UserPoints } from './entities/user-points.entity';
import { Badge } from './entities/badge.entity';
import { UserBadge } from './entities/user-badge.entity';

@Module({
  imports: [TypeOrmModule.forFeature([UserPoints, Badge, UserBadge])],
  controllers: [GamificationController],
  providers: [GamificationService],
  exports: [GamificationService, TypeOrmModule],
})
export class GamificationModule {}
