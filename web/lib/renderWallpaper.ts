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
  const width = config.width;
  const height = config.height;
  const safeRevelation = revelation.trim() || 'Révélation';

  let base: sharp.Sharp;
  if (image && config.template !== 'text-only') {
    base = sharp(image)
      .resize(width, height, { fit: 'cover', position: 'centre' })
      .modulate({ brightness: 0.78, saturation: 0.92 })
      .blur(0.2);
  } else {
    const bgSvg = Buffer.from(`
      <svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="g" x1="0" x2="1" y1="0" y2="1">
            <stop offset="0" stop-color="#111827"/>
            <stop offset="0.55" stop-color="#1f2937"/>
            <stop offset="1" stop-color="#020617"/>
          </linearGradient>
          <radialGradient id="r" cx="50%" cy="35%" r="60%">
            <stop offset="0" stop-color="#475569" stop-opacity="0.65"/>
            <stop offset="1" stop-color="#020617" stop-opacity="0"/>
          </radialGradient>
        </defs>
        <rect width="100%" height="100%" fill="url(#g)"/>
        <rect width="100%" height="100%" fill="url(#r)"/>
      </svg>`);
    base = sharp(bgSvg);
  }

  const overlay = buildOverlay({ width, height, revelation: safeRevelation, wiki });

  return base
    .composite([{ input: overlay, top: 0, left: 0 }])
    .png({ quality: 92, compressionLevel: 8 })
    .toBuffer();
}

function buildOverlay(params: { width: number; height: number; revelation: string; wiki: WikiImageResult }): Buffer {
  const { width, height, revelation, wiki } = params;
  const titleLines = wrapText(revelation, width > 1200 ? 20 : 16);
  const fontSize = Math.max(58, Math.round(width * 0.088));
  const smallSize = Math.max(22, Math.round(width * 0.027));
  const boxY = Math.round(height * 0.62);
  const boxH = Math.round(height * 0.26);
  const pad = Math.round(width * 0.075);
  const lineGap = Math.round(fontSize * 1.08);
  const startY = boxY + Math.round(boxH * 0.28);

  const tspans = titleLines.map((line, index) => {
    const dy = index === 0 ? 0 : lineGap;
    return `<tspan x="${pad}" dy="${index === 0 ? 0 : dy}">${xmlEscape(line)}</tspan>`;
  }).join('');

  const sourceText = wiki.title ? `Source image : Wikipédia — ${wiki.title}` : 'Magic Lockscreen';

  const svg = `
  <svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
    <defs>
      <linearGradient id="shade" x1="0" x2="0" y1="0" y2="1">
        <stop offset="0" stop-color="#000000" stop-opacity="0.05"/>
        <stop offset="0.55" stop-color="#000000" stop-opacity="0.16"/>
        <stop offset="1" stop-color="#000000" stop-opacity="0.78"/>
      </linearGradient>
      <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%">
        <feDropShadow dx="0" dy="7" stdDeviation="8" flood-color="#000000" flood-opacity="0.7"/>
      </filter>
    </defs>
    <rect width="100%" height="100%" fill="url(#shade)"/>
    <rect x="${Math.round(width * 0.035)}" y="${boxY}" width="${Math.round(width * 0.93)}" height="${boxH}" rx="${Math.round(width * 0.045)}" fill="#000000" opacity="0.42"/>
    <text x="${pad}" y="${startY}" font-family="Arial, Helvetica, sans-serif" font-size="${fontSize}" font-weight="800" fill="#ffffff" filter="url(#shadow)">${tspans}</text>
    <text x="${pad}" y="${boxY + boxH - Math.round(height * 0.045)}" font-family="Arial, Helvetica, sans-serif" font-size="${smallSize}" font-weight="500" fill="#ffffff" opacity="0.78">${xmlEscape(sourceText)}</text>
  </svg>`;

  return Buffer.from(svg);
}
