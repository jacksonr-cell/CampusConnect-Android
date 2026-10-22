import { registerAs } from '@nestjs/config';

export default registerAs('storage', () => ({
  provider: process.env.STORAGE_PROVIDER || 'local',   // local | s3 | cloudinary

  local: {
    uploadDest : process.env.UPLOAD_DEST || './uploads',
    baseUrl    : process.env.STORAGE_BASE_URL || 'http://localhost:3000/uploads',
  },

  s3: {
    bucket         : process.env.AWS_S3_BUCKET,
    region         : process.env.AWS_S3_REGION || 'auto',
    endpoint       : process.env.AWS_S3_ENDPOINT,
    accessKeyId    : process.env.AWS_ACCESS_KEY_ID,
    secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
    cdnBaseUrl     : process.env.AWS_S3_CDN_URL, // optional CloudFront domain
  },

  cloudinary: {
    cloudName: process.env.CLOUDINARY_CLOUD_NAME,
    apiKey   : process.env.CLOUDINARY_API_KEY,
    apiSecret: process.env.CLOUDINARY_API_SECRET,
  },

  maxFileSizeMb: parseInt(process.env.MAX_FILE_SIZE_MB, 10) || 10,
}));
