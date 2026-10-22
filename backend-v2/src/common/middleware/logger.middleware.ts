import { Injectable, NestMiddleware, Logger } from '@nestjs/common';
import { Request, Response, NextFunction } from 'express';

@Injectable()
export class LoggerMiddleware implements NestMiddleware {
  private readonly logger = new Logger('HTTP');

  use(req: Request, res: Response, next: NextFunction): void {
    const { method, originalUrl } = req;
    const start = Date.now();

    res.on('finish', () => {
      const { statusCode } = res;
      const duration = Date.now() - start;
      const color =
        statusCode >= 500 ? '\x1b[31m' :   // red
        statusCode >= 400 ? '\x1b[33m' :   // yellow
        statusCode >= 300 ? '\x1b[36m' :   // cyan
                            '\x1b[32m';    // green

      this.logger.log(
        `${color}${method}\x1b[0m ${originalUrl} → ${color}${statusCode}\x1b[0m (${duration}ms)`,
      );
    });

    next();
  }
}
