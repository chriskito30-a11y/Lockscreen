import type { WikiImageResult } from './types';

function userAgent(): string {
  return process.env.MAGIC_USER_AGENT || 'MagicLockscreenAndroid/0.1';
}

function wikiApi(lang: string): string {
  return `https://${encodeURIComponent(lang)}.wikipedia.org/w/api.php`;
}

export async function findWikipediaImage(query: string, lang = 'fr', fallbackLang = 'en'): Promise<WikiImageResult> {
  const cleaned = query.trim();
  if (!cleaned) return { title: '', lang };

  const first = await searchOne(cleaned, lang);
  if (first.imageUrl) return first;

  if (fallbackLang && fallbackLang !== lang) {
    const second = await searchOne(cleaned, fallbackLang);
    if (second.imageUrl || second.title) return second;
  }

  return first.title ? first : { title: cleaned, lang };
}

async function searchOne(query: string, lang: string): Promise<WikiImageResult> {
  const url = new URL(wikiApi(lang));
  url.searchParams.set('action', 'query');
  url.searchParams.set('generator', 'search');
  url.searchParams.set('gsrsearch', query);
  url.searchParams.set('gsrlimit', '1');
  url.searchParams.set('prop', 'pageimages|info');
  url.searchParams.set('inprop', 'url');
  url.searchParams.set('pithumbsize', '1800');
  url.searchParams.set('format', 'json');
  url.searchParams.set('formatversion', '2');
  url.searchParams.set('origin', '*');

  const res = await fetch(url.toString(), {
    cache: 'no-store',
    headers: {
      'accept': 'application/json',
      'user-agent': userAgent()
    }
  });

  if (!res.ok) return { title: query, lang };

  const data = await res.json() as any;
  const page = data?.query?.pages?.[0];
  if (!page) return { title: query, lang };

  return {
    title: page.title || query,
    imageUrl: page.thumbnail?.source,
    pageUrl: page.fullurl,
    lang
  };
}

export async function downloadImage(url?: string): Promise<Buffer | undefined> {
  if (!url) return undefined;
  const res = await fetch(url, {
    cache: 'no-store',
    headers: {
      'accept': 'image/avif,image/webp,image/jpeg,image/png,*/*',
      'user-agent': userAgent()
    }
  });
  if (!res.ok) return undefined;
  const contentType = res.headers.get('content-type') || '';
  if (!contentType.startsWith('image/')) return undefined;
  const arr = await res.arrayBuffer();
  return Buffer.from(arr);
}
