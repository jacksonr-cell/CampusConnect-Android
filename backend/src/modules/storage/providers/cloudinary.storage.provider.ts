import { Injectable, Logger, InternalServerErrorException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { v2 as cloudinary } from 'cloudinary';
import {
  IStorageProvider, UploadOptions, UploadResult,
} from '../storage-provider.interface';

@Injectable()
export class CloudinaryStorageProvider implements IStorageProvider {
  private readonly logger = new Logger(CloudinaryStorageProvider.name);

  constructor(private readonly configService: ConfigService) {
    cloudinary.config({
      cloud_name: this.configService.get<string>('storage.cloudinary.cloudName'),
      api_key   : this.configService.get<string>('storage.cloudinary.apiKey'),
      api_secret: this.configService.get<string>('storage.cloudinary.apiSecret'),
      secure    : true,
    });
  }

  async upload(buffer: Buffer, options: UploadOptions): Promise<UploadResult> {
    const publicId = `${options.folder}/${options.filename.replace(/\.[^/.]+$/, '')}`;

    return new Promise((resolve, reject) => {
      const stream = cloudinary.uploader.upload_stream(
        { public_id: publicId, resource_type: 'image', overwrite: true },
        (error, result) => {
          if (error || !result) {
            this.logger.error(`Cloudinary upload failed for ${publicId}`, error?.message);
            return reject(new InternalServerErrorException('Failed to upload file to storage'));
          }
          resolve({ url: result.secure_url, key: result.public_id });
        },
      );
      stream.end(buffer);
    });
  }

  async delete(key: string): Promise<void> {
    try {
      await cloudinary.uploader.destroy(key);
    } catch {
      this.logger.warn(`Could not delete Cloudinary asset: ${key}`);
    }
  }
}
