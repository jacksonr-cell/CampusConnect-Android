import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as fs from 'fs/promises';
import * as path from 'path';
import {
  IStorageProvider, UploadOptions, UploadResult,
} from '../storage-provider.interface';

@Injectable()
export class LocalStorageProvider implements IStorageProvider {
  private readonly logger = new Logger(LocalStorageProvider.name);
  private readonly uploadDest: string;
  private readonly baseUrl: string;

  constructor(private readonly configService: ConfigService) {
    this.uploadDest = this.configService.get<string>('storage.local.uploadDest', './uploads');
    this.baseUrl    = this.configService.get<string>('storage.local.baseUrl', 'http://localhost:3000/uploads');
  }

  async upload(buffer: Buffer, options: UploadOptions): Promise<UploadResult> {
    const key     = `${options.folder}/${options.filename}`;
    const fullPath = path.join(this.uploadDest, key);

    await fs.mkdir(path.dirname(fullPath), { recursive: true });
    await fs.writeFile(fullPath, buffer);

    this.logger.debug(`Stored file locally: ${key}`);

    return { url: `${this.baseUrl}/${key}`, key };
  }

  async delete(key: string): Promise<void> {
    const fullPath = path.join(this.uploadDest, key);
    try {
      await fs.unlink(fullPath);
    } catch (error) {
      // File may already be gone — not fatal
      this.logger.warn(`Could not delete local file: ${key}`);
    }
  }
}
