import { Module, NestModule, MiddlewareConsumer } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ThrottlerModule, ThrottlerGuard } from '@nestjs/throttler';
import { APP_GUARD } from '@nestjs/core';

// ── Config files
import databaseConfig from './config/database.config';
import jwtConfig      from './config/jwt.config';
import redisConfig    from './config/redis.config';
import mailConfig     from './config/mail.config';
import storageConfig  from './config/storage.config';
import { validateEnvironment } from './config/env.validation';

// ── Infrastructure
import { LoggerModule }     from './common/logger/logger.module';
import { LoggerMiddleware } from './common/middleware/logger.middleware';

// ── Feature modules
import { AuthModule }          from './modules/auth/auth.module';
import { UsersModule }         from './modules/users/users.module';
import { PostsModule }         from './modules/posts/posts.module';
import { AccommodationModule } from './modules/accommodation/accommodation.module';
import { EventsModule }        from './modules/events/events.module';
import { ClubsModule }         from './modules/clubs/clubs.module';
import { MarketplaceModule }   from './modules/marketplace/marketplace.module';
import { MessagingModule }     from './modules/messaging/messaging.module';
import { NotificationsModule } from './modules/notifications/notifications.module';
import { GamificationModule }  from './modules/gamification/gamification.module';

// ── Round 1 — Infrastructure
import { HealthModule }    from './modules/health/health.module';
import { MailModule }      from './modules/mail/mail.module';
import { QueueModule }     from './modules/queue/queue.module';
import { WebSocketModule } from './modules/websocket/websocket.module';

// ── Round 3 — Storage, Search, Reports, Admin
import { StorageModule } from './modules/storage/storage.module';
import { SearchModule }  from './modules/search/search.module';
import { ReportsModule } from './modules/reports/reports.module';
import { AdminModule }   from './modules/admin/admin.module';

@Module({
  imports: [
    // ── Global config ──────────────────────────────────────────────────
    ConfigModule.forRoot({
      isGlobal  : true,
      load      : [databaseConfig, jwtConfig, redisConfig, mailConfig, storageConfig],
      validate  : validateEnvironment,
      envFilePath: ['.env.local', '.env'],
    }),

    // ── Database ────────────────────────────────────────────────────────
    TypeOrmModule.forRootAsync({
      imports    : [ConfigModule],
      useFactory : (cfg: ConfigService) => ({
        type         : 'postgres',
        url          : cfg.get<string>('database.url') || undefined,
        host         : cfg.get<string>('database.host'),
        port         : cfg.get<number>('database.port'),
        username     : cfg.get<string>('database.username'),
        password     : cfg.get<string>('database.password'),
        database     : cfg.get<string>('database.name'),
        autoLoadEntities: true,
        synchronize  : false,
        migrationsRun: cfg.get<string>('TYPEORM_RUN_MIGRATIONS') === 'true',
        migrations   : [__dirname + '/database/migrations/**/*{.ts,.js}'],
        logging      : cfg.get<string>('NODE_ENV') === 'development',
        ssl          : cfg.get<string>('DATABASE_SSL') === 'true'
          ? { rejectUnauthorized: false }
          : false,
        extra        : {
          max            : 20,
          idleTimeoutMillis: 30000,
          connectionTimeoutMillis: 5000,
        },
      }),
      inject: [ConfigService],
    }),

    // ── Rate limiting ────────────────────────────────────────────────────
    ThrottlerModule.forRoot([
      { name: 'short',  ttl: 1000,  limit: 20  },
      { name: 'medium', ttl: 10000, limit: 100 },
      { name: 'long',   ttl: 60000, limit: 500 },
    ]),

    // ── Infrastructure ───────────────────────────────────────────────────
    LoggerModule,
    MailModule,
    QueueModule,
    HealthModule,
    WebSocketModule,
    StorageModule,

    // ── Feature modules ──────────────────────────────────────────────────
    AuthModule,
    UsersModule,
    PostsModule,
    AccommodationModule,
    EventsModule,
    ClubsModule,
    MarketplaceModule,
    MessagingModule,
    NotificationsModule,
    GamificationModule,

    // ── Round 3 ───────────────────────────────────────────────────────────
    SearchModule,
    ReportsModule,
    AdminModule,
  ],
  providers: [
    { provide: APP_GUARD, useClass: ThrottlerGuard },
  ],
})
export class AppModule implements NestModule {
  configure(consumer: MiddlewareConsumer) {
    consumer.apply(LoggerMiddleware).forRoutes('*');
  }
}
