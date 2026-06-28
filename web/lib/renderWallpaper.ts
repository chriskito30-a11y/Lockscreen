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

  if (line) {
    lines.push(line);
  }

  return lines.slice(0, 4);
}

export async function renderWallpaper(params: {
  config: MagicConfig;
  revelation: string;
  wiki: WikiImageResult;
  image?: Buffer;
}): Promise<Buffer> {
  const { config, revelation, wiki, image } = params;
  const width = config.width;
  const height = config.height;
  const safeRevelation = revelation.trim() || 'Révélation';

  let base: sharp.Sharp;

  // Important : dès qu'une image est disponible, on l'utilise.
  // Cela corrige les anciens tokens créés accidentellement en text-only.
  if (image) {
    base = sharp(image)
      .resize(width, height, {
        fit: 'cover',
        position: 'center'
      })
      .modulate({
        brightness: 0.76,
        saturation: 0.98
      })
      .blur(0.2);
  } else {
    const bgSvg = Buffer.from(`
      <svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#020617"/>
            <stop offset="45%" stop-color="#0f172a"/>
            <stop offset="100%" stop-color="#1e3a8a"/>
          </linearGradient>
        </defs>
        <rect width="100%" height="100%" fill="url(#bg)"/>
        <circle cx="${Math.round(width * 0.2)}" cy="${Math.round(height * 0.18)}" r="${Math.round(width * 0.45)}" fill="#2563eb" opacity="0.22"/>
        <circle cx="${Math.round(width * 0.86)}" cy="${Math.round(height * 0.78)}" r="${Math.round(width * 0.52)}" fill="#7c3aed" opacity="0.18"/>
      </svg>
    `);

    base = sharp(bgSvg);
  }

  const overlay = buildOverlay({
    width,
    height,
    revelation: safeRevelation,
    wiki,
    hasPhoto: Boolean(image)
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

  const titleLines = wrapText(revelation, width > 1200 ? 20 : 16);
  const fontSize = Math.max(58, Math.round(width * 0.088));
  const smallSize = Math.max(22, Math.round(width * 0.027));
  const boxY = Math.round(height * 0.60);
  const boxH = Math.round(height * 0.28);
  const pad = Math.round(width * 0.075);
  const lineGap = Math.round(fontSize * 1.08);
  const startY = boxY + Math.round(boxH * 0.30);

  const tspans = titleLines
    .map((line, index) => {
      const dy = index === 0 ? 0 : lineGap;
      return `<tspan x="${pad}" dy="${dy}">${xmlEscape(line)}</tspan>`;
    })
    .join('');

  const sourceText = hasPhoto && wiki.title
    ? `Source image : Wikipédia — ${wiki.title}`
    : wiki.title
      ? `Aucune photo récupérée — ${wiki.title}`
      : 'Aucune photo récupérée';

  const svg = `
    <svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="fade" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#000000" stop-opacity="0.10"/>
          <stop offset="46%" stop-color="#000000" stop-opacity="0.10"/>
          <stop offset="100%" stop-color="#000000" stop-opacity="0.76"/>
        </linearGradient>
        <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%">
          <feDropShadow dx="0" dy="10" stdDeviation="12" flood-color="#000000" flood-opacity="0.55"/>
        </filter>
      </defs>

      <rect width="100%" height="100%" fill="url(#fade)"/>

      <rect
        x="${Math.round(width * 0.045)}"
        y="${boxY}"
        width="${Math.round(width * 0.91)}"
        height="${boxH}"
        rx="${Math.round(width * 0.055)}"
        fill="#020617"
        opacity="0.72"
        filter="url(#shadow)"
      />

      <text
        x="${pad}"
        y="${startY}"
        fill="#ffffff"
        font-family="Arial, Helvetica, sans-serif"
        font-size="${fontSize}"
        font-weight="800"
        letter-spacing="-1"
      >${tspans}</text>

      <text
        x="${pad}"
        y="${boxY + boxH - Math.round(boxH * 0.16)}"
        fill="#cbd5e1"
        font-family="Arial, Helvetica, sans-serif"
        font-size="${smallSize}"
        font-weight="500"
      >${xmlEscape(sourceText)}</text>
    </svg>
  `;

  return Buffer.from(svg);
}
