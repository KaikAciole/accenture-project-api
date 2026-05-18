import { Box, Container, Stack, Button } from '@mui/material';
import { Link } from 'react-router-dom';

const links: { to: string; label: string }[] = [
  { to: '/', label: 'Todos os produtos' },
  { to: '/orders', label: 'Meus pedidos' },
  { to: '/addresses', label: 'Endereços' },
  { to: '/wallet', label: 'Carteira' },
];

export default function SecondaryNav() {
  return (
    <Box sx={{ bgcolor: 'background.paper', borderBottom: 1, borderColor: 'divider' }}>
      <Container maxWidth="xl">
        <Stack direction="row" spacing={1} sx={{ py: 0.5, overflowX: 'auto' }}>
          {links.map((l) => (
            <Button
              key={l.to}
              component={Link}
              to={l.to}
              size="small"
              color="inherit"
              sx={{ textTransform: 'none', whiteSpace: 'nowrap' }}
            >
              {l.label}
            </Button>
          ))}
        </Stack>
      </Container>
    </Box>
  );
}
