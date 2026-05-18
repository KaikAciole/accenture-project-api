import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider } from '@mui/material';
import { buildTheme } from '../theme/theme';
import AssistantChat from './AssistantChat';
import type { AssistantStreamEvent } from '../api/assistant';
import { ApiError } from '../api/client';

const useAuthMock = vi.fn();
const notifyMock = vi.fn();
const askAssistantStreamMock = vi.fn();

vi.mock('../contexts/AuthContext', () => ({ useAuth: () => useAuthMock() }));
vi.mock('../contexts/SnackbarContext', () => ({ useSnackbar: () => ({ notify: notifyMock }) }));
vi.mock('../api/assistant', () => ({
  askAssistantStream: (...args: unknown[]) => askAssistantStreamMock(...args),
}));

function renderChat() {
  return render(
    <ThemeProvider theme={buildTheme('light')}>
      <AssistantChat />
    </ThemeProvider>,
  );
}

async function openAndType(text: string) {
  const user = userEvent.setup();
  renderChat();
  await user.click(screen.getByRole('button', { name: /abrir assistente/i }));
  await user.type(screen.getByPlaceholderText('Pergunte algo…'), text);
  return user;
}

describe('AssistantChat', () => {
  beforeEach(() => {
    useAuthMock.mockReset();
    notifyMock.mockReset();
    askAssistantStreamMock.mockReset();
    useAuthMock.mockReturnValue({ user: { name: 'João' }, isAuthenticated: true, isAdmin: false });
  });

  it('não renderiza para não autenticado', () => {
    useAuthMock.mockReturnValue({ user: null, isAuthenticated: false, isAdmin: false });
    const { container } = renderChat();
    expect(container).toBeEmptyDOMElement();
  });

  it('não renderiza para admin', () => {
    useAuthMock.mockReturnValue({ user: { name: 'A' }, isAuthenticated: true, isAdmin: true });
    const { container } = renderChat();
    expect(container).toBeEmptyDOMElement();
  });

  it('abre o drawer e mostra mensagem inicial', async () => {
    const user = userEvent.setup();
    renderChat();
    await user.click(screen.getByRole('button', { name: /abrir assistente/i }));
    expect(await screen.findByText(/Olá! Pode me perguntar/)).toBeInTheDocument();
    expect(screen.getByText('AcceBot')).toBeInTheDocument();
  });

  it('botão fechar fecha o drawer', async () => {
    const user = userEvent.setup();
    renderChat();
    await user.click(screen.getByRole('button', { name: /abrir assistente/i }));
    expect(screen.getByText('AcceBot')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /fechar/i }));
    await waitFor(() => expect(screen.queryByText('AcceBot')).not.toBeInTheDocument());
  });

  it('botão enviar desabilitado quando input vazio', async () => {
    const user = userEvent.setup();
    renderChat();
    await user.click(screen.getByRole('button', { name: /abrir assistente/i }));
    expect(screen.getByRole('button', { name: /enviar/i })).toBeDisabled();
  });

  it('envia pergunta e drena chunks até resposta final', async () => {
    askAssistantStreamMock.mockImplementation(async (_q: string, onEvent: (e: AssistantStreamEvent) => void) => {
      onEvent({ type: 'chunk', content: 'olá ' });
      onEvent({ type: 'chunk', content: 'mundo' });
      onEvent({ type: 'done' });
    });

    const user = await openAndType('oi');
    await user.click(screen.getByRole('button', { name: /enviar/i }));

    expect(askAssistantStreamMock).toHaveBeenCalledWith('oi', expect.any(Function));
    await screen.findByText('olá mundo', undefined, { timeout: 3000 });
  });

  it('Enter envia sem shift', async () => {
    askAssistantStreamMock.mockImplementation(async (_q: string, onEvent: (e: AssistantStreamEvent) => void) => {
      onEvent({ type: 'chunk', content: 'ok' });
      onEvent({ type: 'done' });
    });

    const user = await openAndType('oi{Enter}');
    expect(askAssistantStreamMock).toHaveBeenCalledWith('oi', expect.any(Function));
    await screen.findByText('ok', undefined, { timeout: 3000 });
    void user;
  });

  it('rate limit no evento error notifica warning', async () => {
    askAssistantStreamMock.mockImplementation(async (_q: string, onEvent: (e: AssistantStreamEvent) => void) => {
      onEvent({ type: 'error', title: 'Rate limit exceeded', retryAfterSeconds: 30 });
    });

    const user = await openAndType('q');
    await user.click(screen.getByRole('button', { name: /enviar/i }));

    await waitFor(() => expect(notifyMock).toHaveBeenCalledWith(expect.stringContaining('Limite de uso'), 'warning'));
  });

  it('rate limit sem retry usa fallback genérico', async () => {
    askAssistantStreamMock.mockImplementation(async (_q: string, onEvent: (e: AssistantStreamEvent) => void) => {
      onEvent({ type: 'error', title: 'Rate limit exceeded' });
    });
    const user = await openAndType('q');
    await user.click(screen.getByRole('button', { name: /enviar/i }));
    await waitFor(() => expect(notifyMock).toHaveBeenCalledWith(
      'Você atingiu o limite de uso, tente novamente em alguns minutos.', 'warning',
    ));
  });

  it('evento error genérico notifica error com detail', async () => {
    askAssistantStreamMock.mockImplementation(async (_q: string, onEvent: (e: AssistantStreamEvent) => void) => {
      onEvent({ type: 'error', detail: 'detalhe x' });
    });
    const user = await openAndType('q');
    await user.click(screen.getByRole('button', { name: /enviar/i }));
    await waitFor(() => expect(notifyMock).toHaveBeenCalledWith('detalhe x', 'error'));
  });

  it('exceção 429 do askAssistantStream notifica warning', async () => {
    askAssistantStreamMock.mockRejectedValue(new ApiError('too many', 429, null));
    const user = await openAndType('q');
    await user.click(screen.getByRole('button', { name: /enviar/i }));
    await waitFor(() => expect(notifyMock).toHaveBeenCalledWith(expect.stringContaining('limite de uso'), 'warning'));
  });

  it('exceção genérica mostra fallback', async () => {
    askAssistantStreamMock.mockRejectedValue(new Error('boom'));
    const user = await openAndType('q');
    await user.click(screen.getByRole('button', { name: /enviar/i }));
    await waitFor(() => expect(notifyMock).toHaveBeenCalledWith('boom', 'error'));
    expect(await screen.findByText(/Desculpe, não consegui responder agora/)).toBeInTheDocument();
  });

  it('quando não recebe nenhum chunk mostra "não recebi resposta"', async () => {
    askAssistantStreamMock.mockImplementation(async (_q: string, onEvent: (e: AssistantStreamEvent) => void) => {
      onEvent({ type: 'done' });
    });
    const user = await openAndType('q');
    await user.click(screen.getByRole('button', { name: /enviar/i }));
    expect(await screen.findByText(/Desculpe, não recebi resposta/)).toBeInTheDocument();
  });
});
