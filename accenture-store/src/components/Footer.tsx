import { Box, Container, Typography } from '@mui/material';

export default function Footer() {
  return (
    <Box component="footer" sx={{ bgcolor: 'background.paper', borderTop: 1, borderColor: 'divider', mt: 6, py: 4 }}>
      <Container maxWidth="xl">
        <Typography variant="body2" color="text.secondary" align="left">
          © {new Date().getFullYear()} Acce Store
        </Typography>
      </Container>
    </Box>
  );
}
