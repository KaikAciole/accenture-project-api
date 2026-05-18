import { useEffect, useState } from 'react';
import {
  Typography, Paper, Skeleton, Alert, Table, TableHead, TableRow, TableCell, TableBody,
  TableContainer, Chip,
} from '@mui/material';
import AdminLayout from '../../components/AdminLayout';
import {
  COMPANY_OWNER_ID, getWalletByOwner, listCompanyTransactions,
} from '../../api/wallets';
import type { Wallet, WalletTransaction } from '../../api/types';

const fmt = (v: number | undefined) =>
  Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

const REASON_LABEL: Record<string, string> = {
  TOP_UP: 'Recarga',
  PAYMENT: 'Pagamento',
  SALE_RECEIVED: 'Venda',
  REFUND: 'Estorno',
  CANCELLATION: 'Cancelamento',
};

export default function AdminCompanyWallet() {
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [txs, setTxs] = useState<WalletTransaction[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getWalletByOwner(COMPANY_OWNER_ID, 'COMPANY')
      .then(setWallet)
      .catch((e: Error) => setError(e.message));
    listCompanyTransactions()
      .then((data) => setTxs(Array.isArray(data) ? data : data?.content || []))
      .catch(() => setTxs([]));
  }, []);

  if (error) return <AdminLayout><Alert severity="error">{error}</Alert></AdminLayout>;

  return (
    <AdminLayout>
      <Typography variant="h5" gutterBottom>Carteira da empresa</Typography>

      <Paper sx={{ p: { xs: 3, sm: 4 }, mb: 2, textAlign: 'center' }}>
        <Typography variant="overline" color="text.secondary">Saldo da empresa</Typography>
        {!wallet ? (
          <Skeleton width={200} height={60} sx={{ mx: 'auto' }} />
        ) : (
          <Typography
            variant="h2"
            color="primary"
            sx={{ fontSize: { xs: '2.5rem', sm: '3.75rem' }, wordBreak: 'break-word' }}
          >
            {fmt(wallet.balance)}
          </Typography>
        )}
      </Paper>

      <TableContainer component={Paper} sx={{ overflowX: 'auto' }}>
        <Typography variant="subtitle1" sx={{ p: 2 }}>Transações</Typography>
        {txs.length === 0 ? (
          <Typography sx={{ p: 2 }} color="text.secondary">Sem transações.</Typography>
        ) : (
          <Table sx={{ minWidth: 600 }}>
            <TableHead>
              <TableRow>
                <TableCell>Data</TableCell>
                <TableCell>Tipo</TableCell>
                <TableCell>Motivo</TableCell>
                <TableCell align="right">Valor</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {txs.map((t) => (
                <TableRow key={t.id}>
                  <TableCell>{t.createdAt ? new Date(t.createdAt).toLocaleString('pt-BR') : ''}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={t.type === 'CREDIT' ? 'Crédito' : 'Débito'}
                      color={t.type === 'CREDIT' ? 'success' : 'default'}
                    />
                  </TableCell>
                  <TableCell>{(t.reason && REASON_LABEL[t.reason]) || t.description || t.reason}</TableCell>
                  <TableCell align="right" sx={{ color: t.type === 'CREDIT' ? 'success.main' : 'text.primary' }}>
                    {t.type === 'CREDIT' ? '+' : '−'} {fmt(Math.abs(t.amount))}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </TableContainer>
    </AdminLayout>
  );
}
