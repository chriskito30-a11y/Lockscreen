import type { ExtractedValue, SourceScanResult } from './types';

const MAX_PREVIEW = 280;
const MAX_BODY_CHARS = 250_000;

function headers(): HeadersInit {
  return {
    'accept': 'application/json, text/plain, */*',
    'user-agent': process.env.MAGIC_USER_AGENT || 'MagicLockscreenAndroid/0.1'
  };
}

export async function fetchSource(sourceUrl: string): Promise<{ raw: string; parsed?: unknown; contentType: 'json' | 'text' }> {
  const res = await fetch(sourceUrl, {
    method: 'GET',
    cache: 'no-store',
    headers: headers(),
    redirect: 'follow'
  });

  if (!res.ok) {
    throw new Error(`Source inaccessible : HTTP ${res.status}`);
  }

  const rawFull = await res.text();
  const raw = rawFull.slice(0, MAX_BODY_CHARS);
  const contentTypeHeader = res.headers.get('content-type') || '';

  if (contentTypeHeader.includes('application/json') || looksLikeJson(raw)) {
    try {
      return { raw, parsed: JSON.parse(raw), contentType: 'json' };
    } catch {
      return { raw, contentType: 'text' };
    }
  }

  return { raw, contentType: 'text' };
}

function looksLikeJson(raw: string): boolean {
  const trimmed = raw.trim();
  return (trimmed.startsWith('{') && trimmed.endsWith('}')) || (trimmed.startsWith('[') && trimmed.endsWith(']'));
}

export function scanParsedSource(raw: string, parsed?: unknown): SourceScanResult {
  if (parsed !== undefined) {
    const paths = collectStringPaths(parsed)
      .filter(item => item.preview.trim().length > 0)
      .slice(0, 80);

    return {
      ok: true,
      contentType: 'json',
      paths,
      rawPreview: stringifyPreview(parsed)
    };
  }

  return {
    ok: true,
    contentType: 'text',
    paths: [{ path: '$text', preview: cleanText(raw).slice(0, MAX_PREVIEW) }],
    rawPreview: cleanText(raw).slice(0, 1200)
  };
}

function collectStringPaths(input: unknown, prefix = ''): Array<{ path: string; preview: string }> {
  const out: Array<{ path: string; preview: string }> = [];

  if (typeof input === 'string' || typeof input === 'number' || typeof input === 'boolean') {
    out.push({ path: prefix || '$', preview: String(input).slice(0, MAX_PREVIEW) });
    return out;
  }

  if (Array.isArray(input)) {
    input.slice(0, 30).forEach((item, index) => {
      out.push(...collectStringPaths(item, prefix ? `${prefix}.${index}` : String(index)));
    });
    return out;
  }

  if (input && typeof input === 'object') {
    for (const [key, value] of Object.entries(input as Record<string, unknown>)) {
      const next = prefix ? `${prefix}.${key}` : key;
      out.push(...collectStringPaths(value, next));
    }
  }

  return out;
}

export function extractValue(raw: string, parsed: unknown | undefined, valuePath?: string): ExtractedValue {
  if (parsed !== undefined) {
    if (valuePath && valuePath !== '$text') {
      const value = getPath(parsed, valuePath);
      const asText = primitiveToText(value);
      if (asText) return { value: asText, sourceKind: 'json-path', path: valuePath };
    }

    const first = collectStringPaths(parsed).find(item => item.preview.trim().length > 0);
    if (first) return { value: first.preview.trim(), sourceKind: 'json-auto', path: first.path };
  }

  const text = cleanText(raw);
  return { value: text, sourceKind: 'text' };
}

function getPath(input: unknown, path: string): unknown {
  if (path === '$') return input;
  const parts = path.split('.').filter(Boolean);
  let current = input as any;
  for (const part of parts) {
    if (current == null) return undefined;
    if (/^\d+$/.test(part) && Array.isArray(current)) {
      current = current[Number(part)];
    } else {
      current = current[part];
    }
  }
  return current;
}

function primitiveToText(value: unknown): string {
  if (typeof value === 'string') return cleanText(value);
  if (typeof value === 'number' || typeof value === 'boolean') return String(value);
  if (value == null) return '';
  return cleanText(JSON.stringify(value));
}

function cleanText(input: string): string {
  return input
    .replace(/<script[\s\S]*?<\/script>/gi, ' ')
    .replace(/<style[\s\S]*?<\/style>/gi, ' ')
    .replace(/<[^>]*>/g, ' ')
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/gi, "'")
    .replace(/\s+/g, ' ')
    .trim();
}

function stringifyPreview(input: unknown): string {
  try {
    return JSON.stringify(input, null, 2).slice(0, 2500);
  } catch {
    return String(input).slice(0, 1200);
  }
}
