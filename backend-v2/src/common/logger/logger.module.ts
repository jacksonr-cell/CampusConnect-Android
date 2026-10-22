import { Module, Global } from '@nestjs/common';
import { WinstonModule } from 'nest-winston';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { AppLogger } from './logger.service';
import { createWinstonConfig } from './winston.config';

@Global()
@Module({
  imports: [
    WinstonModule.forRootAsync({
      imports    : [ConfigModule],
      useFactory : (configService: ConfigService) =>
        createWinstonConfig(configService.get<string>('NODE_ENV', 'development')),
      inject     : [ConfigService],
    }),
  ],
  providers : [AppLogger],
  exports   : [AppLogger, WinstonModule],
})
export class LoggerModule {}
