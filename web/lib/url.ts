export function assertHttpUrl(input: unknown, label = 'URL'): string {
  if (typeof input !== 'string' || !input.trim()) {
    throw new Error(`${label} manquante`);
  }

  const value = input.trim();
  let url: URL;
  try {
    url = new URL(value);
  } catch {
    throw new Error(`${label} invalide`);
  }

  if (url.protocol !== 'https:' && url.protocol !== 'http:') {
    throw new Error(`${label} doit commencer par http:// ou https://`);
  }

  return url.toString();
}

export function safeInteger(input: unknown, fallback: number, min: number, max: number): number {
  const n = Number(input);
  if (!Number.isFinite(n)) return fallback;
  return Math.max(min, Math.min(max, Math.round(n)));
}
