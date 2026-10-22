import { registerAs } from '@nestjs/config';

export default registerAs('redis', () => {
  const url = process.env.REDIS_URL ? new URL(process.env.REDIS_URL) : undefined;
  return {
    host     : url?.hostname || process.env.REDIS_HOST || 'localhost',
    port     : url ? (Number(url.port) || 6379) : (parseInt(process.env.REDIS_PORT, 10) || 6379),
    username : url?.username ? decodeURIComponent(url.username) : undefined,
    password : url?.password ? decodeURIComponent(url.password) : (process.env.REDIS_PASSWORD || undefined),
    db       : url ? (Number(url.pathname.slice(1)) || 0) : (parseInt(process.env.REDIS_DB, 10) || 0),
    tls      : url?.protocol === 'rediss:' ? {} : undefined,
    keyPrefix: 'cc:',
  };
});
