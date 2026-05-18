import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  Container, Grid, Box, Typography, Paper, Button, Stack, Skeleton, Alert, Divider,
  Chip, Breadcrumbs, Link as MuiLink, IconButton, TextField,
} from '@mui/material';
import ImageNotSupportedIcon from '@mui/icons-material/ImageNotSupported';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import FlashOnIcon from '@mui/icons-material/FlashOn';
import LocalShippingOutlinedIcon from '@mui/icons-material/LocalShippingOutlined';
import VerifiedOutlinedIcon from '@mui/icons-material/VerifiedOutlined';
import { getProduct } from '../api/products';
import { useCart } from '../contexts/CartContext';
import { useSnackbar } from '../contexts/SnackbarContext';
import type { Product } from '../api/types';

const fmt = (v: number | undefined) =>
  Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

type StockStatus = { color: 'success' | 'warning' | 'error'; label: string };

const getStockStatus = (stock: number | undefined): StockStatus | null => {
  if (stock === undefined) return null;
  if (stock <= 0) return { color: 'error', label: 'Indisponível' };
  if (stock <= 5) return { color: 'warning', label: `Restam ${stock} unidade${stock > 1 ? 's' : ''}` };
  if (stock <= 10) return { color: 'warning', label: `${stock} em estoque` };
  return { color: 'success', label: 'Em estoque' };
};

export default function ProductDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { addItem } = useCart();
  const { notify } = useSnackbar();
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [quantity, setQuantity] = useState(1);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    getProduct(id)
      .then(setProduct)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <Container maxWidth="xl" sx={{ py: 4 }}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Skeleton variant="rectangular" height={500} sx={{ borderRadius: 2 }} />
          </Grid>
          <Grid item xs={12} md={6}>
            <Skeleton variant="text" height={48} width="80%" />
            <Skeleton variant="text" height={24} width="50%" sx={{ mt: 1 }} />
            <Skeleton variant="text" height={80} sx={{ mt: 2 }} />
            <Skeleton variant="rectangular" height={56} sx={{ mt: 3 }} />
          </Grid>
        </Grid>
      </Container>
    );
  }
  if (error) return <Container sx={{ py: 4 }}><Alert severity="error">{error}</Alert></Container>;
  if (!product) return null;

  const price = product.price ?? product.unitPrice ?? product.basePrice;
  const stock = product.stock ?? product.stockQuantity;
  const stockStatus = getStockStatus(stock);
  const isOutOfStock = stock !== undefined && stock <= 0;
  const maxQty = Math.max(stock ?? 1, 1);

  const incQty = () => setQuantity((q) => Math.min(q + 1, maxQty));
  const decQty = () => setQuantity((q) => Math.max(q - 1, 1));

  const addToCart = () => {
    addItem(product, quantity);
    notify(`${quantity}× adicionado${quantity > 1 ? 's' : ''} ao carrinho`, 'success');
  };

  const buyNow = () => {
    addItem(product, quantity);
    navigate('/cart');
  };

  return (
    <Container maxWidth="xl" sx={{ py: { xs: 2, sm: 3 } }}>
      <Breadcrumbs sx={{ mb: 2 }}>
        <MuiLink component={Link} to="/" underline="hover" color="inherit">
          Início
        </MuiLink>
        {product.category && (
          <MuiLink component={Link} to={`/?q=${encodeURIComponent(product.category)}`} underline="hover" color="inherit">
            {product.category}
          </MuiLink>
        )}
        <Typography color="text.primary" sx={{ maxWidth: 240, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {product.name}
        </Typography>
      </Breadcrumbs>

      <Grid container spacing={{ xs: 2, md: 4 }}>
        <Grid item xs={12} md={6}>
          <Paper
            sx={{
              p: { xs: 2, sm: 4 },
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              bgcolor: 'action.hover',
              minHeight: { xs: 320, sm: 500 },
              overflow: 'hidden',
              borderRadius: 3,
              '&:hover .detail-img': {
                transform: 'scale(1.05)',
              },
            }}
          >
            {product.imageUrl ? (
              <Box
                component="img"
                className="detail-img"
                src={product.imageUrl}
                alt={product.name}
                sx={{
                  maxWidth: '100%',
                  maxHeight: { xs: 320, sm: 500 },
                  objectFit: 'contain',
                  transition: 'transform 0.4s ease',
                }}
              />
            ) : (
              <ImageNotSupportedIcon sx={{ fontSize: 120, color: 'text.disabled' }} />
            )}
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1, flexWrap: 'wrap', gap: 1 }}>
            {product.category && (
              <Chip label={product.category} size="small" color="primary" variant="outlined" />
            )}
            {product.sku && (
              <Chip label={`SKU: ${product.sku}`} size="small" variant="outlined" />
            )}
          </Stack>

          <Typography
            variant="h4"
            sx={{ fontSize: { xs: '1.5rem', sm: '2rem' }, fontWeight: 700, wordBreak: 'break-word' }}
          >
            {product.name}
          </Typography>

          {product.description && (
            <Typography color="text.secondary" sx={{ mt: 1.5, lineHeight: 1.7, whiteSpace: 'pre-wrap' }}>
              {product.description}
            </Typography>
          )}

          <Divider sx={{ my: 2.5 }} />

          <Typography
            variant="h3"
            color="primary"
            sx={{ fontSize: { xs: '2rem', sm: '2.75rem' }, fontWeight: 700 }}
          >
            {fmt(price)}
          </Typography>

          {stockStatus && (
            <Chip
              label={stockStatus.label}
              color={stockStatus.color}
              size="small"
              sx={{ mt: 1.5, fontWeight: 600 }}
            />
          )}

          <Paper variant="outlined" sx={{ p: { xs: 2, sm: 2.5 }, mt: 3, borderRadius: 2 }}>
            <Stack spacing={2}>
              {!isOutOfStock && (
                <Stack direction="row" spacing={1.5} alignItems="center">
                  <Typography variant="body2" sx={{ fontWeight: 500 }}>Quantidade:</Typography>
                  <Box
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      border: 1,
                      borderColor: 'divider',
                      borderRadius: 2,
                    }}
                  >
                    <IconButton size="small" onClick={decQty} disabled={quantity <= 1} aria-label="diminuir">
                      <RemoveIcon fontSize="small" />
                    </IconButton>
                    <TextField
                      value={quantity}
                      size="small"
                      inputProps={{
                        style: { textAlign: 'center', padding: '4px 0', width: '36px' },
                        readOnly: true,
                      }}
                      variant="standard"
                      InputProps={{ disableUnderline: true }}
                    />
                    <IconButton size="small" onClick={incQty} disabled={quantity >= maxQty} aria-label="aumentar">
                      <AddIcon fontSize="small" />
                    </IconButton>
                  </Box>
                </Stack>
              )}

              <Stack spacing={1.5} direction={{ xs: 'column', sm: 'row' }}>
                <Button
                  fullWidth
                  variant="contained"
                  size="large"
                  startIcon={<ShoppingCartIcon />}
                  onClick={addToCart}
                  disabled={isOutOfStock}
                >
                  Adicionar ao carrinho
                </Button>
                <Button
                  fullWidth
                  variant="outlined"
                  size="large"
                  startIcon={<FlashOnIcon />}
                  onClick={buyNow}
                  disabled={isOutOfStock}
                >
                  Comprar agora
                </Button>
              </Stack>
            </Stack>
          </Paper>

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mt: 3 }}>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ color: 'text.secondary' }}>
              <LocalShippingOutlinedIcon fontSize="small" color="primary" />
              <Typography variant="caption">Entrega para todo o Brasil</Typography>
            </Stack>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ color: 'text.secondary' }}>
              <VerifiedOutlinedIcon fontSize="small" color="primary" />
              <Typography variant="caption">Compra 100% segura</Typography>
            </Stack>
          </Stack>
        </Grid>
      </Grid>
    </Container>
  );
}
