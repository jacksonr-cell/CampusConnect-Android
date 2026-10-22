import * as crypto from 'crypto';

const ENCRYPTED_PREFIX = 'enc:v1:';

function deriveKey(key: string): Buffer {
  return crypto.createHash('sha256').update(key, 'utf8').digest();
}

export function isMfaSecretEncrypted(value: string): boolean {
  return value.startsWith(ENCRYPTED_PREFIX);
}

export function encryptMfaSecret(value: string, key?: string): string {
  if (!key) return value; // Local development may omit encryption; production validation requires it.

  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-256-gcm', deriveKey(key), iv);
  const ciphertext = Buffer.concat([cipher.update(value, 'utf8'), cipher.final()]);
  const authTag = cipher.getAuthTag();
  return `${ENCRYPTED_PREFIX}${iv.toString('base64')}:${authTag.toString('base64')}:${ciphertext.toString('base64')}`;
}

export function decryptMfaSecret(value: string, key?: string): string {
  if (!isMfaSecretEncrypted(value)) return value; // Supports pre-encryption local records for gradual upgrade.
  if (!key) throw new Error('MFA_ENCRYPTION_KEY is required to decrypt stored MFA secrets');

  const [iv, authTag, ciphertext] = value.slice(ENCRYPTED_PREFIX.length).split(':').map((part) => Buffer.from(part, 'base64'));
  if (!iv || iv.length !== 12 || !authTag || authTag.length !== 16 || !ciphertext) {
    throw new Error('Stored MFA secret has an invalid encrypted format');
  }

  const decipher = crypto.createDecipheriv('aes-256-gcm', deriveKey(key), iv);
  decipher.setAuthTag(authTag);
  return Buffer.concat([decipher.update(ciphertext), decipher.final()]).toString('utf8');
}
