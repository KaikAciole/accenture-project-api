import { useNavigate } from 'react-router-dom';
import {
  Container, Typography, Paper, Stack, IconButton, TextField, Button, Box, Divider, Alert,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { useCart } from '../contexts/CartContext';
import { useAuth } from '../contexts/AuthContext';

const fmt = (v: number | undefined) =>
  Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

export default function Cart() {
  const { items, updateQuantity, removeItem, subtotal } = useCart();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const goToCheckout = () => {
    if (!isAuthenticated) { navigate('/login'); return; }
    navigate('/checkout');
  };

  if (items.length === 0) {
    return (
      <Container maxWidth="md" sx={{ py: 6 }}>
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h6">Seu carrinho está vazio.</Typography>
          <Button sx={{ mt: 2 }} variant="contained" onClick={() => navigate('/')}>Ver produtos</Button>
        </Paper>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ py: 3 }}>
      <Typography variant="h5" gutterBottom>Seu carrinho</Typography>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
        <Paper sx={{ p: 2, flex: 1 }}>
          {items.map((it, idx) => (
            <Box key={it.sku}>
              {idx > 0 && <Divider sx={{ my: 2 }} />}
              <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                alignItems={{ xs: 'stretch', sm: 'center' }}
              >
                <Stack direction="row" spacing={2} alignItems="center" sx={{ flex: 1, minWidth: 0 }}>
                  <Box
                    component="img"
                    src={it.imageUrl || 'https://placehold.co/100x100?text=P'}
                    alt={it.name}
                    sx={{ width: 80, height: 80, objectFit: 'contain', bgcolor: 'background.default', flexShrink: 0 }}
                  />
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography sx={{ wordBreak: 'break-word' }}>{it.name}</Typography>
                    <Typography color="primary">{fmt(it.unitPrice)}</Typography>
                  </Box>
                </Stack>
                <Stack
                  direction="row"
                  spacing={1}
                  alignItems="center"
                  justifyContent={{ xs: 'space-between', sm: 'flex-end' }}
                >
                  <TextField
                    type="number"
                    size="small"
                    value={it.quantity}
                    onChange={(e) => updateQuantity(it.sku, parseInt(e.target.value || '0', 10))}
                    inputProps={{ min: 0, style: { width: 60 } }}
                  />
                  <Typography sx={{ minWidth: 90, textAlign: 'right' }}>{fmt(it.unitPrice * it.quantity)}</Typography>
                  <IconButton onClick={() => removeItem(it.sku)} aria-label="remover"><DeleteIcon /></IconButton>
                </Stack>
              </Stack>
            </Box>
          ))}
        </Paper>
        <Paper sx={{ p: 2, width: { xs: '100%', md: 320 }, alignSelf: 'flex-start' }}>
          <Typography variant="subtitle1">Resumo</Typography>
          <Stack direction="row" justifyContent="space-between" sx={{ mt: 2 }}>
            <Typography>Subtotal</Typography>
            <Typography>{fmt(subtotal)}</Typography>
          </Stack>
          <Stack direction="row" justifyContent="space-between" sx={{ mt: 1 }}>
            <Typography variant="h6">Total</Typography>
            <Typography variant="h6" color="primary">{fmt(subtotal)}</Typography>
          </Stack>
          {!isAuthenticated && <Alert severity="info" sx={{ mt: 2 }}>Você precisa entrar para finalizar.</Alert>}
          <Button fullWidth variant="contained" sx={{ mt: 2 }} onClick={goToCheckout}>
            Finalizar pedido
          </Button>
        </Paper>
      </Stack>
    </Container>
  );
}
