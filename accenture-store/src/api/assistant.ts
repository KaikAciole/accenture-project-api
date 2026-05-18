import { ApiError, BASE_URL } from './client';
import { deleteCookie, getCookie } from '../lib/cookies';

export type AssistantStreamEvent =
  | { type: 'chunk'; content: string }
  | { type: 'done' }
  | { type: 'error'; title?: string; detail?: string; retryAfterSeconds?: number };

export async function askAssistantStream(
  question: string,
  onEvent: (event: AssistantStreamEvent) => void,
  signal?: AbortSignal,
): Promise<void> {
  const token = getCookie('access_token');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
  };
  if (token) headers.Authorization = `Bearer ${token}`;

  let res: Response;
  try {
    res = await fetch(`${BASE_URL}/api/v1/assistant/ask`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ question }),
      signal,
    });
  } catch {
    throw new ApiError('Falha de rede ao conectar ao assistente', 0, null);
  }

  if (res.status === 401) {
    deleteCookie('access_token');
    localStorage.removeItem('user');
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
    throw new ApiError('Sessão expirada', 401, null);
  }

  if (!res.ok) {
    const errorBody = await res.json().catch(() => ({}));
    throw new ApiError(errorBody.message || `HTTP ${res.status}`, res.status, errorBody);
  }

  if (!res.body) {
    throw new ApiError('Resposta sem corpo do assistente', 0, null);
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });

    let blockEnd: number;
    while ((blockEnd = buffer.indexOf('\n\n')) !== -1) {
      const block = buffer.slice(0, blockEnd);
      buffer = buffer.slice(blockEnd + 2);
      const event = parseSseBlock(block);
      if (event) onEvent(event);
    }
  }

  if (buffer.trim()) {
    const event = parseSseBlock(buffer);
    if (event) onEvent(event);
  }
}

function parseSseBlock(block: string): AssistantStreamEvent | null {
  let eventName = 'message';
  const dataLines: string[] = [];

  for (const rawLine of block.split('\n')) {
    const line = rawLine.replace(/\r$/, '');
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart());
    }
  }

  if (dataLines.length === 0) return null;
  const data = dataLines.join('\n');

  let payload: unknown;
  try {
    payload = JSON.parse(data);
  } catch {
    return null;
  }

  const obj = (payload || {}) as Record<string, unknown>;

  if (eventName === 'chunk') {
    return { type: 'chunk', content: typeof obj.content === 'string' ? obj.content : '' };
  }
  if (eventName === 'done') {
    return { type: 'done' };
  }
  if (eventName === 'error') {
    return {
      type: 'error',
      title: typeof obj.title === 'string' ? obj.title : undefined,
      detail: typeof obj.detail === 'string' ? obj.detail : undefined,
      retryAfterSeconds: typeof obj.retryAfterSeconds === 'number' ? obj.retryAfterSeconds : undefined,
    };
  }

  return null;
}
