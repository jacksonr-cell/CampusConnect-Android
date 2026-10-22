import { Controller, Get } from '@nestjs/common';
import {
  HealthCheck,
  HealthCheckService,
  TypeOrmHealthIndicator,
  MemoryHealthIndicator,
} from '@nestjs/terminus';
import { ApiTags, ApiOperation } from '@nestjs/swagger';
import { Public } from '../../common/decorators/public.decorator';
import { RedisHealthIndicator } from './redis.health';

@ApiTags('health')
@Controller('health')
export class HealthController {
  constructor(
    private readonly health: HealthCheckService,
    private readonly db    : TypeOrmHealthIndicator,
    private readonly memory: MemoryHealthIndicator,
    private readonly redis : RedisHealthIndicator,
  ) {}

  @Get()
  @Public()
  @HealthCheck()
  @ApiOperation({ summary: 'Full platform health check — database, Redis, memory' })
  check() {
    return this.health.check([
      () => this.db.pingCheck('database', { timeout: 3000 }),
      () => this.redis.isHealthy('redis'),
      () => this.memory.checkHeap('memory_heap', 512 * 1024 * 1024),
      () => this.memory.checkRSS('memory_rss', 1024 * 1024 * 1024),
    ]);
  }

  @Get('ping')
  @Public()
  @ApiOperation({ summary: 'Simple liveness check — used by load balancers / Docker healthcheck' })
  ping() {
    return {
      status   : 'ok',
      timestamp: new Date().toISOString(),
      version  : process.env.npm_package_version || '2.0.0',
      uptime   : Math.floor(process.uptime()),
    };
  }
}
