import { registerAs } from '@nestjs/config';

export default registerAs('jwt', () => ({
  accessSecret: process.env.JWT_ACCESS_SECRET || 'campus_connect_access_secret_CHANGE_IN_PROD',
  refreshSecret: process.env.JWT_REFRESH_SECRET || 'campus_connect_refresh_secret_CHANGE_IN_PROD',
  accessExpiresIn: process.env.JWT_ACCESS_EXPIRES_IN || '15m',
  refreshExpiresIn: process.env.JWT_REFRESH_EXPIRES_IN || '7d',
}));
