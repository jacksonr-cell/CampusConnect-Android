import { Module, Global } from '@nestjs/common';
import { StorageService } from './storage.service';
import { LocalStorageProvider } from './providers/local.storage.provider';
import { S3StorageProvider } from './providers/s3.storage.provider';
import { CloudinaryStorageProvider } from './providers/cloudinary.storage.provider';

@Global()
@Module({
  providers: [
    StorageService,
    LocalStorageProvider,
    S3StorageProvider,
    CloudinaryStorageProvider,
  ],
  exports: [StorageService],
})
export class StorageModule {}
