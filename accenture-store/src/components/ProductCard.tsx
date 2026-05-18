import { Card, CardActionArea, CardContent, Typography, Button, Box, CardActions, Chip } from '@mui/material';
import ImageNotSupportedIcon from '@mui/icons-material/ImageNotSupported';
import { Link } from 'react-router-dom';
import { useCart } from '../contexts/CartContext';
import { useSnackbar } from '../contexts/SnackbarContext';
import type { Product } from '../api/types';

const fmt = (v: number | undefined) =>
  Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

interface ProductCardProps {
  product: Product;
}

export default function ProductCard({ product }: ProductCardProps) {
  const { addItem } = useCart();
  const { notify } = useSnackbar();
  const id = product.id || product.sku;
  const price = product.price ?? product.unitPrice ?? product.basePrice;
  const stock = product.stockQuantity ?? product.stock;

  const isOutOfStock = stock !== undefined && stock <= 0;
  const isLowStock = stock !== undefined && stock > 0 && stock <= 5;

  return (
    <Card
      sx={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        transition: 'transform 0.25s ease, box-shadow 0.25s ease',
        '&:hover': {
          transform: 'translateY(-4px)',
          boxShadow: 8,
        },
        '&:hover .product-image': {
          transform: 'scale(1.06)',
        },
      }}
    >
      <CardActionArea component={Link} to={`/products/${id}`}>
        <Box
          sx={{
            position: 'relative',
            height: 220,
            bgcolor: 'action.hover',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            overflow: 'hidden',
          }}
        >
          {product.imageUrl ? (
            <Box
              component="img"
              className="product-image"
              src={product.imageUrl}
              alt={product.name}
              sx={{
                maxWidth: '100%',
                maxHeight: '100%',
                objectFit: 'contain',
                p: 1,
                transition: 'transform 0.4s ease',
              }}
            />
          ) : (
            <ImageNotSupportedIcon sx={{ fontSize: 64, color: 'text.disabled' }} />
          )}
          {isOutOfStock && (
            <Chip
              label="Esgotado"
              size="small"
              color="error"
              sx={{ position: 'absolute', top: 8, right: 8, fontWeight: 600 }}
            />
          )}
          {isLowStock && (
            <Chip
              label={`Restam ${stock}`}
              size="small"
              color="warning"
              sx={{ position: 'absolute', top: 8, right: 8, fontWeight: 600 }}
            />
          )}
        </Box>
        <CardContent sx={{ pb: 1 }}>
          <Typography
            variant="body1"
            sx={{ fontWeight: 500, minHeight: 48 }}
            noWrap
          >
            {product.name}
          </Typography>
          <Typography variant="h6" color="primary" sx={{ mt: 1, fontWeight: 700 }}>
            {fmt(price)}
          </Typography>
        </CardContent>
      </CardActionArea>
      <Box sx={{ flex: 1 }} />
      <CardActions sx={{ p: 2, pt: 0 }}>
        <Button
          fullWidth
          variant="contained"
          disabled={isOutOfStock}
          onClick={() => {
            addItem(product);
            notify('Produto adicionado ao carrinho', 'success');
          }}
        >
          {isOutOfStock ? 'Indisponível' : 'Adicionar ao carrinho'}
        </Button>
      </CardActions>
    </Card>
  );
}
