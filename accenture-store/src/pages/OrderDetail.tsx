import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Container, Typography, Paper, Stack, Chip, Skeleton, Alert, Divider, Button, Dialog,
  DialogTitle, DialogContent, DialogActions, Box,
} from '@mui/material';
import { cancelOrder, getOrder } from '../api/orders';
import { getPaymentByOrder } from '../api/payments';
import { useSnackbar } from '../contexts/SnackbarContext';
import type { Order, OrderStatus, Payment } from '../api/types';

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

const CANCELABLE: OrderStatus[] = ['PENDING', 'RESERVED', 'PAID'];

type PaymentState = Payment | 'loading' | null;

export default function OrderDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { notify } = useSnackbar();
  const [order, setOrder] = useState<Order | null>(null);
  const [payment, setPayment] = useState<PaymentState>('loading');
  const [error, setError] = useState<string | null>(null);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [cancelling, setCancelling] = useState(false);

  const load = () => {
    if (!id) return;
    getOrder(id).then(setOrder).catch((e: Error) => setError(e.message));
    getPaymentByOrder(id).then(setPayment).catch(() => setPayment(null));
  };

  useEffect(load, [id]);

  const handleCancel = async () => {
    if (!id) return;
    setCancelling(true);
    try {
      await cancelOrder(id);
      notify('Pedido cancelado', 'success');
      setConfirmCancel(false);
      window.location.reload();
    } catch (e) {
      notify(e instanceof Error ? e.message : 'Falha ao cancelar', 'error');
    } finally {
      setCancelling(false);
    }
  };

  if (error) return <Container sx={{ py: 4 }}><Alert severity="error">{error}</Alert></Container>;
  if (!order) return <Container sx={{ py: 4 }}><Skeleton variant="rectangular" height={300} /></Container>;

  const total = order.totalAmount ?? order.total ?? order.amount;
  const addr = order.deliveryAddress;
  const canCancel = !!order.status && CANCELABLE.includes(order.status);

  return (
    <Container maxWidth="md" sx={{ py: 3 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="h5">Pedido #{order.id || order.orderId}</Typography>
        <Button variant="text" onClick={() => navigate('/orders')}>Voltar</Button>
      </Stack>

      <Paper sx={{ p: 3, mb: 2 }}>
        <Stack direction="row" spacing={2} alignItems="center">
          <Chip
            label={order.status}
            color={(order.status && STATUS_COLORS[order.status]) || 'default'}
          />
          <Typography color="text.secondary">
            {order.createdAt ? new Date(order.createdAt).toLocaleString('pt-BR') : ''}
          </Typography>
        </Stack>
        <Divider sx={{ my: 2 }} />
        <Stack spacing={1}>
          {(order.items || []).map((it) => (
            <Stack key={it.sku} direction="row" justifyContent="space-between" spacing={2}>
              <Typography sx={{ wordBreak: 'break-word', flex: 1 }}>
                {it.name || it.sku} × {it.quantity}
              </Typography>
              <Typography sx={{ flexShrink: 0 }}>{fmt(it.unitPrice * it.quantity)}</Typography>
            </Stack>
          ))}
        </Stack>
        <Divider sx={{ my: 2 }} />
        <Stack direction="row" justifyContent="space-between">
          <Typography variant="h6">Total</Typography>
          <Typography variant="h6" color="primary">{fmt(total)}</Typography>
        </Stack>
      </Paper>

      {addr && (
        <Paper sx={{ p: 3, mb: 2 }}>
          <Typography variant="subtitle1" gutterBottom>Endereço de entrega</Typography>
          <Typography variant="body2">
            {addr.street}, {addr.number}{addr.complement ? ` — ${addr.complement}` : ''}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {addr.neighborhood} — {addr.city}/{addr.state}
          </Typography>
          <Typography variant="body2" color="text.secondary">CEP {addr.zipCode}</Typography>
        </Paper>
      )}

      <Paper sx={{ p: 3, mb: 2 }}>
        <Typography variant="subtitle1" gutterBottom>Pagamento</Typography>
        {payment === 'loading' ? (
          <Skeleton height={40} />
        ) : !payment ? (
          <Alert severity="info">Aguardando pagamento.</Alert>
        ) : (
          <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap">
            <Chip
              label={payment.status}
              color={payment.status === 'APPROVED' ? 'success' : payment.status === 'REFUSED' ? 'error' : 'warning'}
            />
            <Typography>{payment.method === 'WALLET' ? 'Carteira' : payment.method}</Typography>
            <Typography color="text.secondary">{fmt(payment.amount)}</Typography>
          </Stack>
        )}
      </Paper>

      {canCancel && (
        <Box>
          <Button color="error" variant="outlined" onClick={() => setConfirmCancel(true)}>
            Cancelar pedido
          </Button>
        </Box>
      )}

      <Dialog open={confirmCancel} onClose={() => setConfirmCancel(false)}>
        <DialogTitle>Cancelar pedido</DialogTitle>
        <DialogContent>
          <Typography>
            Tem certeza? Se o pedido já estava pago, o valor será estornado para sua carteira.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmCancel(false)}>Voltar</Button>
          <Button color="error" variant="contained" onClick={handleCancel} disabled={cancelling}>
            {cancelling ? 'Cancelando…' : 'Cancelar pedido'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}
