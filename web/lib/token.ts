import crypto from 'node:crypto';
import type { MagicConfig } from './types';

function getSecret(): Buffer {
  const secret = process.env.MAGIC_CONFIG_SECRET;
  if (!secret || secret.length < 24) {
    throw new Error('MAGIC_CONFIG_SECRET manquant ou trop court. Définis une valeur longue dans les variables d’environnement.');
  }
  return crypto.createHash('sha256').update(secret).digest();
}

function b64url(input: Buffer): string {
  return input.toString('base64url');
}

function fromB64url(input: string): Buffer {
  return Buffer.from(input, 'base64url');
}

export function sealConfig(config: MagicConfig): string {
  const key = getSecret();
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-256-gcm', key, iv);
  const plaintext = Buffer.from(JSON.stringify(config), 'utf8');
  const encrypted = Buffer.concat([cipher.update(plaintext), cipher.final()]);
  const tag = cipher.getAuthTag();
  return ['v1', b64url(iv), b64url(tag), b64url(encrypted)].join('.');
}

export function openConfig(token: string): MagicConfig {
  if (!token || typeof token !== 'string') {
    throw new Error('Token manquant');
  }

  const parts = token.split('.');
  if (parts.length !== 4 || parts[0] !== 'v1') {
    throw new Error('Token invalide');
  }

  const [, ivText, tagText, encryptedText] = parts;
  const key = getSecret();
  const iv = fromB64url(ivText);
  const tag = fromB64url(tagText);
  const encrypted = fromB64url(encryptedText);

  const decipher = crypto.createDecipheriv('aes-256-gcm', key, iv);
  decipher.setAuthTag(tag);
  const plaintext = Buffer.concat([decipher.update(encrypted), decipher.final()]);
  const parsed = JSON.parse(plaintext.toString('utf8')) as MagicConfig;

  if (!parsed.sourceUrl || !parsed.createdAt) {
    throw new Error('Configuration incomplète');
  }

  return parsed;
}
