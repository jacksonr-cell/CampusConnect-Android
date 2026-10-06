import * as winston from 'winston';
import 'winston-daily-rotate-file';

const { combine, timestamp, errors, json, colorize, printf } = winston.format;

const devFormat = printf(({ level, message, timestamp, context, trace, ...meta }) => {
  const ctx   = context ? `[${context}]` : '';
  const extra = Object.keys(meta).length ? ` ${JSON.stringify(meta)}` : '';
  const err   = trace ? `\n${trace}` : '';
  return `${timestamp} ${level} ${ctx} ${message}${extra}${err}`;
});

/** Daily rotating file transport — errors only */
const errorFileTransport = new winston.transports.DailyRotateFile({
  filename      : 'logs/error-%DATE%.log',
  datePattern   : 'YYYY-MM-DD',
  level         : 'error',
  maxSize       : '20m',
  maxFiles      : '14d',
  zippedArchive : true,
});

/** Daily rotating file transport — all levels */
const combinedFileTransport = new winston.transports.DailyRotateFile({
  filename      : 'logs/combined-%DATE%.log',
  datePattern   : 'YYYY-MM-DD',
  maxSize       : '50m',
  maxFiles      : '30d',
  zippedArchive : true,
});

/** Audit log — security events only */
export const auditFileTransport = new winston.transports.DailyRotateFile({
  filename      : 'logs/audit-%DATE%.log',
  datePattern   : 'YYYY-MM-DD',
  maxSize       : '20m',
  maxFiles      : '90d',
  zippedArchive : true,
});

export function createWinstonConfig(nodeEnv: string): winston.LoggerOptions {
  const isDev = nodeEnv !== 'production';

  return {
    level      : isDev ? 'debug' : 'info',
    format     : combine(
      errors({ stack: true }),
      timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
      isDev ? combine(colorize({ all: true }), devFormat) : json(),
    ),
    transports : isDev
      ? [new winston.transports.Console()]
      : [
          new winston.transports.Console(),
          errorFileTransport,
          combinedFileTransport,
        ],
    exitOnError: false,
  };
}
