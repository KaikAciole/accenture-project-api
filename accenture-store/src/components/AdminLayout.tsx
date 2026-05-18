import { type ReactNode } from 'react';
import { Box, Drawer, List, ListItemButton, ListItemIcon, ListItemText, Toolbar, Container } from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import Inventory2Icon from '@mui/icons-material/Inventory2';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import PeopleIcon from '@mui/icons-material/People';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import { NavLink } from 'react-router-dom';

const DRAWER_WIDTH = 240;

const NAV: { to: string; label: string; icon: ReactNode; end?: boolean }[] = [
  { to: '/admin', label: 'Dashboard', icon: <DashboardIcon />, end: true },
  { to: '/admin/products', label: 'Produtos', icon: <Inventory2Icon /> },
  { to: '/admin/orders', label: 'Pedidos', icon: <ReceiptLongIcon /> },
  { to: '/admin/customers', label: 'Clientes', icon: <PeopleIcon /> },
  { to: '/admin/company-wallet', label: 'Carteira da empresa', icon: <AccountBalanceWalletIcon /> },
];

interface AdminLayoutProps {
  children: ReactNode;
}

export default function AdminLayout({ children }: AdminLayoutProps) {
  return (
    <Box sx={{ display: 'flex', flex: 1 }}>
      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          display: { xs: 'none', md: 'block' },
          [`& .MuiDrawer-paper`]: {
            width: DRAWER_WIDTH,
            boxSizing: 'border-box',
            position: 'relative',
          },
        }}
      >
        <Toolbar variant="dense" />
        <List>
          {NAV.map((n) => (
            <ListItemButton
              key={n.to}
              component={NavLink}
              to={n.to}
              end={n.end}
              sx={{
                '&.active': {
                  bgcolor: 'action.selected',
                  borderLeft: 3,
                  borderColor: 'primary.main',
                },
              }}
            >
              <ListItemIcon>{n.icon}</ListItemIcon>
              <ListItemText primary={n.label} />
            </ListItemButton>
          ))}
        </List>
      </Drawer>
      <Box component="section" sx={{ flex: 1, minWidth: 0 }}>
        <Container maxWidth="xl" sx={{ py: 3 }}>
          {children}
        </Container>
      </Box>
    </Box>
  );
}
