import { useState, type FormEvent } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import {
  Box, Paper, Typography, TextField, Button, Stack, Alert, InputAdornment, Link as MuiLink,
  CircularProgress,
} from '@mui/material';
import EmailIcon from '@mui/icons-material/Email';
import SendIcon from '@mui/icons-material/Send';
import { forgotPassword } from '../api/auth';
import { useSnackbar } from '../contexts/SnackbarContext';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function ForgotPassword() {
  const { notify } = useSnackbar();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!EMAIL_RE.test(email)) { setError('E-mail inválido'); return; }
    setError(null); setLoading(true);
    try {
      await forgotPassword(email.trim());
    } catch {
      /* resposta é sempre genérica por segurança */
    } finally {
      setLoading(false);
      notify('Se o email existir, você receberá um link no seu email.', 'info');
      navigate('/login');
    }
  };

  return (
    <Box
      sx={{
        flex: 1,
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        px: 2,
        py: 6,
        background: (t) =>
          t.palette.mode === 'dark'
            ? 'radial-gradient(circle at 20% 20%, #2a0a3f 0%, #0d0218 60%, #000 100%)'
            : 'radial-gradient(circle at 20% 20%, #f3e8ff 0%, #ffffff 60%, #fafafa 100%)',
      }}
    >
      <Paper elevation={10} sx={{ p: { xs: 3, sm: 5 }, width: '100%', maxWidth: 440, borderRadius: 3 }}>
        <Stack alignItems="center" spacing={1} sx={{ mb: 3 }}>
          <Box
            component="img"
            src="/logo.svg"
            alt="acce store"
            sx={{ height: 56, mb: 1 }}
            onError={(e) => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
          />
          <Typography variant="h5" sx={{ fontWeight: 700 }}>Recuperar senha</Typography>
          <Typography variant="body2" color="text.secondary" align="center">
            Informe o e-mail da sua conta e enviaremos um link para redefinir sua senha.
          </Typography>
        </Stack>

        <Box component="form" onSubmit={submit} noValidate>
          <Stack spacing={2.5}>
            {error && <Alert severity="error" variant="outlined">{error}</Alert>}
            <TextField
              label="E-mail"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              fullWidth
              autoComplete="email"
              autoFocus
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <EmailIcon color="action" />
                  </InputAdornment>
                ),
              }}
            />
            <Button
              type="submit"
              variant="contained"
              size="large"
              disabled={loading}
              startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <SendIcon />}
              sx={{ py: 1.25, fontWeight: 600 }}
            >
              {loading ? 'Enviando…' : 'Enviar link'}
            </Button>
            <Typography variant="body2" align="center">
              <MuiLink component={RouterLink} to="/login" underline="hover">
                Voltar ao login
              </MuiLink>
            </Typography>
          </Stack>
        </Box>
      </Paper>
    </Box>
  );
}
