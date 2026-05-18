import { useState, type ChangeEvent } from 'react';
import {
  Grid, TextField, Button, Stack, InputAdornment, CircularProgress,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { lookupCep } from '../api/cep';
import { useSnackbar } from '../contexts/SnackbarContext';
import { ApiError } from '../api/client';
import type { Address } from '../api/types';

const emptyAddress: Address = {
  zipCode: '', street: '', number: '', complement: '', neighborhood: '', city: '', state: '',
};

const onlyDigits = (s: string) => s.replace(/\D/g, '');
const formatCep = (s: string) => {
  const d = onlyDigits(s).slice(0, 8);
  return d.length > 5 ? `${d.slice(0, 5)}-${d.slice(5)}` : d;
};

interface AddressFormProps {
  initial?: Address;
  onCancel?: () => void;
  onSubmit: (address: Address) => Promise<void> | void;
  submitLabel?: string;
}

export default function AddressForm({
  initial,
  onCancel,
  onSubmit,
  submitLabel = 'Salvar endereço',
}: AddressFormProps) {
  const { notify } = useSnackbar();
  const [form, setForm] = useState<Address>(initial || emptyAddress);
  const [filledFromCep, setFilledFromCep] = useState(!!initial?.street);
  const [cepLoading, setCepLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const set = (k: keyof Address) => (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const v = k === 'zipCode' ? formatCep(e.target.value) : e.target.value;
    setForm((f) => ({ ...f, [k]: v }));
    if (k === 'zipCode') setFilledFromCep(false);
  };

  const searchCep = async () => {
    const clean = onlyDigits(form.zipCode);
    if (clean.length !== 8) {
      notify('Informe um CEP com 8 dígitos', 'warning');
      return;
    }
    setCepLoading(true);
    try {
      const res = await lookupCep(clean);
      setForm((f) => ({
        ...f,
        zipCode: formatCep(clean),
        street: res.street || res.logradouro || '',
        complement: res.complement || f.complement || '',
        neighborhood: res.neighborhood || res.district || res.bairro || '',
        city: res.city || res.localidade || '',
        state: res.state || res.uf || '',
      }));
      setFilledFromCep(true);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        notify('CEP não encontrado, verifique o número', 'warning');
      } else if (err instanceof ApiError && err.status === 502) {
        notify('Não conseguimos consultar o CEP agora, tente em alguns minutos', 'error');
      } else {
        notify(err instanceof Error ? err.message : 'Falha ao consultar CEP', 'error');
      }
    } finally {
      setCepLoading(false);
    }
  };

  const handleSubmit = async () => {
    if (!filledFromCep) {
      notify('Busque o CEP antes de salvar', 'warning');
      return;
    }
    if (!form.number.trim()) {
      notify('Informe o número', 'warning');
      return;
    }
    setSaving(true);
    try {
      await onSubmit({
        ...form,
        zipCode: onlyDigits(form.zipCode),
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Stack spacing={2}>
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6}>
          <TextField
            label="CEP"
            value={form.zipCode}
            onChange={set('zipCode')}
            fullWidth
            inputProps={{ inputMode: 'numeric', maxLength: 9 }}
            placeholder="00000-000"
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <Button
                    onClick={searchCep}
                    disabled={cepLoading || onlyDigits(form.zipCode).length !== 8}
                    startIcon={cepLoading ? <CircularProgress size={16} /> : <SearchIcon />}
                    size="small"
                  >
                    Buscar
                  </Button>
                </InputAdornment>
              ),
            }}
          />
        </Grid>
      </Grid>

      {filledFromCep && (
        <>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={8}>
              <TextField label="Logradouro" value={form.street} onChange={set('street')} fullWidth />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField label="Bairro" value={form.neighborhood || ''} onChange={set('neighborhood')} fullWidth />
            </Grid>
            <Grid item xs={8} sm={8}>
              <TextField label="Cidade" value={form.city} onChange={set('city')} fullWidth />
            </Grid>
            <Grid item xs={4} sm={4}>
              <TextField label="UF" value={form.state} onChange={set('state')} fullWidth inputProps={{ maxLength: 2, style: { textTransform: 'uppercase' } }} />
            </Grid>
            <Grid item xs={6} sm={4}>
              <TextField
                label="Número"
                value={form.number}
                onChange={set('number')}
                fullWidth
                required
                autoFocus
              />
            </Grid>
            <Grid item xs={6} sm={8}>
              <TextField
                label="Complemento"
                value={form.complement || ''}
                onChange={set('complement')}
                fullWidth
              />
            </Grid>
          </Grid>
        </>
      )}

      <Stack direction="row" spacing={1} justifyContent="flex-end">
        {onCancel && <Button onClick={onCancel}>Cancelar</Button>}
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={saving || !filledFromCep}
        >
          {saving ? 'Salvando…' : submitLabel}
        </Button>
      </Stack>
    </Stack>
  );
}
