import {
  AppBar, Toolbar, Box, InputBase, IconButton, Badge, Button, Menu, MenuItem, Typography, Paper,
  Avatar, Divider, ListItemIcon, Stack,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import CloseIcon from '@mui/icons-material/Close';
import PersonOutlineIcon from '@mui/icons-material/PersonOutline';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import LocationOnOutlinedIcon from '@mui/icons-material/LocationOnOutlined';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import LogoutIcon from '@mui/icons-material/Logout';
import { useState, type FormEvent, type MouseEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useCart } from '../contexts/CartContext';
import ThemeToggleButton from './ThemeToggleButton';
import WalletBadge from './WalletBadge';

const getInitials = (name?: string): string => {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0][0].toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
};

export default function Header() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const { totalItems } = useCart();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [q, setQ] = useState(params.get('q') || '');
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const [mobileSearchOpen, setMobileSearchOpen] = useState(false);

  const submitSearch = (e: FormEvent) => {
    e.preventDefault();
    navigate(`/?q=${encodeURIComponent(q)}`);
    setMobileSearchOpen(false);
  };

  const handleLogout = () => {
    logout();
    setAnchor(null);
    navigate('/login', { replace: true, state: null });
  };

  const closeMenu = () => setAnchor(null);
  const firstName = user?.name?.trim().split(/\s+/)[0] || 'Você';
  const initials = getInitials(user?.name);

  return (
    <AppBar position="sticky" color="primary" elevation={2}>
      <Toolbar sx={{ gap: { xs: 0.5, sm: 1, md: 2 }, px: { xs: 1, sm: 2 } }}>
        {!mobileSearchOpen && (
          <Box
            component={Link}
            to={isAdmin ? '/admin' : '/'}
            sx={{ display: 'flex', alignItems: 'center', textDecoration: 'none', color: 'inherit', flexShrink: 0 }}
          >
            <Box
              component="img"
              src="/logo.svg"
              alt="acce store"
              sx={{ height: { xs: 28, sm: 32 }, mr: { xs: 0.5, sm: 1 }, display: 'block' }}
              onError={(e) => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
            />
            <Typography
              variant="h6"
              sx={{ fontWeight: 700, letterSpacing: 0.5, display: { xs: 'none', sm: 'block' } }}
            >
              acce<Box component="span" sx={{ color: '#fff' }}>{'>'}</Box>store
            </Typography>
          </Box>
        )}

        {!isAdmin && (
          <Paper
            component="form"
            onSubmit={submitSearch}
            sx={{
              flex: 1,
              alignItems: 'center',
              px: 1,
              maxWidth: 800,
              display: { xs: mobileSearchOpen ? 'flex' : 'none', md: 'flex' },
            }}
          >
            <InputBase
              sx={{ flex: 1, px: 1 }}
              placeholder="Buscar produtos…"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              inputProps={{ 'aria-label': 'buscar produtos' }}
              autoFocus={mobileSearchOpen}
            />
            <IconButton type="submit" aria-label="buscar" sx={{ color: 'primary.main' }}>
              <SearchIcon />
            </IconButton>
            {mobileSearchOpen && (
              <IconButton
                onClick={() => setMobileSearchOpen(false)}
                aria-label="fechar busca"
                sx={{ display: { md: 'none' }, color: 'primary.main' }}
              >
                <CloseIcon />
              </IconButton>
            )}
          </Paper>
        )}

        {!mobileSearchOpen && <Box sx={{ flex: 1 }} />}

        {!mobileSearchOpen && !isAdmin && (
          <IconButton
            color="inherit"
            onClick={() => setMobileSearchOpen(true)}
            aria-label="abrir busca"
            sx={{ display: { xs: 'inline-flex', md: 'none' } }}
          >
            <SearchIcon />
          </IconButton>
        )}

        {!mobileSearchOpen && (
          <>
            <ThemeToggleButton />

            <WalletBadge />

            {!isAdmin && (
              <IconButton color="inherit" component={Link} to="/cart" aria-label="carrinho">
                <Badge
                  badgeContent={totalItems}
                  color="secondary"
                  sx={{
                    '& .MuiBadge-badge': {
                      transition: 'transform 0.2s ease',
                    },
                  }}
                >
                  <ShoppingCartIcon />
                </Badge>
              </IconButton>
            )}

            {isAuthenticated ? (
              <>
                <Button
                  color="inherit"
                  onClick={(e: MouseEvent<HTMLButtonElement>) => setAnchor(e.currentTarget)}
                  sx={{ display: { xs: 'none', sm: 'inline-flex' }, minWidth: 0, textTransform: 'none' }}
                  startIcon={
                    <Avatar
                      sx={{
                        width: 28,
                        height: 28,
                        bgcolor: 'secondary.main',
                        color: 'primary.contrastText',
                        fontSize: '0.8rem',
                        fontWeight: 700,
                      }}
                    >
                      {initials}
                    </Avatar>
                  }
                >
                  Olá, {firstName}
                </Button>
                <IconButton
                  color="inherit"
                  onClick={(e) => setAnchor(e.currentTarget)}
                  aria-label="menu do usuário"
                  sx={{ display: { xs: 'inline-flex', sm: 'none' }, p: 0.5 }}
                >
                  <Avatar
                    sx={{
                      width: 30,
                      height: 30,
                      bgcolor: 'secondary.main',
                      color: 'primary.contrastText',
                      fontSize: '0.8rem',
                      fontWeight: 700,
                    }}
                  >
                    {initials}
                  </Avatar>
                </IconButton>
                <Menu
                  anchorEl={anchor}
                  open={!!anchor}
                  onClose={closeMenu}
                  PaperProps={{
                    sx: {
                      mt: 1,
                      minWidth: 260,
                      borderRadius: 2,
                      overflow: 'visible',
                    },
                  }}
                  transformOrigin={{ horizontal: 'right', vertical: 'top' }}
                  anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
                >
                  <Box sx={{ px: 2, py: 1.5 }}>
                    <Stack direction="row" spacing={1.5} alignItems="center">
                      <Avatar
                        sx={{
                          width: 44,
                          height: 44,
                          bgcolor: 'primary.main',
                          color: 'primary.contrastText',
                          fontWeight: 700,
                        }}
                      >
                        {initials}
                      </Avatar>
                      <Box sx={{ minWidth: 0 }}>
                        <Typography variant="subtitle2" sx={{ fontWeight: 700 }} noWrap>
                          {user?.name || 'Usuário'}
                        </Typography>
                        <Typography variant="caption" color="text.secondary" noWrap sx={{ display: 'block' }}>
                          {user?.email || ''}
                        </Typography>
                      </Box>
                    </Stack>
                  </Box>
                  <Divider />
                  {!isAdmin && (
                    <MenuItem component={Link} to="/profile" onClick={closeMenu}>
                      <ListItemIcon><PersonOutlineIcon fontSize="small" /></ListItemIcon>
                      Meu perfil
                    </MenuItem>
                  )}
                  {!isAdmin && (
                    <MenuItem component={Link} to="/orders" onClick={closeMenu}>
                      <ListItemIcon><ReceiptLongIcon fontSize="small" /></ListItemIcon>
                      Meus pedidos
                    </MenuItem>
                  )}
                  {!isAdmin && (
                    <MenuItem component={Link} to="/wallet" onClick={closeMenu}>
                      <ListItemIcon><AccountBalanceWalletIcon fontSize="small" /></ListItemIcon>
                      Carteira
                    </MenuItem>
                  )}
                  {!isAdmin && (
                    <MenuItem component={Link} to="/addresses" onClick={closeMenu}>
                      <ListItemIcon><LocationOnOutlinedIcon fontSize="small" /></ListItemIcon>
                      Endereços
                    </MenuItem>
                  )}
                  {isAdmin && (
                    <MenuItem component={Link} to="/admin" onClick={closeMenu}>
                      <ListItemIcon><AdminPanelSettingsIcon fontSize="small" /></ListItemIcon>
                      Painel admin
                    </MenuItem>
                  )}
                  <Divider />
                  <MenuItem onClick={handleLogout}>
                    <ListItemIcon><LogoutIcon fontSize="small" color="error" /></ListItemIcon>
                    <Typography color="error">Sair</Typography>
                  </MenuItem>
                </Menu>
              </>
            ) : (
              <Button color="inherit" component={Link} to="/login" startIcon={<AccountCircleIcon />}>
                Entrar
              </Button>
            )}
          </>
        )}
      </Toolbar>
    </AppBar>
  );
}
