export function validateEnvironment(env: Record<string, unknown>): Record<string, unknown> {
  if (env.NODE_ENV !== 'production') return env;

  const demoMode = env.DEMO_MODE === 'true';
  const required = [
    'DATABASE_URL',
    'REDIS_URL',
    'JWT_ACCESS_SECRET',
    'JWT_REFRESH_SECRET',
    'MFA_ENCRYPTION_KEY',
  ];
  if (!demoMode) {
    required.push(
      'SMTP_HOST', 'SMTP_USER', 'SMTP_PASS', 'EMAIL_FROM',
      'AWS_S3_BUCKET', 'AWS_S3_ENDPOINT', 'AWS_S3_CDN_URL',
      'AWS_ACCESS_KEY_ID', 'AWS_SECRET_ACCESS_KEY',
    );
  }
  const missing = required.filter((key) =>
    typeof env[key] !== 'string' || (env[key] as string).trim().length === 0,
  );
  if (missing.length) {
    throw new Error(`Missing required production environment variables: ${missing.join(', ')}`);
  }

  const accessSecret = String(env.JWT_ACCESS_SECRET);
  const refreshSecret = String(env.JWT_REFRESH_SECRET);
  for (const [key, value] of [['JWT_ACCESS_SECRET', accessSecret], ['JWT_REFRESH_SECRET', refreshSecret]]) {
    if (value.length < 32 || /change|example|campus_connect|development/i.test(value)) {
      throw new Error(`${key} must be a unique, randomly generated secret of at least 32 characters`);
    }
  }
  if (accessSecret === refreshSecret) {
    throw new Error('JWT_ACCESS_SECRET and JWT_REFRESH_SECRET must be different');
  }
  if (String(env.MFA_ENCRYPTION_KEY).length < 32) {
    throw new Error('MFA_ENCRYPTION_KEY must contain at least 32 characters');
  }
  if (!String(env.DATABASE_URL).startsWith('postgres')) {
    throw new Error('DATABASE_URL must be a PostgreSQL connection URL');
  }
  if (!['local', 's3', 'cloudinary'].includes(String(env.STORAGE_PROVIDER || 'local'))) {
    throw new Error('STORAGE_PROVIDER must be local, s3, or cloudinary');
  }
  if (!demoMode && env.STORAGE_PROVIDER !== 's3') {
    throw new Error('Production file storage must use the configured S3-compatible object store');
  }
  if (env.STORAGE_PROVIDER === 's3') {
    const storageRequired = [
      'AWS_S3_BUCKET', 'AWS_S3_ENDPOINT', 'AWS_S3_CDN_URL',
      'AWS_ACCESS_KEY_ID', 'AWS_SECRET_ACCESS_KEY',
    ];
    const missingStorage = storageRequired.filter((key) =>
      typeof env[key] !== 'string' || (env[key] as string).trim().length === 0,
    );
    if (missingStorage.length) {
      throw new Error(`S3 storage is missing required environment variables: ${missingStorage.join(', ')}`);
    }
    if (!String(env.AWS_S3_ENDPOINT).startsWith('https://') || !String(env.AWS_S3_CDN_URL).startsWith('https://')) {
      throw new Error('S3 endpoint and public media URL must use HTTPS in production');
    }
  }

  return env;
}
