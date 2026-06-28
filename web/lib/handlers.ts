import { badRequest, jsonResponse, pngResponse } from './http';
import { openConfig, sealConfig } from './token';
import { assertHttpUrl, safeInteger } from './url';
import { downloadImage, findWikipediaImage } from './wikipedia';
import { extractValue, fetchSource, scanParsedSource } from './source';
import { renderWallpaper } from './renderWallpaper';
import type { MagicConfig } from './types';
import crypto from 'node:crypto';

export async function handleScan(request: Request): Promise<Response> {
  try {
    const body = await request.json().catch(() => ({}));
    const sourceUrl = assertHttpUrl(body.sourceUrl, 'URL source');
    const source = await fetchSource(sourceUrl);
    const scan = scanParsedSource(source.raw, source.parsed);
    return jsonResponse(scan);
  } catch (error) {
    return badRequest(error instanceof Error ? error.message : 'Erreur scan');
  }
}

export async function handleConfig(request: Request): Promise<Response> {
  try {
    const body = await request.json().catch(() => ({}));
    const sourceUrl = assertHttpUrl(body.sourceUrl, 'URL source');
    const config: MagicConfig = {
      sourceUrl,
      valuePath: typeof body.valuePath === 'string' ? body.valuePath : undefined,
      wikiLang: typeof body.wikiLang === 'string' && body.wikiLang ? body.wikiLang.slice(0, 6) : 'fr',
      fallbackLang: typeof body.fallbackLang === 'string' && body.fallbackLang ? body.fallbackLang.slice(0, 6) : 'en',
      width: safeInteger(body.width, 1080, 360, 4096),
      height: safeInteger(body.height, 2400, 640, 5000),
      template: body.template === 'text-only' ? 'text-only' : 'wiki-photo',
      createdAt: Date.now()
    };

    const token = sealConfig(config);
    const baseUrl = new URL(request.url).origin;

    return jsonResponse({
      ok: true,
      token,
      valueUrl: `${baseUrl}/api/value?token=${encodeURIComponent(token)}`,
      wallpaperUrl: `${baseUrl}/api/wallpaper?token=${encodeURIComponent(token)}`,
      dynamicValueUrl: `${baseUrl}/api/value/${encodeURIComponent(token)}`,
      dynamicWallpaperUrl: `${baseUrl}/api/wallpaper/${encodeURIComponent(token)}`,
      config: {
        ...config,
        sourceUrl: maskUrl(config.sourceUrl)
      }
    });
  } catch (error) {
    return badRequest(error instanceof Error ? error.message : 'Erreur configuration');
  }
}

export async function handleValue(token: string | null): Promise<Response> {
  try {
    if (!token) throw new Error('Token manquant');
    const config = openConfig(token);
    const source = await fetchSource(config.sourceUrl);
    const extracted = extractValue(source.raw, source.parsed, config.valuePath);
    const value = extracted.value.trim();
    const hash = crypto.createHash('sha256').update(value).digest('hex').slice(0, 16);

    return jsonResponse({
      ok: true,
      value,
      hash,
      sourceKind: extracted.sourceKind,
      path: extracted.path,
      at: new Date().toISOString()
    });
  } catch (error) {
    return badRequest(error instanceof Error ? error.message : 'Erreur valeur');
  }
}

export async function handleWallpaper(token: string | null): Promise<Response> {
  try {
    if (!token) throw new Error('Token manquant');
    const config = openConfig(token);
    const source = await fetchSource(config.sourceUrl);
    const extracted = extractValue(source.raw, source.parsed, config.valuePath);
    const revelation = extracted.value.trim();
    if (!revelation) throw new Error('Aucune révélation détectée');

    const wiki = await findWikipediaImage(revelation, config.wikiLang, config.fallbackLang);
    const image = await downloadImage(wiki.imageUrl);
    const buffer = await renderWallpaper({ config, revelation, wiki, image });

    return pngResponse(buffer, {
      headers: {
        'x-magic-value': encodeURIComponent(revelation.slice(0, 180)),
        'x-magic-wiki-title': encodeURIComponent(wiki.title || ''),
        'x-magic-wiki-lang': wiki.lang
      }
    });
  } catch (error) {
    const fallbackConfig: MagicConfig = {
      sourceUrl: 'about:blank',
      wikiLang: 'fr',
      fallbackLang: 'en',
      width: 1080,
      height: 2400,
      template: 'text-only',
      createdAt: Date.now()
    };
    const message = error instanceof Error ? error.message : 'Erreur wallpaper';
    const buffer = await renderWallpaper({
      config: fallbackConfig,
      revelation: message,
      wiki: { title: 'Erreur', lang: 'fr' }
    });
    return pngResponse(buffer, { status: 200, headers: { 'x-magic-error': encodeURIComponent(message) } });
  }
}

function maskUrl(url: string): string {
  try {
    const parsed = new URL(url);
    const path = parsed.pathname;
    if (path.length <= 8) return `${parsed.origin}/***`;
    return `${parsed.origin}${path.slice(0, 6)}***${path.slice(-8)}`;
  } catch {
    return '***';
  }
}
