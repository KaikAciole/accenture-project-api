import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, Link as RouterLink, useLocation, type Location } from 'react-router-dom';
import {
  Box, Paper, Typography, TextField, Button, Stack, Alert, InputAdornment, IconButton,
  Divider, Link as MuiLink, CircularProgress,
} from '@mui/material';
import EmailIcon from '@mui/icons-material/Email';
import LockIcon from '@mui/icons-material/Lock';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import LoginIcon from '@mui/icons-material/Login';
import { login as loginApi } from '../api/auth';
import { ApiError } from '../api/client';
import { useAuth } from '../contexts/AuthContext';

interface LoginLocationState {
  from?: Location;
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function Login() {
  const { login, isAuthenticated, isAdmin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as LoginLocationState | null)?.from?.pathname || '/';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPwd, setShowPwd] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  useEffect(() => {
    if (!submitted || !isAuthenticated) return;
    const fromIsAdmin = from.startsWith('/admin');
    const target = isAdmin ? '/admin' : (fromIsAdmin ? '/' : from);
    navigate(target, { replace: true });
  }, [submitted, isAuthenticated, isAdmin, from, navigate]);

  const validate = (): string | null => {
    if (!email.trim()) return 'Informe seu e-mail';
    if (!EMAIL_RE.test(email)) return 'E-mail inválido';
    if (!password) return 'Informe sua senha';
    if (password.length < 8) return 'Senha precisa ter pelo menos 8 caracteres';
    return null;
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const v = validate();
    if (v) return setError(v);
    setError(null); setLoading(true);
    try {
      const res = await loginApi(email, password);
      const token = res.token || res.accessToken;
      if (!token) throw new Error('Token não retornado pelo servidor');
      login(token, { email, customerId: res.customerId, name: res.name, roles: res.roles });
      setSubmitted(true);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setError('E-mail ou senha inválidos.');
      } else {
        setError(err instanceof Error ? err.message : 'Falha ao entrar');
      }
    } finally {
      setLoading(false);
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
      <Paper
        elevation={10}
        sx={{
          p: { xs: 3, sm: 5 },
          width: '100%',
          maxWidth: 440,
          borderRadius: 3,
          backdropFilter: 'blur(8px)',
        }}
      >
        <Stack alignItems="center" spacing={1} sx={{ mb: 3 }}>
          <Box
            component="img"
            src="/logo.svg"
            alt="acce store"
            sx={{ height: 56, mb: 1 }}
            onError={(e) => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
          />
          <Typography variant="h4" sx={{ fontWeight: 700, letterSpacing: 0.5 }}>
            Bem-vindo de volta
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Entre na sua conta para continuar comprando
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
            <TextField
              label="Senha"
              type={showPwd ? 'text' : 'password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              fullWidth
              autoComplete="current-password"
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <LockIcon color="action" />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={() => setShowPwd((s) => !s)} edge="end" aria-label="alternar senha">
                      {showPwd ? <VisibilityOffIcon /> : <VisibilityIcon />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />
            <Button
              type="submit"
              variant="contained"
              size="large"
              disabled={loading}
              startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <LoginIcon />}
              sx={{ py: 1.25, fontWeight: 600 }}
            >
              {loading ? 'Entrando…' : 'Entrar'}
            </Button>

            <Typography variant="body2" align="center">
              <MuiLink component={RouterLink} to="/forgot-password" underline="hover">
                Esqueci minha senha
              </MuiLink>
            </Typography>

            <Divider sx={{ my: 1 }}>ou</Divider>

            <Typography variant="body2" align="center" color="text.secondary">
              Não tem uma conta?{' '}
              <MuiLink component={RouterLink} to="/register" fontWeight={600}>
                Crie sua conta
              </MuiLink>
            </Typography>
            <Typography variant="caption" align="center" color="text.secondary">
              <MuiLink component={RouterLink} to="/" underline="hover">
                Voltar para a loja
              </MuiLink>
            </Typography>
          </Stack>
        </Box>
      </Paper>
    </Box>
  );
}
