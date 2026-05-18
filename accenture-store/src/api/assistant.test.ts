import { describe, it, expect, beforeEach, vi, type Mock } from 'vitest';
import { askAssistantStream, type AssistantStreamEvent } from './assistant';
import { ApiError } from './client';
import { setCookie } from '../lib/cookies';

function createReadableStream(chunks: string[]): ReadableStream<Uint8Array> {
  const encoder = new TextEncoder();
  let i = 0;
  return new ReadableStream<Uint8Array>({
    pull(controller) {
      if (i >= chunks.length) {
        controller.close();
        return;
      }
      controller.enqueue(encoder.encode(chunks[i++]));
    },
  });
}

function streamResponse(chunks: string[], init: { status?: number; ok?: boolean } = {}) {
  const status = init.status ?? 200;
  const ok = init.ok ?? (status >= 200 && status < 300);
  return {
    ok,
    status,
    body: createReadableStream(chunks),
    json: vi.fn(),
  } as unknown as Response;
}

describe('api/assistant - askAssistantStream', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn());
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { pathname: '/', href: '/' },
    });
  });

  it('envia Authorization quando há token e Accept text/event-stream', async () => {
    setCookie('access_token', 'jwt');
    (fetch as unknown as Mock).mockResolvedValueOnce(streamResponse([]));
    await askAssistantStream('oi', () => {});
    const [, init] = (fetch as unknown as Mock).mock.calls[0];
    expect(init.headers.Authorization).toBe('Bearer jwt');
    expect(init.headers.Accept).toBe('text/event-stream');
    expect(init.body).toBe(JSON.stringify({ question: 'oi' }));
  });

  it('processa eventos chunk + done corretamente', async () => {
    (fetch as unknown as Mock).mockResolvedValueOnce(streamResponse([
      'event: chunk\ndata: {"content":"Olá "}\n\n',
      'event: chunk\ndata: {"content":"mundo"}\n\nevent: done\ndata: {}\n\n',
    ]));

    const events: AssistantStreamEvent[] = [];
    await askAssistantStream('q', (e) => events.push(e));

    expect(events).toEqual([
      { type: 'chunk', content: 'Olá ' },
      { type: 'chunk', content: 'mundo' },
      { type: 'done' },
    ]);
  });

  it('processa evento error com title/detail/retryAfterSeconds', async () => {
    (fetch as unknown as Mock).mockResolvedValueOnce(streamResponse([
      'event: error\ndata: {"title":"X","detail":"D","retryAfterSeconds":30}\n\n',
    ]));
    const events: AssistantStreamEvent[] = [];
    await askAssistantStream('q', (e) => events.push(e));
    expect(events[0]).toEqual({ type: 'error', title: 'X', detail: 'D', retryAfterSeconds: 30 });
  });

  it('chunk sem content vira string vazia', async () => {
    (fetch as unknown as Mock).mockResolvedValueOnce(streamResponse([
      'event: chunk\ndata: {}\n\n',
    ]));
    const events: AssistantStreamEvent[] = [];
    await askAssistantStream('q', (e) => events.push(e));
    expect(events[0]).toEqual({ type: 'chunk', content: '' });
  });

  it('ignora blocos com JSON inválido e eventos desconhecidos', async () => {
    (fetch as unknown as Mock).mockResolvedValueOnce(streamResponse([
      'event: chunk\ndata: {invalid\n\n',
      'event: outro\ndata: {"x":1}\n\n',
      'event: chunk\ndata: {"content":"ok"}\n\n',
    ]));
    const events: AssistantStreamEvent[] = [];
    await askAssistantStream('q', (e) => events.push(e));
    expect(events).toEqual([{ type: 'chunk', content: 'ok' }]);
  });

  it('processa último bloco sem \\n\\n final', async () => {
    (fetch as unknown as Mock).mockResolvedValueOnce(streamResponse([
      'event: chunk\ndata: {"content":"final"}',
    ]));
    const events: AssistantStreamEvent[] = [];
    await askAssistantStream('q', (e) => events.push(e));
    expect(events).toEqual([{ type: 'chunk', content: 'final' }]);
  });

  it('lança ApiError quando 401 e redireciona', async () => {
    setCookie('access_token', 'jwt');
    localStorage.setItem('user', JSON.stringify({}));
    (fetch as unknown as Mock).mockResolvedValueOnce({
      ok: false, status: 401, body: null, json: vi.fn().mockResolvedValue({}),
    } as unknown as Response);

    await expect(askAssistantStream('q', () => {})).rejects.toBeInstanceOf(ApiError);
    expect(window.location.href).toBe('/login');
    expect(localStorage.getItem('user')).toBeNull();
  });

  it('lança ApiError quando status diferente de 2xx', async () => {
    (fetch as unknown as Mock).mockResolvedValueOnce({
      ok: false, status: 500, body: null,
      json: vi.fn().mockResolvedValue({ message: 'erro server' }),
    } as unknown as Response);

    await expect(askAssistantStream('q', () => {})).rejects.toMatchObject({
      status: 500, message: 'erro server',
    });
  });

  it('lança ApiError quando resposta não tem body', async () => {
    (fetch as unknown as Mock).mockResolvedValueOnce({
      ok: true, status: 200, body: null,
    } as unknown as Response);

    await expect(askAssistantStream('q', () => {})).rejects.toMatchObject({
      status: 0, message: 'Resposta sem corpo do assistente',
    });
  });

  it('encapsula falha de rede como ApiError', async () => {
    (fetch as unknown as Mock).mockRejectedValueOnce(new Error('net'));
    await expect(askAssistantStream('q', () => {})).rejects.toMatchObject({
      status: 0, message: 'Falha de rede ao conectar ao assistente',
    });
  });
});
