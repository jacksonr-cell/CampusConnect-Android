import { registerAs } from '@nestjs/config';

export default registerAs('mail', () => ({
  enabled : process.env.EMAIL_ENABLED !== 'false',
  host    : process.env.SMTP_HOST     || 'smtp.gmail.com',
  port    : parseInt(process.env.SMTP_PORT, 10) || 587,
  secure  : process.env.SMTP_SECURE   === 'true',
  user    : process.env.SMTP_USER     || '',
  pass    : process.env.SMTP_PASS     || '',
  from    : process.env.EMAIL_FROM    || 'CampusConnect <noreply@campusconnect.app>',
  appName : process.env.APP_NAME      || 'CampusConnect',
  appUrl  : process.env.APP_URL       || 'https://campusconnect.app',
}));
