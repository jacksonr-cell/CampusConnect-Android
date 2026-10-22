import { Injectable } from '@nestjs/common';
import { HealthIndicator, HealthIndicatorResult, HealthCheckError } from '@nestjs/terminus';
import { ConfigService } from '@nestjs/config';
import Redis from 'ioredis';

@Injectable()
export class RedisHealthIndicator extends HealthIndicator {
  private client: Redis;

  constructor(private readonly configService: ConfigService) {
    super();
    this.client = new Redis({
      host: this.configService.get<string>('redis.host'),
      port: this.configService.get<number>('redis.port'),
      username: this.configService.get<string>('redis.username'),
      password: this.configService.get<string>('redis.password'),
      tls: this.configService.get('redis.tls'),
      lazyConnect: true,
      retryStrategy: () => null, // don't retry forever during health checks
    });
  }

  async isHealthy(key: string): Promise<HealthIndicatorResult> {
    try {
      if (this.client.status !== 'ready') {
        await this.client.connect();
      }
      const start = Date.now();
      await this.client.ping();
      const latencyMs = Date.now() - start;

      return this.getStatus(key, true, { latencyMs });
    } catch (error) {
      throw new HealthCheckError(
        'Redis check failed',
        this.getStatus(key, false, { message: (error as Error).message }),
      );
    }
  }
}
