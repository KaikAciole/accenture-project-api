import { useState, type FormEvent } from 'react';
import { Link as RouterLink, useNavigate, useSearchParams } from 'react-router-dom';
import {
  Box, Paper, Typography, TextField, Button, Stack, Alert, InputAdornment, IconButton,
  Link as MuiLink, CircularProgress,
} from '@mui/material';
import LockIcon from '@mui/icons-material/Lock';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import SaveIcon from '@mui/icons-material/Save';
import { resetPassword } from '../api/auth';
import { useSnackbar } from '../contexts/SnackbarContext';
import { ApiError } from '../api/client';

export default function ResetPassword() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { notify } = useSnackbar();
  const email = params.get('email') || '';
  const token = params.get('token') || '';

  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const linkInvalid = !email || !token;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (password.length < 8) { setError('Senha precisa ter pelo menos 8 caracteres'); return; }
    if (password !== confirm) { setError('Senhas não conferem'); return; }
    setError(null); setLoading(true);
    try {
      await resetPassword(email, token, password);
      notify('Senha redefinida com sucesso! Faça login.', 'success');
      navigate('/login');
    } catch (err) {
      if (err instanceof ApiError && err.status === 422) {
        setError('Token inválido ou expirado, solicite um novo.');
      } else {
        setError(err instanceof Error ? err.message : 'Falha ao redefinir senha');
      }
    } finally { setLoading(false); }
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
            ? 'radial-gradient(circle at 80% 20%, #2a0a3f 0%, #0d0218 60%, #000 100%)'
            : 'radial-gradient(circle at 80% 20%, #f3e8ff 0%, #ffffff 60%, #fafafa 100%)',
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
          <Typography variant="h5" sx={{ fontWeight: 700 }}>Nova senha</Typography>
          <Typography variant="body2" color="text.secondary" align="center">
            Defina uma nova senha para sua conta.
          </Typography>
        </Stack>

        {linkInvalid ? (
          <Alert severity="error">
            Link inválido ou incompleto.{' '}
            <MuiLink component={RouterLink} to="/forgot-password">Solicite um novo link</MuiLink>.
          </Alert>
        ) : (
          <Box component="form" onSubmit={submit} noValidate>
            <Stack spacing={2.5}>
              {error && <Alert severity="error" variant="outlined">{error}</Alert>}
              <TextField
                label="Nova senha"
                type={showPwd ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                fullWidth
                autoComplete="new-password"
                autoFocus
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
              <TextField
                label="Confirmar senha"
                type={showPwd ? 'text' : 'password'}
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                required
                fullWidth
                autoComplete="new-password"
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <LockIcon color="action" />
                    </InputAdornment>
                  ),
                }}
              />
              <Button
                type="submit"
                variant="contained"
                size="large"
                disabled={loading}
                startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <SaveIcon />}
                sx={{ py: 1.25, fontWeight: 600 }}
              >
                {loading ? 'Salvando…' : 'Redefinir senha'}
              </Button>
              <Typography variant="body2" align="center">
                <MuiLink component={RouterLink} to="/login" underline="hover">
                  Voltar ao login
                </MuiLink>
              </Typography>
            </Stack>
          </Box>
        )}
      </Paper>
    </Box>
  );
}
