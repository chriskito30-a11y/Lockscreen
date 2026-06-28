import type { WikiImageResult } from './types';

type WikiPage = {
  title?: string;
  fullurl?: string;
  thumbnail?: {
    source?: string;
  };
};

function userAgent(): string {
  return process.env.MAGIC_USER_AGENT || 'MagicLockscreenAndroid/0.6 https://lockscreen-mu.vercel.app';
}

function cleanLang(lang: string | undefined, fallback: string): string {
  const cleaned = (lang || fallback).toLowerCase().replace(/[^a-z-]/g, '').slice(0, 8);
  return cleaned || fallback;
}

function wikiApi(lang: string): string {
  return `https://${encodeURIComponent(cleanLang(lang, 'fr'))}.wikipedia.org/w/api.php`;
}

function wikiSummaryApi(lang: string, title: string): string {
  return `https://${encodeURIComponent(cleanLang(lang, 'fr'))}.wikipedia.org/api/rest_v1/page/summary/${encodeURIComponent(title)}`;
}

function normalizeQuery(query: string): string {
  return query
    .replace(/_/g, ' ')
    .replace(/[“”"]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

function languages(primary: string, fallback: string): string[] {
  return Array.from(new Set([
    cleanLang(primary, 'fr'),
    cleanLang(fallback, 'en'),
    'fr',
    'en'
  ].filter(Boolean)));
}

export async function findWikipediaImage(
  query: string,
  lang = 'fr',
  fallbackLang = 'en'
): Promise<WikiImageResult> {
  const cleaned = normalizeQuery(query);
  if (!cleaned) return { title: '', lang: cleanLang(lang, 'fr') };

  let best: WikiImageResult | undefined;

  for (const currentLang of languages(lang, fallbackLang)) {
    const searched = await searchOne(cleaned, currentLang);
    if (searched.imageUrl) return searched;
    if (!best && searched.title) best = searched;

    const exact = await exactTitle(searched.title || cleaned, currentLang);
    if (exact.imageUrl) return exact;
    if (!best && exact.title) best = exact;

    const summary = await summaryImage(searched.title || cleaned, currentLang);
    if (summary.imageUrl) return summary;
    if (!best && summary.title) best = summary;
  }

  return best || { title: cleaned, lang: cleanLang(lang, 'fr') };
}

async function searchOne(query: string, lang: string): Promise<WikiImageResult> {
  const url = new URL(wikiApi(lang));
  url.searchParams.set('action', 'query');
  url.searchParams.set('generator', 'search');
  url.searchParams.set('gsrsearch', query);
  url.searchParams.set('gsrlimit', '1');
  url.searchParams.set('prop', 'pageimages|info');
  url.searchParams.set('inprop', 'url');
  url.searchParams.set('pithumbsize', '2200');
  url.searchParams.set('format', 'json');
  url.searchParams.set('formatversion', '2');
  url.searchParams.set('origin', '*');

  const res = await fetch(url.toString(), {
    cache: 'no-store',
    headers: { accept: 'application/json', 'user-agent': userAgent() }
  });

  if (!res.ok) return { title: query, lang };

  const data = await res.json() as any;
  const page = data?.query?.pages?.[0] as WikiPage | undefined;
  if (!page) return { title: query, lang };

  return {
    title: page.title || query,
    imageUrl: page.thumbnail?.source,
    pageUrl: page.fullurl,
    lang
  };
}

async function exactTitle(query: string, lang: string): Promise<WikiImageResult> {
  const url = new URL(wikiApi(lang));
  url.searchParams.set('action', 'query');
  url.searchParams.set('titles', query);
  url.searchParams.set('redirects', '1');
  url.searchParams.set('prop', 'pageimages|info');
  url.searchParams.set('inprop', 'url');
  url.searchParams.set('pithumbsize', '2200');
  url.searchParams.set('format', 'json');
  url.searchParams.set('formatversion', '2');
  url.searchParams.set('origin', '*');

  const res = await fetch(url.toString(), {
    cache: 'no-store',
    headers: { accept: 'application/json', 'user-agent': userAgent() }
  });

  if (!res.ok) return { title: query, lang };

  const data = await res.json() as any;
  const pages = data?.query?.pages as WikiPage[] | undefined;
  const page = pages?.[0];
  if (!page) return { title: query, lang };

  return {
    title: page.title || query,
    imageUrl: page.thumbnail?.source,
    pageUrl: page.fullurl,
    lang
  };
}

async function summaryImage(title: string, lang: string): Promise<WikiImageResult> {
  const res = await fetch(wikiSummaryApi(lang, title), {
    cache: 'no-store',
    redirect: 'follow',
    headers: { accept: 'application/json', 'user-agent': userAgent() }
  });

  if (!res.ok) return { title, lang };

  const data = await res.json() as any;
  return {
    title: data?.title || title,
    imageUrl: data?.originalimage?.source || data?.thumbnail?.source,
    pageUrl: data?.content_urls?.desktop?.page,
    lang
  };
}

export async function downloadImage(url?: string): Promise<Buffer | undefined> {
  if (!url) return undefined;

  const res = await fetch(url, {
    cache: 'no-store',
    redirect: 'follow',
    headers: {
      accept: 'image/jpeg,image/png,image/webp,image/*,*/*',
      'user-agent': userAgent()
    }
  });

  if (!res.ok) return undefined;

  const contentType = res.headers.get('content-type') || '';
  if (!contentType.startsWith('image/')) return undefined;

  const arr = await res.arrayBuffer();
  const buffer = Buffer.from(arr);
  return buffer.length > 500 ? buffer : undefined;
}
