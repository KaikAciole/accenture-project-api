import { useEffect, useState, type ChangeEvent } from 'react';
import {
  Typography, Paper, Stack, Button, Skeleton, Alert, Table, TableHead, TableRow, TableCell, TableBody,
  TableContainer, IconButton, Dialog, DialogTitle, DialogContent, DialogActions, TextField, Grid,
  Box,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ImageNotSupportedIcon from '@mui/icons-material/ImageNotSupported';
import AdminLayout from '../../components/AdminLayout';
import { useSnackbar } from '../../contexts/SnackbarContext';
import {
  createProduct, deleteProduct, listProducts, updateProduct,
} from '../../api/products';
import type { Product, ProductPayload } from '../../api/types';

const fmt = (v: number | undefined) =>
  Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

interface FormState {
  sku: string;
  name: string;
  description: string;
  category: string;
  basePrice: string;
  stockQuantity: string;
  imageUrl: string;
}

const empty: FormState = {
  sku: '', name: '', description: '', category: '', basePrice: '', stockQuantity: '', imageUrl: '',
};

const toForm = (p: Product): FormState => ({
  sku: p.sku || '',
  name: p.name,
  description: p.description || '',
  category: p.category || '',
  basePrice: String(p.basePrice ?? p.price ?? p.unitPrice ?? ''),
  stockQuantity: String(p.stockQuantity ?? p.stock ?? ''),
  imageUrl: p.imageUrl || '',
});

const toPayload = (f: FormState): ProductPayload => ({
  sku: f.sku.trim(),
  name: f.name.trim(),
  description: f.description.trim() || undefined,
  category: f.category.trim(),
  basePrice: Number(f.basePrice.replace(',', '.')),
  stockQuantity: parseInt(f.stockQuantity, 10),
  imageUrl: f.imageUrl.trim() || undefined,
});

type FieldKey = keyof FormState;
type FieldErrors = Partial<Record<FieldKey, string>>;

const URL_RE = /^https?:\/\/.+/i;

function validateForm(f: FormState, editing: boolean): FieldErrors {
  const errs: FieldErrors = {};
  if (!f.sku.trim()) errs.sku = 'SKU obrigatório';
  if (!f.name.trim()) errs.name = 'Nome obrigatório';
  if (!editing && !f.description.trim()) errs.description = 'Descrição obrigatória';
  if (!f.category.trim()) errs.category = 'Categoria obrigatória';

  const priceStr = f.basePrice.trim().replace(',', '.');
  if (!priceStr) errs.basePrice = 'Preço obrigatório';
  else if (!/^\d+(\.\d{1,2})?$/.test(priceStr)) errs.basePrice = 'Use formato 0,00';
  else if (Number(priceStr) <= 0) errs.basePrice = 'Preço deve ser maior que zero';

  const stockStr = f.stockQuantity.trim();
  if (!stockStr) errs.stockQuantity = 'Estoque obrigatório';
  else if (!/^\d+$/.test(stockStr)) errs.stockQuantity = 'Use apenas números inteiros';
  else if (Number(stockStr) < 0) errs.stockQuantity = 'Estoque não pode ser negativo';

  if (f.imageUrl.trim() && !URL_RE.test(f.imageUrl.trim())) {
    errs.imageUrl = 'URL deve começar com http(s)://';
  }
  return errs;
}

export default function AdminProducts() {
  const { notify } = useSnackbar();
  const [items, setItems] = useState<Product[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Product | null>(null);
  const [form, setForm] = useState<FormState>(empty);
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<Product | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [touched, setTouched] = useState<Partial<Record<FieldKey, boolean>>>({});

  const set = (k: FieldKey) => (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    let v = e.target.value;
    if (k === 'stockQuantity') {
      v = v.replace(/[^\d]/g, '');
    } else if (k === 'basePrice') {
      v = v.replace(/[^\d.,]/g, '');
      const firstSep = v.search(/[.,]/);
      if (firstSep >= 0) {
        v = v.slice(0, firstSep + 1) + v.slice(firstSep + 1).replace(/[.,]/g, '');
      }
    }
    setForm((f) => ({ ...f, [k]: v }));
  };

  const markTouched = (k: FieldKey) => () =>
    setTouched((t) => ({ ...t, [k]: true }));

  const reload = () => {
    setItems(null);
    listProducts({ size: 100 })
      .then((data) => setItems(Array.isArray(data) ? data : data?.content || []))
      .catch((e: Error) => setError(e.message));
  };
  useEffect(reload, []);

  const openCreate = () => {
    setEditing(null);
    setForm(empty);
    setFormError(null);
    setTouched({});
    setFormOpen(true);
  };
  const openEdit = (p: Product) => {
    setEditing(p);
    setForm(toForm(p));
    setFormError(null);
    setTouched({});
    setFormOpen(true);
  };

  const errs = validateForm(form, !!editing);
  const isValid = Object.keys(errs).length === 0;
  const show = (k: FieldKey) => (touched[k] ? errs[k] : undefined);

  const submit = async () => {
    setTouched({
      sku: true, name: true, description: true, category: true,
      basePrice: true, stockQuantity: true, imageUrl: true,
    });
    if (!isValid) {
      setFormError('Verifique os campos destacados antes de salvar.');
      return;
    }
    setFormError(null);
    setSaving(true);
    try {
      const payload = toPayload(form);
      if (editing?.id) {
        await updateProduct(editing.id, payload);
        notify('Produto atualizado', 'success');
      } else {
        await createProduct(payload);
        notify('Produto criado', 'success');
      }
      setFormOpen(false);
      reload();
    } catch (e) {
      notify(e instanceof Error ? e.message : 'Erro ao salvar produto', 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!confirmDelete?.id) return;
    setDeleting(true);
    try {
      await deleteProduct(confirmDelete.id);
      notify('Produto excluído', 'success');
      setConfirmDelete(null);
      reload();
    } catch (e) {
      notify(e instanceof Error ? e.message : 'Erro ao excluir', 'error');
    } finally { setDeleting(false); }
  };

  return (
    <AdminLayout>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="h5">Produtos</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
          Novo produto
        </Button>
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <TableContainer component={Paper} sx={{ overflowX: 'auto' }}>
        {items === null ? (
          <Skeleton variant="rectangular" height={300} />
        ) : items.length === 0 ? (
          <Typography sx={{ p: 3 }} color="text.secondary">Sem produtos cadastrados.</Typography>
        ) : (
          <Table sx={{ minWidth: 700 }}>
            <TableHead>
              <TableRow>
                <TableCell />
                <TableCell>SKU</TableCell>
                <TableCell>Nome</TableCell>
                <TableCell>Categoria</TableCell>
                <TableCell align="right">Preço</TableCell>
                <TableCell align="right">Estoque</TableCell>
                <TableCell />
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map((p) => (
                <TableRow key={p.id || p.sku} hover>
                  <TableCell sx={{ width: 56 }}>
                    <Box sx={{
                      width: 40, height: 40, bgcolor: 'action.hover',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}>
                      {p.imageUrl ? (
                        <Box component="img" src={p.imageUrl} alt="" sx={{ maxWidth: '100%', maxHeight: '100%' }} />
                      ) : (
                        <ImageNotSupportedIcon fontSize="small" color="disabled" />
                      )}
                    </Box>
                  </TableCell>
                  <TableCell>{p.sku}</TableCell>
                  <TableCell sx={{ maxWidth: 240, wordBreak: 'break-word' }}>{p.name}</TableCell>
                  <TableCell>{p.category || '—'}</TableCell>
                  <TableCell align="right">{fmt(p.basePrice ?? p.price ?? p.unitPrice)}</TableCell>
                  <TableCell align="right">{p.stockQuantity ?? p.stock ?? 0}</TableCell>
                  <TableCell align="right">
                    <IconButton onClick={() => openEdit(p)} aria-label="editar"><EditIcon /></IconButton>
                    <IconButton onClick={() => setConfirmDelete(p)} aria-label="excluir"><DeleteIcon /></IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </TableContainer>

      <Dialog open={formOpen} onClose={() => setFormOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editing ? 'Editar produto' : 'Novo produto'}</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 0 }}>
            {formError && (
              <Grid item xs={12}><Alert severity="error">{formError}</Alert></Grid>
            )}
            <Grid item xs={12} sm={6}>
              <TextField
                label="SKU"
                value={form.sku}
                onChange={set('sku')}
                onBlur={markTouched('sku')}
                fullWidth
                required
                disabled={!!editing}
                error={!!show('sku')}
                helperText={show('sku') || ' '}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Categoria"
                value={form.category}
                onChange={set('category')}
                onBlur={markTouched('category')}
                fullWidth
                required
                error={!!show('category')}
                helperText={show('category') || ' '}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Nome"
                value={form.name}
                onChange={set('name')}
                onBlur={markTouched('name')}
                fullWidth
                required
                error={!!show('name')}
                helperText={show('name') || ' '}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Descrição"
                value={form.description}
                onChange={set('description')}
                onBlur={markTouched('description')}
                fullWidth
                required={!editing}
                multiline
                minRows={3}
                error={!!show('description')}
                helperText={show('description') || (editing ? 'Deixe em branco para manter a descrição atual' : ' ')}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Preço base (R$)"
                value={form.basePrice}
                onChange={set('basePrice')}
                onBlur={markTouched('basePrice')}
                fullWidth
                required
                inputProps={{ inputMode: 'decimal' }}
                placeholder="0,00"
                error={!!show('basePrice')}
                helperText={show('basePrice') || ' '}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label="Estoque"
                value={form.stockQuantity}
                onChange={set('stockQuantity')}
                onBlur={markTouched('stockQuantity')}
                fullWidth
                required
                inputProps={{ inputMode: 'numeric', pattern: '[0-9]*', min: 0 }}
                onKeyDown={(e) => {
                  if (['e', 'E', '+', '-', '.', ','].includes(e.key)) e.preventDefault();
                }}
                error={!!show('stockQuantity')}
                helperText={show('stockQuantity') || ' '}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="URL da imagem"
                value={form.imageUrl}
                onChange={set('imageUrl')}
                onBlur={markTouched('imageUrl')}
                fullWidth
                error={!!show('imageUrl')}
                helperText={show('imageUrl') || ' '}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFormOpen(false)}>Cancelar</Button>
          <Button variant="contained" onClick={submit} disabled={saving || !isValid}>
            {saving ? 'Salvando…' : 'Salvar'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={!!confirmDelete} onClose={() => setConfirmDelete(null)}>
        <DialogTitle>Excluir produto</DialogTitle>
        <DialogContent>
          <Typography>Excluir <strong>{confirmDelete?.name}</strong>?</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDelete(null)}>Cancelar</Button>
          <Button color="error" variant="contained" onClick={handleDelete} disabled={deleting}>
            {deleting ? 'Excluindo…' : 'Excluir'}
          </Button>
        </DialogActions>
      </Dialog>
    </AdminLayout>
  );
}
