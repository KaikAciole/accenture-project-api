import { AppBar, Toolbar, Box, Button, Typography } from '@mui/material';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import { Link } from 'react-router-dom';
import ThemeToggleButton from './ThemeToggleButton';

export default function MinimalHeader() {
  return (
    <AppBar position="sticky" color="primary" elevation={2}>
      <Toolbar sx={{ gap: { xs: 0.5, sm: 1 }, px: { xs: 1, sm: 2 } }}>
        <Box
          component={Link}
          to="/"
          sx={{ display: 'flex', alignItems: 'center', textDecoration: 'none', color: 'inherit', flexShrink: 0 }}
        >
          <Box
            component="img"
            src="/logo.svg"
            alt="acce store"
            sx={{ height: { xs: 28, sm: 32 }, mr: { xs: 0.5, sm: 1 }, display: 'block' }}
            onError={(e) => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
          />
          <Typography variant="h6" sx={{ fontWeight: 700, letterSpacing: 0.5 }}>
            acce<Box component="span" sx={{ color: '#fff' }}>{'>'}</Box>store
          </Typography>
        </Box>
        <Box sx={{ flex: 1 }} />
        <ThemeToggleButton />
        <Button color="inherit" component={Link} to="/login" startIcon={<AccountCircleIcon />}>
          Entrar
        </Button>
      </Toolbar>
    </AppBar>
  );
}
