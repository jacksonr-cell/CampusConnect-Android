import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as crypto from 'crypto';
import { IStorageProvider, UploadResult } from './storage-provider.interface';
import { LocalStorageProvider } from './providers/local.storage.provider';
import { S3StorageProvider } from './providers/s3.storage.provider';
import { CloudinaryStorageProvider } from './providers/cloudinary.storage.provider';

@Injectable()
export class StorageService {
  private readonly logger = new Logger(StorageService.name);
  private readonly provider: IStorageProvider;
  private readonly providerName: string;

  constructor(
    private readonly configService: ConfigService,
    private readonly localProvider: LocalStorageProvider,
    private readonly s3Provider: S3StorageProvider,
    private readonly cloudinaryProvider: CloudinaryStorageProvider,
  ) {
    this.providerName = this.configService.get<string>('storage.provider', 'local');

    switch (this.providerName) {
      case 's3':
        this.provider = this.s3Provider;
        break;
      case 'cloudinary':
        this.provider = this.cloudinaryProvider;
        break;
      default:
        this.provider = this.localProvider;
    }

    this.logger.log(`Storage provider active: ${this.providerName}`);
  }

  /** Uploads a processed image buffer under a logical folder. */
  async uploadImage(buffer: Buffer, folder: string, extension = 'webp'): Promise<UploadResult> {
    const filename = `${crypto.randomUUID()}.${extension}`;
    return this.provider.upload(buffer, {
      folder,
      filename,
      contentType: `image/${extension}`,
    });
  }

  async delete(key: string): Promise<void> {
    return this.provider.delete(key);
  }

  getProviderName(): string {
    return this.providerName;
  }
}
