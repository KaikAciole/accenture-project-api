import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Box, Skeleton, Tooltip, Typography } from '@mui/material';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import { useAuth } from '../contexts/AuthContext';
import { getWalletByOwner } from '../api/wallets';

const fmt = (v: number | undefined) =>
  Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

export default function WalletBadge() {
  const { user, isAuthenticated, isAdmin } = useAuth();
  const [balance, setBalance] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isAuthenticated || isAdmin || !user?.customerId) {
      setLoading(false);
      setBalance(null);
      return;
    }
    let cancelled = false;
    setLoading(true);
    getWalletByOwner(user.customerId, 'CUSTOMER')
      .then((wallet) => {
        if (cancelled) return;
        setBalance(wallet.balance ?? 0);
      })
      .catch(() => {
        if (cancelled) return;
        setBalance(null);
      })
      .finally(() => {
        if (cancelled) return;
        setLoading(false);
      });
    const interval = setInterval(() => {
      if (!user?.customerId) return;
      getWalletByOwner(user.customerId, 'CUSTOMER')
        .then((wallet) => {
          if (!cancelled) setBalance(wallet.balance ?? 0);
        })
        .catch(() => { /* mantém saldo anterior */ });
    }, 15000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [user?.customerId, isAuthenticated, isAdmin]);

  if (!isAuthenticated || isAdmin) return null;
  if (loading) {
    return (
      <Skeleton
        variant="rounded"
        width={92}
        height={32}
        sx={{ display: { xs: 'none', sm: 'block' }, bgcolor: 'rgba(255,255,255,0.2)' }}
      />
    );
  }
  if (balance === null) return null;

  return (
    <Tooltip title={`Saldo: ${fmt(balance)}`}>
      <Box
        component={Link}
        to="/wallet"
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 0.75,
          px: { xs: 1, sm: 1.5 },
          py: 0.5,
          borderRadius: 2,
          bgcolor: 'rgba(255,255,255,0.12)',
          color: 'inherit',
          textDecoration: 'none',
          transition: 'background-color 0.2s ease, transform 0.2s ease',
          border: '1px solid rgba(255,255,255,0.15)',
          '&:hover': {
            bgcolor: 'rgba(255,255,255,0.22)',
            transform: 'translateY(-1px)',
          },
        }}
      >
        <AccountBalanceWalletIcon fontSize="small" />
        <Typography
          variant="body2"
          sx={{ fontWeight: 600, display: { xs: 'none', sm: 'inline' } }}
        >
          {fmt(balance)}
        </Typography>
      </Box>
    </Tooltip>
  );
}
