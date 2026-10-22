import { registerAs } from '@nestjs/config';

export default registerAs('database', () => ({
  url: process.env.DATABASE_URL || undefined,
  host: process.env.DATABASE_HOST || 'localhost',
  port: parseInt(process.env.DATABASE_PORT, 10) || 5432,
  username: process.env.DATABASE_USERNAME || 'campusconnect',
  password: process.env.DATABASE_PASSWORD || 'campusconnect_dev',
  name: process.env.DATABASE_NAME || 'campusconnect',
}));
