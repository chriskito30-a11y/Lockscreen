export function jsonResponse(data: unknown, init?: ResponseInit): Response {
  return new Response(JSON.stringify(data, null, 2), {
    ...init,
    headers: {
      'content-type': 'application/json; charset=utf-8',
      'cache-control': 'no-store, max-age=0',
      ...(init?.headers ?? {})
    }
  });
}

export function textResponse(data: string, init?: ResponseInit): Response {
  return new Response(data, {
    ...init,
    headers: {
      'content-type': 'text/plain; charset=utf-8',
      'cache-control': 'no-store, max-age=0',
      ...(init?.headers ?? {})
    }
  });
}

export function pngResponse(buffer: Buffer, init?: ResponseInit): Response {
  const body = buffer.buffer.slice(
    buffer.byteOffset,
    buffer.byteOffset + buffer.byteLength
  ) as ArrayBuffer;

  return new Response(body, {
    ...init,
    headers: {
      'content-type': 'image/png',
      'cache-control': 'no-store, max-age=0',
      ...(init?.headers ?? {})
    }
  });
}

export function badRequest(message: string, status = 400): Response {
  return jsonResponse({ ok: false, error: message }, { status });
}
