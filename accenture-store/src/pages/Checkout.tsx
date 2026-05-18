import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Container, Paper, Typography, Stack, RadioGroup, FormControlLabel, Radio, Button, Alert, Divider,
  Box, Skeleton, CircularProgress, Link as MuiLink,
} from '@mui/material';
import { useAuth } from '../contexts/AuthContext';
import { useCart } from '../contexts/CartContext';
import { useSnackbar } from '../contexts/SnackbarContext';
import { listAddresses, createAddress } from '../api/customers';
import { createOrder, getOrder } from '../api/orders';
import { createPayment, processPayment } from '../api/payments';
import { getWalletByOwner } from '../api/wallets';
import AddressForm from '../components/AddressForm';
import type { Address, Order, Wallet } from '../api/types';

const fmt = (v: number | undefined) =>
  Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

const wait = (ms: number) => new Promise((r) => setTimeout(r, ms));

export default function Checkout() {
  const { user } = useAuth();
  const { items, subtotal, clear } = useCart();
  const { notify } = useSnackbar();
  const navigate = useNavigate();

  const [addresses, setAddresses] = useState<Address[] | null>(null);
  const [addressId, setAddressId] = useState('');
  const [showNewAddress, setShowNewAddress] = useState(false);
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [processing, setProcessing] = useState(false);
  const [stage, setStage] = useState<'idle' | 'reserving' | 'paying'>('idle');

  useEffect(() => {
    if (items.length === 0) {
      navigate('/cart', { replace: true });
    }
  }, [items.length, navigate]);

  useEffect(() => {
    if (!user?.customerId) return;
    listAddresses(user.customerId)
      .then((data) => {
        const list = Array.isArray(data) ? data : data?.content || [];
        setAddresses(list);
        if (list[0]?.id) setAddressId(list[0].id);
        else setShowNewAddress(true);
      })
      .catch((e: Error) => notify(e.message, 'error'));
    getWalletByOwner(user.customerId, 'CUSTOMER')
      .then(setWallet)
      .catch(() => setWallet(null));
  }, [user?.customerId, notify]);

  const onCreateAddress = async (address: Address) => {
    if (!user?.customerId) return;
    try {
      const created = await createAddress(user.customerId, address);
      notify('Endereço cadastrado', 'success');
      setAddresses((prev) => (prev ? [...prev, created] : [created]));
      if (created.id) setAddressId(created.id);
      setShowNewAddress(false);
    } catch (e) {
      notify(e instanceof Error ? e.message : 'Erro ao cadastrar endereço', 'error');
    }
  };

  const pollUntilReserved = async (orderId: string): Promise<Order> => {
    for (let i = 0; i < 10; i++) {
      await wait(1000);
      const o = await getOrder(orderId);
      if (o.status === 'RESERVED' || o.status === 'PAID') return o;
      if (o.status === 'CANCELED') throw new Error('Pedido cancelado');
    }
    throw new Error('timeout');
  };

  const confirm = async () => {
    if (!addressId) {
      notify('Selecione um endereço de entrega', 'warning');
      return;
    }
    if (!user?.customerId) return;

    setProcessing(true);
    try {
      setStage('reserving');
      const order = await createOrder({
        addressId,
        items: items.map((it) => ({ sku: it.sku, quantity: it.quantity, unitPrice: it.unitPrice })),
      });
      const orderId = order.id || order.orderId;
      if (!orderId) throw new Error('Pedido criado sem ID');

      let reserved: Order;
      try {
        reserved = await pollUntilReserved(orderId);
      } catch (e) {
        if (e instanceof Error && e.message === 'timeout') {
          notify(
            'Estamos demorando mais que o esperado. Verifique seus pedidos em alguns instantes.',
            'warning',
          );
          navigate('/orders');
          return;
        }
        throw e;
      }

      setStage('paying');
      const amount = reserved.totalAmount ?? reserved.total ?? reserved.amount ?? subtotal;
      const payment = await createPayment({
        orderId,
        customerId: user.customerId,
        amount,
        method: 'WALLET',
      });
      await processPayment(payment.id);

      clear();
      notify('Pedido realizado com sucesso!', 'success');
      navigate(`/orders/${orderId}`);
    } catch (e) {
      notify(e instanceof Error ? e.message : 'Falha ao finalizar pedido', 'error');
    } finally {
      setProcessing(false);
      setStage('idle');
    }
  };

  if (addresses === null) {
    return <Container sx={{ py: 4 }}><Skeleton variant="rectangular" height={300} /></Container>;
  }

  const insufficientBalance = wallet !== null && wallet.balance < subtotal;
  const hasAddresses = addresses.length > 0;

  return (
    <Container maxWidth="md" sx={{ py: 3 }}>
      <Typography variant="h5" gutterBottom>Checkout</Typography>

      <Paper sx={{ p: 3, mb: 2 }}>
        <Typography variant="subtitle1" gutterBottom>Endereço de entrega</Typography>

        {hasAddresses && !showNewAddress && (
          <>
            <RadioGroup value={addressId} onChange={(e) => setAddressId(e.target.value)}>
              <Stack spacing={1}>
                {addresses.map((a) => (
                  <Paper key={a.id} variant="outlined" sx={{ p: 1.5 }}>
                    <FormControlLabel
                      value={a.id}
                      control={<Radio />}
                      sx={{ alignItems: 'flex-start', m: 0, width: '100%' }}
                      label={
                        <Box>
                          <Typography variant="body2">
                            {a.street}, {a.number}{a.complement ? ` — ${a.complement}` : ''}
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            {a.neighborhood || a.district} — {a.city}/{a.state} — CEP {a.zipCode}
                          </Typography>
                        </Box>
                      }
                    />
                  </Paper>
                ))}
              </Stack>
            </RadioGroup>
            <Box sx={{ mt: 1.5 }}>
              <MuiLink component="button" type="button" onClick={() => setShowNewAddress(true)}>
                Entregar em outro endereço
              </MuiLink>
            </Box>
          </>
        )}

        {(showNewAddress || !hasAddresses) && (
          <Box sx={{ mt: hasAddresses ? 2 : 0 }}>
            {!hasAddresses && (
              <Alert severity="info" sx={{ mb: 2 }}>
                Cadastre um endereço para continuar.
              </Alert>
            )}
            <AddressForm
              onCancel={hasAddresses ? () => setShowNewAddress(false) : undefined}
              onSubmit={onCreateAddress}
              submitLabel="Salvar e usar"
            />
          </Box>
        )}
      </Paper>

      <Paper sx={{ p: 3, mb: 2 }}>
        <Typography variant="subtitle1" gutterBottom>Resumo do pedido</Typography>
        <Stack spacing={1}>
          {items.map((it) => (
            <Stack key={it.sku} direction="row" justifyContent="space-between" spacing={2}>
              <Typography sx={{ wordBreak: 'break-word', flex: 1 }}>
                {it.name} × {it.quantity}
              </Typography>
              <Typography sx={{ flexShrink: 0 }}>{fmt(it.unitPrice * it.quantity)}</Typography>
            </Stack>
          ))}
        </Stack>
        <Divider sx={{ my: 2 }} />
        <Stack direction="row" justifyContent="space-between">
          <Typography variant="h6">Total</Typography>
          <Typography variant="h6" color="primary">{fmt(subtotal)}</Typography>
        </Stack>
      </Paper>

      <Paper sx={{ p: 3, mb: 2 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Typography variant="subtitle1">Pagamento com carteira</Typography>
          <Typography variant="body2" color="text.secondary">
            Saldo atual: <strong>{wallet ? fmt(wallet.balance) : '—'}</strong>
          </Typography>
        </Stack>
        {insufficientBalance && (
          <Alert
            severity="warning"
            sx={{ mt: 2 }}
            action={<Button color="inherit" size="small" onClick={() => navigate('/wallet')}>Ir para Carteira</Button>}
          >
            Saldo insuficiente. Vá em Carteira para recarregar.
          </Alert>
        )}
      </Paper>

      {processing && (
        <Alert severity="info" sx={{ mb: 2 }} icon={<CircularProgress size={20} />}>
          {stage === 'reserving' ? 'Reservando estoque…' : 'Processando pagamento…'}
        </Alert>
      )}

      <Button
        variant="contained"
        color="primary"
        size="large"
        fullWidth
        disabled={processing || !addressId || insufficientBalance || showNewAddress}
        onClick={confirm}
      >
        {processing ? 'Processando…' : 'Confirmar e pagar'}
      </Button>
    </Container>
  );
}
