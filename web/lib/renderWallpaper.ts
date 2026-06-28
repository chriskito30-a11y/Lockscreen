import sharp from 'sharp';
import type { MagicConfig, WikiImageResult } from './types';

function xmlEscape(input: string): string {
  return input
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

function wrapText(input: string, maxChars: number): string[] {
  const words = input.trim().split(/\s+/).filter(Boolean);
  const lines: string[] = [];
  let line = '';

  for (const word of words) {
    const next = line ? `${line} ${word}` : word;
    if (next.length > maxChars && line) {
      lines.push(line);
      line = word;
    } else {
      line = next;
    }
  }

  if (line) lines.push(line);
  return lines.slice(0, 4);
}

export async function renderWallpaper(params: {
  config: MagicConfig;
  revelation: string;
  wiki: WikiImageResult;
  image?: Buffer;
}): Promise<Buffer> {
  const { config, revelation, wiki, image } = params;
  const width = config.width || 1080;
  const height = config.height || 2400;
  const value = revelation.trim() || 'Révélation';
  const hasPhoto = Boolean(image && image.length > 500);

  let base: sharp.Sharp;

  // Toujours utiliser la photo si elle existe, même avec un ancien token créé en text-only.
  if (hasPhoto && image) {
    base = sharp(image)
      .resize(width, height, { fit: 'cover', position: 'center' })
      .modulate({ brightness: 0.70, saturation: 0.95 });
  } else {
    const fallbackSvg = `
      <svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#020617"/>
            <stop offset="50%" stop-color="#0f172a"/>
            <stop offset="100%" stop-color="#1d4ed8"/>
          </linearGradient>
        </defs>
        <rect width="100%" height="100%" fill="url(#bg)"/>
        <circle cx="${Math.round(width * 0.18)}" cy="${Math.round(height * 0.16)}" r="${Math.round(width * 0.45)}" fill="#2563eb" opacity="0.30"/>
        <circle cx="${Math.round(width * 0.84)}" cy="${Math.round(height * 0.82)}" r="${Math.round(width * 0.56)}" fill="#7c3aed" opacity="0.22"/>
      </svg>
    `;
    base = sharp(Buffer.from(fallbackSvg));
  }

  const overlay = buildOverlay({ width, height, revelation: value, wiki, hasPhoto });

  return base
    .composite([{ input: overlay, top: 0, left: 0 }])
    .png({ quality: 92, compressionLevel: 8 })
    .toBuffer();
}

function buildOverlay(params: {
  width: number;
  height: number;
  revelation: string;
  wiki: WikiImageResult;
  hasPhoto: boolean;
}): Buffer {
  const { width, height, revelation, wiki, hasPhoto } = params;

  const lines = wrapText(revelation, width > 1200 ? 18 : 14);
  const titleSize = Math.max(76, Math.round(width * 0.115));
  const smallSize = Math.max(30, Math.round(width * 0.034));
  const centerX = Math.round(width / 2);

  // Placé haut/centre pour éviter l'horloge Android et les notifications.
  const boxX = Math.round(width * 0.055);
  const boxY = Math.round(height * 0.31);
  const boxW = Math.round(width * 0.89);
  const boxH = Math.round(height * 0.36);
  const firstTextY = boxY + Math.round(boxH * 0.32);
  const lineGap = Math.round(titleSize * 1.12);

  const textElements = lines.map((line, index) => {
    const y = firstTextY + index * lineGap;
    return `
      <text x="${centerX}" y="${y}"
        text-anchor="middle"
        font-family="Arial, Helvetica, sans-serif"
        font-size="${titleSize}"
        font-weight="900"
        fill="#ffffff"
        stroke="#000000"
        stroke-width="7"
        paint-order="stroke fill"
        letter-spacing="-2">${xmlEscape(line)}</text>
    `;
  }).join('\n');

  const statusLabel = hasPhoto ? 'PHOTO OK' : 'SANS PHOTO';
  const statusColor = hasPhoto ? '#16a34a' : '#dc2626';
  const sourceText = hasPhoto && wiki.title
    ? `Photo Wikipédia — ${wiki.title}`
    : wiki.title
      ? `Photo non récupérée — ${wiki.title}`
      : 'Photo non récupérée';

  const svg = `
    <svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="fade" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#000000" stop-opacity="0.20"/>
          <stop offset="45%" stop-color="#000000" stop-opacity="0.18"/>
          <stop offset="100%" stop-color="#000000" stop-opacity="0.80"/>
        </linearGradient>
      </defs>

      <rect width="100%" height="100%" fill="url(#fade)"/>

      <rect x="${boxX}" y="${boxY}" width="${boxW}" height="${boxH}"
        rx="${Math.round(width * 0.06)}" fill="#020617" opacity="0.82"/>

      ${textElements}

      <text x="${centerX}" y="${boxY + boxH - Math.round(boxH * 0.13)}"
        text-anchor="middle"
        font-family="Arial, Helvetica, sans-serif"
        font-size="${smallSize}"
        font-weight="700"
        fill="#e2e8f0"
        stroke="#000000"
        stroke-width="3"
        paint-order="stroke fill">${xmlEscape(sourceText)}</text>

      <rect x="${Math.round(width * 0.07)}" y="${Math.round(height * 0.08)}"
        width="${Math.round(width * 0.34)}" height="${Math.round(height * 0.055)}"
        rx="${Math.round(width * 0.025)}" fill="${statusColor}" opacity="0.95"/>

      <text x="${Math.round(width * 0.24)}" y="${Math.round(height * 0.118)}"
        text-anchor="middle"
        font-family="Arial, Helvetica, sans-serif"
        font-size="${Math.round(width * 0.034)}"
        font-weight="900"
        fill="#ffffff">${statusLabel}</text>
    </svg>
  `;

  return Buffer.from(svg);
}
