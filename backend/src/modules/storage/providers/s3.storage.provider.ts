import { Injectable, Logger, InternalServerErrorException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { S3Client, DeleteObjectCommand } from '@aws-sdk/client-s3';
import { Upload } from '@aws-sdk/lib-storage';
import {
  IStorageProvider, UploadOptions, UploadResult,
} from '../storage-provider.interface';

@Injectable()
export class S3StorageProvider implements IStorageProvider {
  private readonly logger = new Logger(S3StorageProvider.name);
  private readonly client: S3Client;
  private readonly bucket: string;
  private readonly cdnBaseUrl?: string;
  private readonly region: string;
  private readonly endpoint?: string;

  constructor(private readonly configService: ConfigService) {
    this.bucket     = this.configService.get<string>('storage.s3.bucket');
    this.region     = this.configService.get<string>('storage.s3.region');
    this.endpoint   = this.configService.get<string>('storage.s3.endpoint');
    this.cdnBaseUrl = this.configService.get<string>('storage.s3.cdnBaseUrl');

    if (!this.bucket) {
      this.logger.warn('STORAGE_PROVIDER=s3 but AWS_S3_BUCKET is not configured');
    }

    this.client = new S3Client({
      region     : this.region,
      endpoint   : this.endpoint,
      forcePathStyle: Boolean(this.endpoint),
      credentials: {
        accessKeyId    : this.configService.get<string>('storage.s3.accessKeyId'),
        secretAccessKey: this.configService.get<string>('storage.s3.secretAccessKey'),
      },
    });
  }

  async upload(buffer: Buffer, options: UploadOptions): Promise<UploadResult> {
    const key = `${options.folder}/${options.filename}`;

    try {
      const upload = new Upload({
        client: this.client,
        params: {
          Bucket     : this.bucket,
          Key        : key,
          Body       : buffer,
          ContentType: options.contentType,
          CacheControl: 'public, max-age=31536000, immutable',
        },
      });
      await upload.done();
    } catch (error) {
      this.logger.error(`S3 upload failed for key ${key}`, (error as Error).stack);
      throw new InternalServerErrorException('Failed to upload file to storage');
    }

    const url = this.cdnBaseUrl
      ? `${this.cdnBaseUrl.replace(/\/$/, '')}/${key}`
      : `https://${this.bucket}.s3.${this.region}.amazonaws.com/${key}`;

    return { url, key };
  }

  async delete(key: string): Promise<void> {
    try {
      await this.client.send(new DeleteObjectCommand({ Bucket: this.bucket, Key: key }));
    } catch {
      this.logger.warn(`Could not delete S3 object: ${key}`);
    }
  }
}
