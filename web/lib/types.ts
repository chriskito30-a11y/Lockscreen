export type MagicConfig = {
  sourceUrl: string;
  valuePath?: string;
  wikiLang: string;
  fallbackLang: string;
  width: number;
  height: number;
  template: 'wiki-photo' | 'text-only';
  createdAt: number;
};

export type SourceScanResult = {
  ok: boolean;
  contentType: 'json' | 'text';
  paths: Array<{ path: string; preview: string }>;
  rawPreview: string;
  error?: string;
};

export type ExtractedValue = {
  value: string;
  sourceKind: 'json-path' | 'json-auto' | 'text';
  path?: string;
};

export type WikiImageResult = {
  title: string;
  imageUrl?: string;
  pageUrl?: string;
  lang: string;
};
