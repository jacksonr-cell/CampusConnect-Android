import { Injectable, Scope } from '@nestjs/common';
import { WINSTON_MODULE_PROVIDER } from 'nest-winston';
import { Inject } from '@nestjs/common';
import { Logger as WinstonLogger } from 'winston';
import { auditFileTransport } from './winston.config';

@Injectable({ scope: Scope.TRANSIENT })
export class AppLogger {
  private context: string = 'App';

  constructor(
    @Inject(WINSTON_MODULE_PROVIDER)
    private readonly logger: WinstonLogger,
  ) {}

  setContext(context: string) {
    this.context = context;
    return this;
  }

  log(message: string, meta?: Record<string, any>) {
    this.logger.info(message, { context: this.context, ...meta });
  }

  error(message: string, trace?: string, meta?: Record<string, any>) {
    this.logger.error(message, { context: this.context, trace, ...meta });
  }

  warn(message: string, meta?: Record<string, any>) {
    this.logger.warn(message, { context: this.context, ...meta });
  }

  debug(message: string, meta?: Record<string, any>) {
    this.logger.debug(message, { context: this.context, ...meta });
  }

  /** Security-sensitive events written to audit log. */
  audit(event: string, meta: Record<string, any>) {
    const entry = {
      event,
      timestamp: new Date().toISOString(),
      context  : this.context,
      ...meta,
    };
    this.logger.info(JSON.stringify(entry), { context: 'AUDIT' });
    // Also write directly to audit transport
    auditFileTransport.write?.({ level: 'info', message: JSON.stringify(entry) } as any, () => {});
  }
}
