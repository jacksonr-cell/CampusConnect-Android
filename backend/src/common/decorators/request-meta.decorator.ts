import { createParamDecorator, ExecutionContext } from '@nestjs/common';

export interface RequestMeta {
  ipAddress: string;
  userAgent: string;
}

/** Extracts client IP + user-agent for audit/session purposes. */
export const ReqMeta = createParamDecorator(
  (_data: unknown, ctx: ExecutionContext): RequestMeta => {
    const request = ctx.switchToHttp().getRequest();
    const ipAddress =
      (request.headers['x-forwarded-for'] as string)?.split(',')[0]?.trim() ||
      request.socket?.remoteAddress ||
      request.ip ||
      'unknown';
    const userAgent = request.headers['user-agent'] || 'unknown';
    return { ipAddress, userAgent };
  },
);
