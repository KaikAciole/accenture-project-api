import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Typography, Paper, Skeleton, Alert, Table, TableHead, TableRow, TableCell, TableBody,
  TableContainer, Chip, Button,
} from '@mui/material';
import AdminLayout from '../../components/AdminLayout';
import { listAllOrders } from '../../api/orders';
import type { Order } from '../../api/types';

const fmt = (v: number | undefined) =>
  Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

type ChipColor = 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
const STATUS_COLORS: Record<string, ChipColor> = {
  PENDING: 'warning',
  RESERVED: 'info',
  PAID: 'success',
  CANCELED: 'default',
  REFUNDED: 'secondary',
};

export default function AdminOrders() {
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listAllOrders(0, 100)
      .then((res) => setOrders(
        Array.isArray(res) ? res : res?.data || res?.content || res?.items || [],
      ))
      .catch((e: Error) => setError(e.message));
  }, []);

  return (
    <AdminLayout>
      <Typography variant="h5" gutterBottom>Pedidos</Typography>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      <TableContainer component={Paper} sx={{ overflowX: 'auto' }}>
        {orders === null ? (
          <Skeleton variant="rectangular" height={300} />
        ) : orders.length === 0 ? (
          <Typography sx={{ p: 3 }} color="text.secondary">Nenhum pedido.</Typography>
        ) : (
          <Table sx={{ minWidth: 700 }}>
            <TableHead>
              <TableRow>
                <TableCell>Pedido</TableCell>
                <TableCell>Cliente</TableCell>
                <TableCell>Data</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Total</TableCell>
                <TableCell />
              </TableRow>
            </TableHead>
            <TableBody>
              {orders.map((o) => {
                const oid = o.id || o.orderId;
                return (
                  <TableRow key={oid} hover>
                    <TableCell sx={{ maxWidth: 160, wordBreak: 'break-all' }}>{oid}</TableCell>
                    <TableCell sx={{ maxWidth: 200, wordBreak: 'break-all' }}>{o.customerId}</TableCell>
                    <TableCell>{o.createdAt ? new Date(o.createdAt).toLocaleString('pt-BR') : '—'}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={o.status}
                        color={(o.status && STATUS_COLORS[o.status]) || 'default'}
                      />
                    </TableCell>
                    <TableCell align="right">{fmt(o.totalAmount ?? o.total ?? o.amount)}</TableCell>
                    <TableCell align="right">
                      <Button component={Link} to={`/orders/${oid}`} size="small">Detalhes</Button>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
      </TableContainer>
    </AdminLayout>
  );
}
