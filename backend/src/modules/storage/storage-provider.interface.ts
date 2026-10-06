export interface UploadResult {
  /** Publicly accessible URL of the stored file. */
  url: string;
  /** Provider-specific key/path — used for deletion. */
  key: string;
}

export interface UploadOptions {
  /** Logical folder, e.g. 'avatars', 'properties', 'posts'. */
  folder     : string;
  /** Desired filename (without extension collisions handled by provider). */
  filename   : string;
  contentType: string;
}

export interface IStorageProvider {
  upload(buffer: Buffer, options: UploadOptions): Promise<UploadResult>;
  delete(key: string): Promise<void>;
}
