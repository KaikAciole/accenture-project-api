import { useState, type ChangeEvent, type FormEvent } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import {
  Box, Paper, Typography, TextField, Button, Stack, Alert, InputAdornment, IconButton,
  Divider, Link as MuiLink, CircularProgress, LinearProgress,
} from '@mui/material';
import EmailIcon from '@mui/icons-material/Email';
import LockIcon from '@mui/icons-material/Lock';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import PersonIcon from '@mui/icons-material/Person';
import BadgeIcon from '@mui/icons-material/Badge';
import PhoneIcon from '@mui/icons-material/Phone';
import { register as registerApi } from '../api/auth';
import { useSnackbar } from '../contexts/SnackbarContext';

interface RegisterForm {
  name: string;
  cpf: string;
  phone: string;
  email: string;
  password: string;
  confirm: string;
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const onlyDigits = (s: string) => s.replace(/\D/g, '');
const formatCpf = (s: string) => {
  const d = onlyDigits(s).slice(0, 11);
  return d
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
};
const formatPhone = (s: string) => {
  const d = onlyDigits(s).slice(0, 11);
  if (d.length <= 10) return d.replace(/(\d{2})(\d{4})(\d{0,4})/, '($1) $2-$3').trim().replace(/-$/, '');
  return d.replace(/(\d{2})(\d{5})(\d{0,4})/, '($1) $2-$3').trim().replace(/-$/, '');
};

interface FieldErrors {
  name?: string;
  cpf?: string;
  phone?: string;
  email?: string;
  password?: string;
  confirm?: string;
}

function validateFields(form: RegisterForm): FieldErrors {
  const errs: FieldErrors = {};
  const name = form.name.trim();
  if (!name) errs.name = 'Informe seu nome';
  else if (name.length < 2) errs.name = 'Nome muito curto';

  const cpfDigits = onlyDigits(form.cpf);
  if (!cpfDigits) errs.cpf = 'Informe seu CPF';
  else if (cpfDigits.length !== 11) errs.cpf = 'CPF deve ter 11 dígitos';

  const phoneDigits = onlyDigits(form.phone);
  if (!phoneDigits) errs.phone = 'Informe seu telefone';
  else if (phoneDigits.length < 10) errs.phone = 'Telefone deve ter pelo menos 10 dígitos';
  else if (phoneDigits.length > 11) errs.phone = 'Telefone inválido';

  if (!form.email.trim()) errs.email = 'Informe seu e-mail';
  else if (!EMAIL_RE.test(form.email)) errs.email = 'E-mail inválido';

  if (!form.password) errs.password = 'Informe uma senha';
  else if (form.password.length < 8) errs.password = 'Senha precisa ter pelo menos 8 caracteres';

  if (!form.confirm) errs.confirm = 'Confirme sua senha';
  else if (form.confirm !== form.password) errs.confirm = 'Senhas não conferem';

  return errs;
}

function scorePassword(pwd: string): { score: number; label: string; color: 'error' | 'warning' | 'info' | 'success' } {
  let score = 0;
  if (pwd.length >= 8) score++;
  if (pwd.length >= 12) score++;
  if (/[A-Z]/.test(pwd)) score++;
  if (/[0-9]/.test(pwd)) score++;
  if (/[^A-Za-z0-9]/.test(pwd)) score++;
  if (score <= 1) return { score: 20, label: 'Fraca', color: 'error' };
  if (score === 2) return { score: 45, label: 'Razoável', color: 'warning' };
  if (score === 3) return { score: 70, label: 'Boa', color: 'info' };
  return { score: 100, label: 'Forte', color: 'success' };
}

export default function Register() {
  const navigate = useNavigate();
  const { notify } = useSnackbar();
  const [form, setForm] = useState<RegisterForm>({ name: '', cpf: '', phone: '', email: '', password: '', confirm: '' });
  const [touched, setTouched] = useState<Record<keyof RegisterForm, boolean>>({
    name: false, cpf: false, phone: false, email: false, password: false, confirm: false,
  });
  const [showPwd, setShowPwd] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const set = (k: keyof RegisterForm) => (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    let v = e.target.value;
    if (k === 'cpf') v = formatCpf(v);
    else if (k === 'phone') v = formatPhone(v);
    setForm({ ...form, [k]: v });
  };

  const markTouched = (k: keyof RegisterForm) => () =>
    setTouched((t) => ({ ...t, [k]: true }));

  const fieldErrors = validateFields(form);
  const isValid = Object.keys(fieldErrors).length === 0;
  const show = (k: keyof RegisterForm) => (touched[k] ? fieldErrors[k] : undefined);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setTouched({ name: true, cpf: true, phone: true, email: true, password: true, confirm: true });
    if (!isValid) {
      setError('Verifique os campos destacados antes de continuar.');
      return;
    }
    setError(null); setLoading(true);
    try {
      await registerApi({
        email: form.email,
        password: form.password,
        name: form.name.trim(),
        cpf: onlyDigits(form.cpf),
        phone: onlyDigits(form.phone),
      });
      notify('Conta criada! Faça login.', 'success');
      navigate('/login');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao registrar');
    } finally { setLoading(false); }
  };

  const strength = form.password ? scorePassword(form.password) : null;

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
      <Paper
        elevation={10}
        sx={{
          p: { xs: 3, sm: 5 },
          width: '100%',
          maxWidth: 460,
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
            Crie sua conta
          </Typography>
          <Typography variant="body2" color="text.secondary" align="center">
            Leva menos de 1 minuto para começar a comprar
          </Typography>
        </Stack>

        <Box component="form" onSubmit={submit} noValidate>
          <Stack spacing={2.5}>
            {error && <Alert severity="error" variant="outlined">{error}</Alert>}

            <TextField
              label="Nome completo"
              value={form.name}
              onChange={set('name')}
              onBlur={markTouched('name')}
              required
              fullWidth
              autoComplete="name"
              autoFocus
              error={!!show('name')}
              helperText={show('name') || ' '}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <PersonIcon color="action" />
                  </InputAdornment>
                ),
              }}
            />

            <TextField
              label="CPF"
              value={form.cpf}
              onChange={set('cpf')}
              onBlur={markTouched('cpf')}
              required
              fullWidth
              autoComplete="off"
              inputProps={{ inputMode: 'numeric', maxLength: 14 }}
              placeholder="000.000.000-00"
              error={!!show('cpf')}
              helperText={show('cpf') || ' '}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <BadgeIcon color="action" />
                  </InputAdornment>
                ),
              }}
            />

            <TextField
              label="Telefone"
              value={form.phone}
              onChange={set('phone')}
              onBlur={markTouched('phone')}
              required
              fullWidth
              autoComplete="tel"
              inputProps={{ inputMode: 'tel', maxLength: 15 }}
              placeholder="(00) 00000-0000"
              error={!!show('phone')}
              helperText={show('phone') || ' '}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <PhoneIcon color="action" />
                  </InputAdornment>
                ),
              }}
            />

            <TextField
              label="E-mail"
              type="email"
              value={form.email}
              onChange={set('email')}
              onBlur={markTouched('email')}
              required
              fullWidth
              autoComplete="email"
              error={!!show('email')}
              helperText={show('email') || ' '}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <EmailIcon color="action" />
                  </InputAdornment>
                ),
              }}
            />

            <Box>
              <TextField
                label="Senha"
                type={showPwd ? 'text' : 'password'}
                value={form.password}
                onChange={set('password')}
                onBlur={markTouched('password')}
                required
                fullWidth
                autoComplete="new-password"
                error={!!show('password')}
                helperText={show('password') || 'Mínimo de 8 caracteres'}
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
              {strength && (
                <Box sx={{ mt: 1 }}>
                  <LinearProgress
                    variant="determinate"
                    value={strength.score}
                    color={strength.color}
                    sx={{ height: 6, borderRadius: 3 }}
                  />
                  <Typography variant="caption" color={`${strength.color}.main`}>
                    Força da senha: {strength.label}
                  </Typography>
                </Box>
              )}
            </Box>

            <TextField
              label="Confirmar senha"
              type={showConfirm ? 'text' : 'password'}
              value={form.confirm}
              onChange={set('confirm')}
              onBlur={markTouched('confirm')}
              required
              fullWidth
              autoComplete="new-password"
              error={!!show('confirm')}
              helperText={show('confirm') || ' '}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <LockIcon color="action" />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={() => setShowConfirm((s) => !s)} edge="end" aria-label="alternar confirmação">
                      {showConfirm ? <VisibilityOffIcon /> : <VisibilityIcon />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />

            <Button
              type="submit"
              variant="contained"
              size="large"
              disabled={loading || !isValid}
              startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <PersonAddIcon />}
              sx={{ py: 1.25, fontWeight: 600 }}
            >
              {loading ? 'Criando…' : 'Criar conta'}
            </Button>

            <Divider sx={{ my: 1 }}>ou</Divider>

            <Typography variant="body2" align="center" color="text.secondary">
              Já tem uma conta?{' '}
              <MuiLink component={RouterLink} to="/login" fontWeight={600}>
                Entrar
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
