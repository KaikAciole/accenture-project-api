import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import {
  Fab, Drawer, Box, Stack, Typography, IconButton, TextField, Paper, Avatar,
  keyframes,
} from '@mui/material';
import ChatBubbleOutlineIcon from '@mui/icons-material/ChatBubbleOutline';
import CloseIcon from '@mui/icons-material/Close';
import SendIcon from '@mui/icons-material/Send';
import SmartToyOutlinedIcon from '@mui/icons-material/SmartToyOutlined';
import PersonIcon from '@mui/icons-material/Person';
import { useAuth } from '../contexts/AuthContext';
import { useSnackbar } from '../contexts/SnackbarContext';
import { askAssistantStream } from '../api/assistant';
import { ApiError } from '../api/client';
import type { AssistantMessage } from '../api/types';

const DRAWER_WIDTH = 400;

const blink = keyframes`
  0%, 80%, 100% { opacity: 0.2; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-3px); }
`;

const getInitials = (name?: string): string => {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0][0].toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
};

function TypingDots() {
  return (
    <Stack direction="row" spacing={0.6} sx={{ alignItems: 'center', px: 1, py: 0.5 }}>
      {[0, 1, 2].map((i) => (
        <Box
          key={i}
          sx={{
            width: 6,
            height: 6,
            borderRadius: '50%',
            bgcolor: 'text.secondary',
            animation: `${blink} 1.2s ${i * 0.18}s infinite both`,
          }}
        />
      ))}
    </Stack>
  );
}

export default function AssistantChat() {
  const { user, isAuthenticated, isAdmin } = useAuth();
  const { notify } = useSnackbar();
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<AssistantMessage[]>([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const endRef = useRef<HTMLDivElement>(null);
  const queueRef = useRef('');
  const intervalRef = useRef<number | null>(null);
  const streamDoneRef = useRef(true);
  const initials = getInitials(user?.name);

  const TICK_MS = 20;

  const stopTyping = () => {
    if (intervalRef.current !== null) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  };

  const drainTick = () => {
    if (queueRef.current.length === 0) {
      if (streamDoneRef.current) {
        stopTyping();
        setSending(false);
      }
      return;
    }
    const charsPerTick = Math.max(1, Math.ceil(queueRef.current.length / 50));
    const slice = queueRef.current.slice(0, charsPerTick);
    queueRef.current = queueRef.current.slice(charsPerTick);
    setMessages((prev) => {
      const next = [...prev];
      const last = next[next.length - 1];
      if (last && last.role === 'assistant') {
        next[next.length - 1] = { role: 'assistant', content: last.content + slice };
      }
      return next;
    });
  };

  const ensureTyping = () => {
    if (intervalRef.current !== null) return;
    intervalRef.current = window.setInterval(drainTick, TICK_MS);
  };

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, sending]);

  useEffect(() => {
    if (!isAuthenticated || isAdmin) {
      queueRef.current = '';
      streamDoneRef.current = true;
      stopTyping();
      setMessages([]);
      setInput('');
      setOpen(false);
      setSending(false);
    }
  }, [isAuthenticated, isAdmin, user?.customerId]);

  useEffect(() => () => stopTyping(), []);

  if (!isAuthenticated || isAdmin) return null;

  const send = async (e?: FormEvent) => {
    e?.preventDefault();
    const text = input.trim();
    if (!text || sending) return;
    setInput('');
    setMessages((prev) => [
      ...prev,
      { role: 'user', content: text },
      { role: 'assistant', content: '' },
    ]);
    setSending(true);
    queueRef.current = '';
    streamDoneRef.current = false;
    ensureTyping();

    let receivedAnyChunk = false;
    let errorNotified = false;

    const replaceLastAssistant = (content: string) => {
      queueRef.current = '';
      setMessages((prev) => {
        const next = [...prev];
        const last = next[next.length - 1];
        if (last && last.role === 'assistant') {
          next[next.length - 1] = { role: 'assistant', content };
        }
        return next;
      });
    };

    try {
      await askAssistantStream(text, (event) => {
        if (event.type === 'chunk') {
          receivedAnyChunk = true;
          queueRef.current += event.content;
        } else if (event.type === 'error') {
          errorNotified = true;
          const retry = event.retryAfterSeconds;
          if (event.title === 'Rate limit exceeded' || retry !== undefined) {
            notify(
              retry
                ? `Limite de uso atingido. Tente novamente em ${retry}s.`
                : 'Você atingiu o limite de uso, tente novamente em alguns minutos.',
              'warning',
            );
          } else {
            notify(event.detail || 'Falha ao falar com o assistente', 'error');
          }
          replaceLastAssistant(event.detail || 'Desculpe, não consegui responder agora.');
        }
      });

      if (!receivedAnyChunk && !errorNotified) {
        replaceLastAssistant('Desculpe, não recebi resposta.');
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 429) {
        notify('Você atingiu o limite de uso, tente novamente em alguns minutos.', 'warning');
      } else {
        notify(err instanceof Error ? err.message : 'Falha ao falar com o assistente', 'error');
      }
      replaceLastAssistant('Desculpe, não consegui responder agora.');
    } finally {
      streamDoneRef.current = true;
    }
  };

  const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  };

  return (
    <>
      <Fab
        color="primary"
        aria-label="abrir assistente"
        onClick={() => setOpen(true)}
        sx={{
          position: 'fixed',
          bottom: { xs: 16, sm: 24 },
          right: { xs: 16, sm: 24 },
          zIndex: 1200,
          transition: 'transform 0.2s ease',
          '&:hover': { transform: 'scale(1.06)' },
        }}
      >
        <SmartToyOutlinedIcon />
      </Fab>

      <Drawer
        anchor="right"
        open={open}
        onClose={() => setOpen(false)}
        PaperProps={{ sx: { width: { xs: '100%', sm: DRAWER_WIDTH }, display: 'flex', flexDirection: 'column' } }}
      >
        <Box sx={{
          p: 2, bgcolor: 'primary.main', color: 'primary.contrastText',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <Stack direction="row" spacing={1.5} alignItems="center">
            <Avatar sx={{ bgcolor: 'secondary.main', width: 36, height: 36 }}>
              <SmartToyOutlinedIcon fontSize="small" />
            </Avatar>
            <Box>
              <Typography variant="subtitle1" sx={{ fontWeight: 700, lineHeight: 1.2 }}>
                AcceBot
              </Typography>
              <Typography variant="caption" sx={{ opacity: 0.85 }}>
                online
              </Typography>
            </Box>
          </Stack>
          <IconButton onClick={() => setOpen(false)} sx={{ color: 'inherit' }} aria-label="fechar">
            <CloseIcon />
          </IconButton>
        </Box>

        <Box sx={{ flex: 1, overflowY: 'auto', p: 2, bgcolor: 'background.default' }}>
          {messages.length === 0 ? (
            <Stack spacing={2} alignItems="center" sx={{ mt: 4, textAlign: 'center', px: 2 }}>
              <Avatar sx={{ bgcolor: 'primary.main', width: 56, height: 56 }}>
                <SmartToyOutlinedIcon />
              </Avatar>
              <Typography variant="body2" color="text.secondary">
                Olá! Pode me perguntar sobre produtos, pedidos ou qualquer dúvida da loja.
              </Typography>
            </Stack>
          ) : (
            <Stack spacing={1.5}>
              {messages.map((m, i) => {
                const isUser = m.role === 'user';
                const isEmptyAssistant = !isUser && !m.content && i === messages.length - 1 && sending;
                return (
                  <Stack
                    key={i}
                    direction={isUser ? 'row-reverse' : 'row'}
                    spacing={1}
                    alignItems="flex-end"
                  >
                    <Avatar
                      sx={{
                        width: 28,
                        height: 28,
                        bgcolor: isUser ? 'secondary.main' : 'primary.main',
                        color: 'primary.contrastText',
                        fontSize: '0.75rem',
                        fontWeight: 700,
                      }}
                    >
                      {isUser ? (initials !== '?' ? initials : <PersonIcon fontSize="small" />) : <SmartToyOutlinedIcon fontSize="small" />}
                    </Avatar>
                    <Paper
                      elevation={0}
                      sx={{
                        p: 1.5,
                        maxWidth: '75%',
                        bgcolor: isUser ? 'primary.main' : 'action.hover',
                        color: isUser ? 'primary.contrastText' : 'text.primary',
                        borderRadius: 2,
                        borderTopRightRadius: isUser ? 4 : 16,
                        borderTopLeftRadius: isUser ? 16 : 4,
                        whiteSpace: 'pre-wrap',
                        wordBreak: 'break-word',
                      }}
                    >
                      {isEmptyAssistant ? (
                        <TypingDots />
                      ) : (
                        <Typography variant="body2">{m.content}</Typography>
                      )}
                    </Paper>
                  </Stack>
                );
              })}
            </Stack>
          )}
          <div ref={endRef} />
        </Box>

        <Box
          component="form"
          onSubmit={send}
          sx={{ p: 2, borderTop: 1, borderColor: 'divider', display: 'flex', gap: 1 }}
        >
          <TextField
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKeyDown}
            placeholder="Pergunte algo…"
            fullWidth
            size="small"
            multiline
            maxRows={4}
            disabled={sending}
          />
          <IconButton type="submit" color="primary" disabled={!input.trim() || sending} aria-label="enviar">
            <SendIcon />
          </IconButton>
        </Box>
      </Drawer>
    </>
  );
}
