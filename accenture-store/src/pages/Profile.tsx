import { useEffect, useState, type ChangeEvent } from 'react';
import { Container, Paper, Typography, Stack, TextField, Button, Skeleton, Alert } from '@mui/material';
import { useAuth } from '../contexts/AuthContext';
import { useSnackbar } from '../contexts/SnackbarContext';
import { getCustomer, updateCustomer } from '../api/customers';
import type { Customer } from '../api/types';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const onlyDigits = (s: string) => s.replace(/\D/g, '');
const formatPhone = (s: string) => {
  const d = onlyDigits(s).slice(0, 11);
  if (d.length <= 10) return d.replace(/(\d{2})(\d{4})(\d{0,4})/, '($1) $2-$3').trim().replace(/-$/, '');
  return d.replace(/(\d{2})(\d{5})(\d{0,4})/, '($1) $2-$3').trim().replace(/-$/, '');
};

interface ProfileErrors {
  name?: string;
  email?: string;
  phone?: string;
}

function validateProfile(d: Customer): ProfileErrors {
  const errs: ProfileErrors = {};
  const name = (d.name || '').trim();
  if (!name) errs.name = 'Informe seu nome';
  else if (name.length < 2) errs.name = 'Nome muito curto';

  const email = (d.email || '').trim();
  if (!email) errs.email = 'Informe seu e-mail';
  else if (!EMAIL_RE.test(email)) errs.email = 'E-mail inválido';

  const phoneDigits = onlyDigits(d.phone || '');
  if (!phoneDigits) errs.phone = 'Informe seu telefone';
  else if (phoneDigits.length < 10) errs.phone = 'Telefone deve ter pelo menos 10 dígitos';
  else if (phoneDigits.length > 11) errs.phone = 'Telefone inválido';

  return errs;
}

export default function Profile() {
  const { user } = useAuth();
  const { notify } = useSnackbar();
  const [data, setData] = useState<Customer | null>(null);
  const [originalEmail, setOriginalEmail] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [touched, setTouched] = useState<Record<'name' | 'email' | 'phone', boolean>>({
    name: false, email: false, phone: false,
  });

  const set = (k: keyof Customer) => (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const raw = e.target.value;
    const v = k === 'phone' ? formatPhone(raw) : raw;
    setData((d) => (d ? { ...d, [k]: v } : d));
  };

  const markTouched = (k: 'name' | 'email' | 'phone') => () =>
    setTouched((t) => ({ ...t, [k]: true }));

  useEffect(() => {
    if (!user?.customerId) return;
    getCustomer(user.customerId)
      .then((c) => {
        setData({ ...c, phone: c.phone ? formatPhone(c.phone) : '' });
        setOriginalEmail(c.email || '');
      })
      .catch((e: Error) => setError(e.message));
  }, [user?.customerId]);

  const errs: ProfileErrors = data ? validateProfile(data) : {};
  const isValid = Object.keys(errs).length === 0;
  const show = (k: 'name' | 'email' | 'phone') => (touched[k] ? errs[k] : undefined);

  const save = async () => {
    if (!user?.customerId || !data) return;
    setTouched({ name: true, email: true, phone: true });
    if (!isValid) {
      notify('Verifique os campos destacados antes de salvar.', 'error');
      return;
    }
    const emailChanged = !!data.email && data.email !== originalEmail;
    setSaving(true);
    try {
      await updateCustomer(user.customerId, {
        name: data.name,
        email: data.email,
        phone: onlyDigits(data.phone || ''),
      });
      if (emailChanged) {
        notify(`Email atualizado. Use ${data.email} para entrar na próxima vez.`, 'success');
        setOriginalEmail(data.email || '');
      } else {
        notify('Perfil atualizado', 'success');
      }
    } catch (e) {
      notify(e instanceof Error ? e.message : 'Erro ao salvar', 'error');
    } finally { setSaving(false); }
  };

  if (error) return <Container sx={{ py: 4 }}><Alert severity="error">{error}</Alert></Container>;
  if (!data) return <Container sx={{ py: 4 }}><Skeleton variant="rectangular" height={300} /></Container>;

  return (
    <Container maxWidth="sm" sx={{ py: 3 }}>
      <Paper sx={{ p: 4 }}>
        <Typography variant="h5" gutterBottom>Meu perfil</Typography>
        <Stack spacing={2}>
          <TextField
            label="Nome"
            value={data.name || ''}
            onChange={set('name')}
            onBlur={markTouched('name')}
            fullWidth
            error={!!show('name')}
            helperText={show('name') || ' '}
          />
          <TextField
            label="E-mail"
            type="email"
            value={data.email || ''}
            onChange={set('email')}
            onBlur={markTouched('email')}
            fullWidth
            error={!!show('email')}
            helperText={show('email') || 'Alterar o e-mail muda seu login'}
          />
          <TextField
            label="CPF"
            value={data.cpf || ''}
            fullWidth
            disabled
            helperText="O CPF não pode ser alterado"
          />
          <TextField
            label="Telefone"
            value={data.phone || ''}
            onChange={set('phone')}
            onBlur={markTouched('phone')}
            fullWidth
            inputProps={{ inputMode: 'tel', maxLength: 15 }}
            placeholder="(00) 00000-0000"
            error={!!show('phone')}
            helperText={show('phone') || ' '}
          />
          <Button variant="contained" onClick={save} disabled={saving || !isValid}>
            {saving ? 'Salvando…' : 'Salvar alterações'}
          </Button>
        </Stack>
      </Paper>
    </Container>
  );
}
