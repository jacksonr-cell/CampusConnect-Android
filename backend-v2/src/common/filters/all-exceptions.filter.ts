import {
  ExceptionFilter,
  Catch,
  ArgumentsHost,
  HttpException,
  HttpStatus,
  Logger,
} from '@nestjs/common';
import { Request, Response } from 'express';
import { QueryFailedError, EntityNotFoundError } from 'typeorm';

@Catch()
export class AllExceptionsFilter implements ExceptionFilter {
  private readonly logger = new Logger(AllExceptionsFilter.name);

  catch(exception: unknown, host: ArgumentsHost): void {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();
    const request = ctx.getRequest<Request>();

    let status = HttpStatus.INTERNAL_SERVER_ERROR;
    let message: string | string[] = 'Internal server error';

    // ── HTTP Exceptions (NestJS / class-validator) ──────────────────────
    if (exception instanceof HttpException) {
      status = exception.getStatus();
      const res = exception.getResponse();
      message =
        typeof res === 'string'
          ? res
          : (res as any).message || exception.message;
    }

    // ── TypeORM: unique constraint violation ────────────────────────────
    else if (exception instanceof QueryFailedError) {
      const err = exception as any;
      if (err.code === '23505') {
        status = HttpStatus.CONFLICT;
        message = this.extractUniqueConstraintMessage(err.detail || err.message);
      } else if (err.code === '23503') {
        status = HttpStatus.BAD_REQUEST;
        message = 'Referenced resource does not exist';
      } else if (err.code === '23502') {
        status = HttpStatus.BAD_REQUEST;
        message = 'A required field is missing';
      } else {
        status = HttpStatus.INTERNAL_SERVER_ERROR;
        message = 'Database operation failed';
        this.logger.error('QueryFailedError', err.message, err.stack);
      }
    }

    // ── TypeORM: entity not found ───────────────────────────────────────
    else if (exception instanceof EntityNotFoundError) {
      status = HttpStatus.NOT_FOUND;
      message = 'Resource not found';
    }

    // ── Unexpected errors ───────────────────────────────────────────────
    else {
      this.logger.error(
        `Unhandled exception on ${request.method} ${request.url}`,
        exception instanceof Error ? exception.stack : String(exception),
      );
    }

    response.status(status).json({
      success: false,
      statusCode: status,
      timestamp: new Date().toISOString(),
      path: request.url,
      method: request.method,
      error: Array.isArray(message) ? message : [message],
    });
  }

  private extractUniqueConstraintMessage(detail: string): string {
    // PostgreSQL detail: Key (email)=(x@y.com) already exists.
    const match = detail?.match(/Key \((.+?)\)=\(.+?\) already exists/);
    if (match) {
      const field = match[1].replace(/_/g, ' ');
      return `This ${field} is already in use`;
    }
    return 'A record with these details already exists';
  }
}
