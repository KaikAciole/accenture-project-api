import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Container, Typography, Paper, Table, TableHead, TableRow, TableCell, TableBody, TableContainer,
  Skeleton, Alert, Chip, Button, Box, Stack,
} from '@mui/material';
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import ShoppingBagOutlinedIcon from '@mui/icons-material/ShoppingBagOutlined';
import { listMyOrders } from '../api/orders';
import type { Order } from '../api/types';

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

export default function Orders() {
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listMyOrders()
      .then((data) => {
        if (Array.isArray(data)) return setOrders(data);
        setOrders(data?.data || data?.content || data?.items || []);
      })
      .catch((e: Error) => setError(e.message));
  }, []);

  return (
    <Container maxWidth="lg" sx={{ py: 3 }}>
      <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
        <ReceiptLongOutlinedIcon color="primary" />
        <Typography variant="h5">Meus pedidos</Typography>
      </Stack>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {orders === null ? (
        <Skeleton variant="rectangular" height={200} />
      ) : orders.length === 0 ? (
        <Paper sx={{ p: { xs: 4, sm: 6 }, textAlign: 'center' }}>
          <ShoppingBagOutlinedIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 1.5 }} />
          <Typography variant="h6" gutterBottom>Você ainda não fez pedidos</Typography>
          <Typography color="text.secondary" sx={{ mb: 3 }}>
            Quando finalizar uma compra, ela aparecerá aqui.
          </Typography>
          <Button component={Link} to="/" variant="contained">Ver produtos</Button>
        </Paper>
      ) : (
        <TableContainer component={Paper} sx={{ overflowX: 'auto' }}>
          <Table sx={{ minWidth: 600 }}>
            <TableHead>
              <TableRow>
                <TableCell>Pedido</TableCell>
                <TableCell>Data</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Total</TableCell>
                <TableCell />
              </TableRow>
            </TableHead>
            <TableBody>
              {orders.map((o) => {
                const oid = o.id || o.orderId;
                const shortId = oid ? oid.slice(0, 8) : '—';
                return (
                  <TableRow key={oid} hover>
                    <TableCell>
                      <Box
                        component="span"
                        sx={{
                          fontFamily: 'monospace',
                          fontWeight: 600,
                          bgcolor: 'action.hover',
                          px: 1,
                          py: 0.25,
                          borderRadius: 1,
                          fontSize: '0.85rem',
                        }}
                      >
                        #{shortId}
                      </Box>
                    </TableCell>
                    <TableCell>{o.createdAt ? new Date(o.createdAt).toLocaleString('pt-BR') : '—'}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={o.status}
                        color={(o.status && STATUS_COLORS[o.status]) || 'default'}
                      />
                    </TableCell>
                    <TableCell align="right" sx={{ fontWeight: 600 }}>
                      {fmt(o.totalAmount ?? o.total ?? o.amount)}
                    </TableCell>
                    <TableCell align="right">
                      <Button component={Link} to={`/orders/${oid}`} size="small" variant="outlined">
                        Detalhes
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Container>
  );
}
