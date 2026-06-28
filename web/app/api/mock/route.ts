import { jsonResponse } from '@/lib/http';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function GET(request: Request) {
  const url = new URL(request.url);
  const selection = url.searchParams.get('selection') || url.searchParams.get('value') || 'Tom Cruise';
  return jsonResponse({
    selection,
    updatedAt: new Date().toISOString(),
    nested: {
      result: {
        name: selection
      }
    }
  });
}
