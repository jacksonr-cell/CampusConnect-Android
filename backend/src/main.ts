import { NestFactory, Reflector } from '@nestjs/core';
import {
  ValidationPipe,
  VersioningType,
  ClassSerializerInterceptor,
} from '@nestjs/common';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { ConfigService } from '@nestjs/config';
import { IoAdapter } from '@nestjs/platform-socket.io';
import { WINSTON_MODULE_NEST_PROVIDER } from 'nest-winston';
import { NestExpressApplication } from '@nestjs/platform-express';
import { join } from 'path';
import helmet from 'helmet';
import { AppModule } from './app.module';
import { AllExceptionsFilter } from './common/filters/all-exceptions.filter';
import { ResponseInterceptor } from './common/interceptors/response.interceptor';

async function bootstrap() {
  const app = await NestFactory.create<NestExpressApplication>(AppModule, {
    bufferLogs: true,   // buffer until Winston is ready
  });

  // ── Winston logger ─────────────────────────────────────────────────────
  app.useLogger(app.get(WINSTON_MODULE_NEST_PROVIDER));

  const configService = app.get(ConfigService);
  const reflector     = app.get(Reflector);

  // ── Static file serving (local storage provider only) ──────────────────
  if (configService.get<string>('storage.provider', 'local') === 'local') {
    app.useStaticAssets(join(process.cwd(), 'uploads'), {
      prefix: '/uploads',
      maxAge: '30d',
    });
  }

  // ── Security ───────────────────────────────────────────────────────────
  app.use(helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc : ["'self'"],
        styleSrc   : ["'self'", "'unsafe-inline'"],
        scriptSrc  : ["'self'"],
        imgSrc     : ["'self'", 'data:', 'https:'],
      },
    },
    crossOriginEmbedderPolicy: false,   // needed for Swagger UI
  }));

  // ── CORS ───────────────────────────────────────────────────────────────
  app.enableCors({
    origin: configService
      .get<string>('CORS_ORIGINS', 'http://localhost:3000')
      .split(',')
      .map(o => o.trim()),
    methods     : ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'x-refresh-token'],
    credentials : true,
  });

  // ── WebSockets ─────────────────────────────────────────────────────────
  app.useWebSocketAdapter(new IoAdapter(app));

  // ── Versioning + global prefix ─────────────────────────────────────────
  app.enableVersioning({ type: VersioningType.URI, defaultVersion: '1' });
  app.setGlobalPrefix('api');

  // ── Global pipes / filters / interceptors ──────────────────────────────
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist             : true,
      forbidNonWhitelisted  : true,
      transform             : true,
      transformOptions      : { enableImplicitConversion: true },
      stopAtFirstError      : false,
    }),
  );
  app.useGlobalFilters(new AllExceptionsFilter());
  app.useGlobalInterceptors(
    new ClassSerializerInterceptor(reflector),
    new ResponseInterceptor(),
  );

  // ── Swagger ────────────────────────────────────────────────────────────
  const swaggerConfig = new DocumentBuilder()
    .setTitle('CampusConnect API')
    .setDescription(
      `CampusConnect REST API v2 — Enterprise-grade student super-app.

**Base URL:** \`/api/v1\`
**WebSocket:** \`ws://host/ws\` (JWT Bearer auth on handshake)

Modules: Auth · Users · Social Feed · Kabarak Nest · Events · Clubs · Marketplace · Messaging · Notifications · Gamification · Health`,
    )
    .setVersion('2.0')
    .addBearerAuth(
      { type: 'http', scheme: 'bearer', bearerFormat: 'JWT' },
      'access-token',
    )
    .addTag('auth',          'Authentication & session management')
    .addTag('users',         'User profiles & social graph')
    .addTag('posts',         'Social feed, comments & reactions')
    .addTag('accommodation', 'Kabarak Nest — listings, bookings & reviews')
    .addTag('events',        'Campus events & RSVPs')
    .addTag('clubs',         'Student clubs & societies')
    .addTag('marketplace',   'Student buy / sell / trade')
    .addTag('messaging',     'Direct & group messaging')
    .addTag('notifications', 'In-app notifications')
    .addTag('gamification',  'Points, badges & leaderboard')
    .addTag('search',        'Global search across all entity types')
    .addTag('reports',       'Content & user reporting')
    .addTag('admin',         '🛡️ Admin dashboard — analytics, user management, moderation')
    .addTag('health',        'Platform health & liveness checks')
    .build();

  const document = SwaggerModule.createDocument(app, swaggerConfig);
  SwaggerModule.setup('api/docs', app, document, {
    swaggerOptions   : { persistAuthorization: true, docExpansion: 'none' },
    customSiteTitle  : 'CampusConnect API v2',
  });

  const port = configService.get<number>('PORT', 3000);
  await app.listen(port);

  const logger = app.get(WINSTON_MODULE_NEST_PROVIDER);
  logger.log(`\n🚀  CampusConnect API  →  http://localhost:${port}/api/v1`, 'Bootstrap');
  logger.log(`📚  Swagger Docs       →  http://localhost:${port}/api/docs`,  'Bootstrap');
  logger.log(`🔌  WebSocket          →  ws://localhost:${port}/ws`,           'Bootstrap');
}

bootstrap();
