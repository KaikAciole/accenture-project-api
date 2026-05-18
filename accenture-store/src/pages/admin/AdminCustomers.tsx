import { useEffect, useState } from 'react';
import {
  Typography, Paper, Skeleton, Alert, Table, TableHead, TableRow, TableCell, TableBody,
  TableContainer, Button, IconButton, Drawer, Box, Stack, Chip, Dialog, DialogTitle, DialogContent,
  DialogActions,
} from '@mui/material';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import DeleteIcon from '@mui/icons-material/Delete';
import CloseIcon from '@mui/icons-material/Close';
import AdminLayout from '../../components/AdminLayout';
import { useSnackbar } from '../../contexts/SnackbarContext';
import { deleteCustomer, listCustomers } from '../../api/customers';
import { listTransactionsByOwner } from '../../api/wallets';
import type { Customer, WalletTransaction } from '../../api/types';

const fmt = (v: number | undefined) =>
  Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

const REASON_LABEL: Record<string, string> = {
  TOP_UP: 'Recarga',
  PAYMENT: 'Pagamento',
  SALE_RECEIVED: 'Venda',
  REFUND: 'Estorno',
  CANCELLATION: 'Cancelamento',
};

export default function AdminCustomers() {
  const { notify } = useSnackbar();
  const [items, setItems] = useState<Customer[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [drawerFor, setDrawerFor] = useState<Customer | null>(null);
  const [txs, setTxs] = useState<WalletTransaction[] | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Customer | null>(null);
  const [deleting, setDeleting] = useState(false);

  const reload = () => {
    setItems(null);
    listCustomers(0, 100)
      .then((data) => setItems(Array.isArray(data) ? data : data?.content || []))
      .catch((e: Error) => setError(e.message));
  };
  useEffect(reload, []);

  const openDrawer = (c: Customer) => {
    setDrawerFor(c);
    setTxs(null);
    const ownerId = c.id || c.customerId;
    if (!ownerId) return;
    listTransactionsByOwner(ownerId, 'CUSTOMER')
      .then((data) => setTxs(Array.isArray(data) ? data : data?.content || []))
      .catch(() => setTxs([]));
  };

  const handleDelete = async () => {
    const cid = confirmDelete?.id || confirmDelete?.customerId;
    if (!cid) return;
    setDeleting(true);
    try {
      await deleteCustomer(cid);
      notify('Cliente excluído', 'success');
      setConfirmDelete(null);
      reload();
    } catch (e) {
      notify(e instanceof Error ? e.message : 'Erro ao excluir', 'error');
    } finally { setDeleting(false); }
  };

  return (
    <AdminLayout>
      <Typography variant="h5" gutterBottom>Clientes</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      <TableContainer component={Paper} sx={{ overflowX: 'auto' }}>
        {items === null ? (
          <Skeleton variant="rectangular" height={300} />
        ) : items.length === 0 ? (
          <Typography sx={{ p: 3 }} color="text.secondary">Sem clientes.</Typography>
        ) : (
          <Table sx={{ minWidth: 700 }}>
            <TableHead>
              <TableRow>
                <TableCell>Nome</TableCell>
                <TableCell>E-mail</TableCell>
                <TableCell>CPF</TableCell>
                <TableCell>Telefone</TableCell>
                <TableCell align="right" />
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map((c) => (
                <TableRow key={c.id || c.customerId} hover>
                  <TableCell>{c.name}</TableCell>
                  <TableCell sx={{ wordBreak: 'break-all' }}>{c.email}</TableCell>
                  <TableCell>{c.cpf}</TableCell>
                  <TableCell>{c.phone}</TableCell>
                  <TableCell align="right">
                    <Button
                      startIcon={<ReceiptLongIcon />}
                      onClick={() => openDrawer(c)}
                      size="small"
                    >
                      Ver extrato
                    </Button>
                    <IconButton onClick={() => setConfirmDelete(c)} aria-label="excluir" color="error">
                      <DeleteIcon />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </TableContainer>

      <Drawer
        anchor="right"
        open={!!drawerFor}
        onClose={() => setDrawerFor(null)}
        PaperProps={{ sx: { width: { xs: '100%', sm: 480 } } }}
      >
        <Box sx={{ p: 3 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
            <Typography variant="h6">Extrato de {drawerFor?.name}</Typography>
            <IconButton onClick={() => setDrawerFor(null)} aria-label="fechar"><CloseIcon /></IconButton>
          </Stack>
          {txs === null ? (
            <Skeleton variant="rectangular" height={200} />
          ) : txs.length === 0 ? (
            <Typography color="text.secondary">Sem transações.</Typography>
          ) : (
            <Stack spacing={1}>
              {txs.map((t) => (
                <Paper key={t.id} variant="outlined" sx={{ p: 1.5 }}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Stack>
                      <Stack direction="row" spacing={1} alignItems="center">
                        <Chip
                          size="small"
                          label={t.type === 'CREDIT' ? 'Crédito' : 'Débito'}
                          color={t.type === 'CREDIT' ? 'success' : 'default'}
                        />
                        <Typography variant="body2">
                          {(t.reason && REASON_LABEL[t.reason]) || t.description || t.reason}
                        </Typography>
                      </Stack>
                      <Typography variant="caption" color="text.secondary">
                        {t.createdAt ? new Date(t.createdAt).toLocaleString('pt-BR') : ''}
                      </Typography>
                    </Stack>
                    <Typography
                      sx={{ color: t.type === 'CREDIT' ? 'success.main' : 'text.primary' }}
                    >
                      {t.type === 'CREDIT' ? '+' : '−'} {fmt(Math.abs(t.amount))}
                    </Typography>
                  </Stack>
                </Paper>
              ))}
            </Stack>
          )}
        </Box>
      </Drawer>

      <Dialog open={!!confirmDelete} onClose={() => setConfirmDelete(null)}>
        <DialogTitle>Excluir cliente</DialogTitle>
        <DialogContent>
          <Typography>
            Excluir <strong>{confirmDelete?.name}</strong>? Esta ação não pode ser desfeita.
          </Typography>
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
