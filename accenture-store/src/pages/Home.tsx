import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Container, Grid, Skeleton, Alert, Typography, Box, Pagination, Stack } from '@mui/material';
import ProductCard from '../components/ProductCard';
import { listProducts } from '../api/products';
import type { Paginated, Product } from '../api/types';

type ProductsResponse = Paginated<Product> | Product[] | null;

export default function Home() {
  const [params] = useSearchParams();
  const q = params.get('q') || '';
  const [page, setPage] = useState(0);
  const [data, setData] = useState<ProductsResponse>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancel = false;
    setLoading(true); setError(null);
    listProducts({ page, size: 12, q })
      .then((res) => { if (!cancel) setData(res); })
      .catch((err: Error) => { if (!cancel) setError(err.message); })
      .finally(() => { if (!cancel) setLoading(false); });
    return () => { cancel = true; };
  }, [page, q]);

  const items: Product[] = Array.isArray(data)
    ? data
    : data?.content || data?.items || [];
  const totalPages = (data && !Array.isArray(data) && data.totalPages) || 1;

  return (
    <Container maxWidth="xl" sx={{ py: 3 }}>
      {q && <Typography variant="h6" sx={{ mb: 2 }}>Resultados para “{q}”</Typography>}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Não foi possível carregar os produtos: {error}.
        </Alert>
      )}
      <Grid container spacing={2}>
        {loading
          ? Array.from({ length: 8 }).map((_, i) => (
              <Grid key={i} item xs={12} sm={6} md={4} lg={3}>
                <Skeleton variant="rectangular" height={320} />
              </Grid>
            ))
          : items.length === 0 && !error
            ? <Box sx={{ p: 4 }}><Typography color="text.secondary">Nenhum produto encontrado.</Typography></Box>
            : items.map((p) => (
                <Grid key={p.id || p.sku} item xs={12} sm={6} md={4} lg={3}>
                  <ProductCard product={p} />
                </Grid>
              ))}
      </Grid>
      {totalPages > 1 && (
        <Stack alignItems="center" sx={{ mt: 4 }}>
          <Pagination count={totalPages} page={page + 1} onChange={(_, v) => setPage(v - 1)} color="primary" />
        </Stack>
      )}
    </Container>
  );
}
