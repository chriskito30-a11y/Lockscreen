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
  const safeRevelation = revelation.trim() || 'Révélation';

  let base: sharp.Sharp;

  // On utilise la photo dès qu'elle existe, même si un ancien token a été créé en text-only.
  if (image && image.length > 500) {
    base = sharp(image)
      .resize(width, height, {
        fit: 'cover',
        position: 'center'
      })
      .modulate({
        brightness: 0.72,
        saturation: 0.98
      });
  } else {
    const bgSvg = Buffer.from(`
      <svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#020617"/>
            <stop offset="48%" stop-color="#0f172a"/>
            <stop offset="100%" stop-color="#1d4ed8"/>
          </linearGradient>
        </defs>
        <rect width="100%" height="100%" fill="url(#bg)"/>
        <circle cx="${Math.round(width * 0.18)}" cy="${Math.round(height * 0.16)}" r="${Math.round(width * 0.45)}" fill="#2563eb" opacity="0.30"/>
        <circle cx="${Math.round(width * 0.85)}" cy="${Math.round(height * 0.82)}" r="${Math.round(width * 0.55)}" fill="#7c3aed" opacity="0.22"/>
      </svg>
    `);

    base = sharp(bgSvg);
  }

  const overlay = buildOverlay({
    width,
    height,
    revelation: safeRevelation,
    wiki,
    hasPhoto: Boolean(image && image.length > 500)
  });

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

  const titleLines = wrapText(revelation, width > 1200 ? 20 : 15);
  const titleSize = Math.max(68, Math.round(width * 0.105));
  const smallSize = Math.max(28, Math.round(width * 0.032));
  const pad = Math.round(width * 0.07);

  // Zone volontairement très visible au centre, pas en bas,
  // pour éviter que Samsung/Android masque le texte avec l'horloge.
  const boxY = Math.round(height * 0.36);
  const boxH = Math.round(height * 0.30);
  const startY = boxY + Math.round(boxH * 0.34);
  const lineGap = Math.round(titleSize * 1.08);

  const tspans = titleLines
    .map((line, index) => {
      const dy = index === 0 ? 0 : lineGap;
      return `<tspan x="${pad}" dy="${dy}">${xmlEscape(line)}</tspan>`;
    })
    .join('');

  const sourceText = hasPhoto && wiki.title
    ? `Photo Wikipédia — ${wiki.title}`
    : wiki.title
      ? `Photo non récupérée — ${wiki.title}`
      : 'Photo non récupérée';

  const photoBadge = hasPhoto ? 'PHOTO OK' : 'SANS PHOTO';

  const svg = `
    <svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="fade" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#000000" stop-opacity="0.22"/>
          <stop offset="45%" stop-color="#000000" stop-opacity="0.18"/>
          <stop offset="100%" stop-color="#000000" stop-opacity="0.78"/>
        </linearGradient>

        <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%">
          <feDropShadow dx="0" dy="12" stdDeviation="14" flood-color="#000000" flood-opacity="0.65"/>
        </filter>
      </defs>

      <rect width="100%" height="100%" fill="url(#fade)"/>

      <rect
        x="${Math.round(width * 0.045)}"
        y="${boxY}"
        width="${Math.round(width * 0.91)}"
        height="${boxH}"
        rx="${Math.round(width * 0.06)}"
        fill="#020617"
        opacity="0.78"
        filter="url(#shadow)"
      />

      <text
        x="${pad}"
        y="${startY}"
        fill="#ffffff"
        font-family="Arial, Helvetica, sans-serif"
        font-size="${titleSize}"
        font-weight="900"
        letter-spacing="-2"
      >${tspans}</text>

      <text
        x="${pad}"
        y="${boxY + boxH - Math.round(boxH * 0.13)}"
        fill="#cbd5e1"
        font-family="Arial, Helvetica, sans-serif"
        font-size="${smallSize}"
        font-weight="600"
      >${xmlEscape(sourceText)}</text>

      <rect
        x="${pad}"
        y="${Math.round(height * 0.08)}"
        width="${Math.round(width * 0.32)}"
        height="${Math.round(height * 0.045)}"
        rx="${Math.round(width * 0.025)}"
        fill="${hasPhoto ? '#16a34a' : '#dc2626'}"
        opacity="0.92"
      />

      <text
        x="${pad + Math.round(width * 0.035)}"
        y="${Math.round(height * 0.111)}"
        fill="#ffffff"
        font-family="Arial, Helvetica, sans-serif"
        font-size="${Math.round(width * 0.032)}"
        font-weight="800"
      >${photoBadge}</text>
    </svg>
  `;

  return Buffer.from(svg);
}
