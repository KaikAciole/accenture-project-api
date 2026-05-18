import { useEffect, useRef, useState } from 'react';
import {
  Container, Typography, Paper, Skeleton, Alert, Table, TableHead, TableRow, TableCell, TableBody,
  TableContainer, Chip, Stack, Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  CircularProgress, Box,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { useAuth } from '../contexts/AuthContext';
import { useSnackbar } from '../contexts/SnackbarContext';
import {
  createTopUp, getWalletByOwner, listTransactionsByOwner, submitTopUp,
} from '../api/wallets';
import type { Wallet as WalletType, WalletTransaction } from '../api/types';

const fmt = (v: number | undefined) =>
  Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

const REASON_LABEL: Record<string, string> = {
  TOP_UP: 'Recarga',
  PAYMENT: 'Pagamento',
  SALE_RECEIVED: 'Venda',
  REFUND: 'Estorno',
  CANCELLATION: 'Cancelamento',
};

export default function Wallet() {
  const { user } = useAuth();
  const { notify } = useSnackbar();
  const [wallet, setWallet] = useState<WalletType | null>(null);
  const [txs, setTxs] = useState<WalletTransaction[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [topUpOpen, setTopUpOpen] = useState(false);
  const [amount, setAmount] = useState('');
  const [generating, setGenerating] = useState(false);
  const [pixQrCode, setPixQrCode] = useState<string | null>(null);
  const [pixQrCodeBase64, setPixQrCodeBase64] = useState<string | null>(null);
  const inFlightRef = useRef(false);
  const balanceBeforeTopUpRef = useRef<number | null>(null);

  const loadWallet = () => {
    if (!user?.customerId) return;
    getWalletByOwner(user.customerId, 'CUSTOMER')
      .then(setWallet)
      .catch((e: Error) => setError(e.message));
    listTransactionsByOwner(user.customerId, 'CUSTOMER')
      .then((data) => setTxs(Array.isArray(data) ? data : data?.content || []))
      .catch(() => setTxs([]));
  };

  useEffect(loadWallet, [user?.customerId]);

  useEffect(() => {
    if (!pixQrCode) return;
    const interval = setInterval(loadWallet, 3000);
    return () => clearInterval(interval);
  }, [pixQrCode, user?.customerId]);

  useEffect(() => {
    if (!pixQrCode) return;
    const before = balanceBeforeTopUpRef.current;
    if (before === null || !wallet) return;
    if (wallet.balance > before) {
      notify('Pagamento confirmado! Saldo atualizado.', 'success');
      closeTopUp();
    }
  }, [wallet?.balance, pixQrCode]);

  const generateQr = async () => {
    if (inFlightRef.current) return;
    if (!wallet?.id || !user?.customerId || !user.email) {
      notify('Não foi possível identificar a carteira', 'error');
      return;
    }
    const value = Number(amount.replace(',', '.'));
    if (!value || value <= 0) {
      notify('Informe um valor maior que zero', 'warning');
      return;
    }
    inFlightRef.current = true;
    setGenerating(true);
    try {
      const topUp = await createTopUp(wallet.id, {
        customerId: user.customerId,
        amount: value,
        customerEmail: user.email,
      });
      const submission = await submitTopUp(topUp.id);
      balanceBeforeTopUpRef.current = wallet.balance;
      setPixQrCode(submission.qrCode);
      setPixQrCodeBase64(submission.qrCodeBase64);
    } catch (e) {
      notify(e instanceof Error ? e.message : 'Falha ao gerar QR Code Pix', 'error');
    } finally {
      setGenerating(false);
      inFlightRef.current = false;
    }
  };

  const copyPixCode = async () => {
    if (!pixQrCode) return;
    try {
      await navigator.clipboard.writeText(pixQrCode);
      notify('Codigo Pix copiado', 'success');
    } catch {
      notify('Nao foi possivel copiar o codigo', 'error');
    }
  };

  const closeTopUp = () => {
    setTopUpOpen(false);
    setAmount('');
    setPixQrCode(null);
    setPixQrCodeBase64(null);
    balanceBeforeTopUpRef.current = null;
  };

  if (error) return <Container sx={{ py: 4 }}><Alert severity="error">{error}</Alert></Container>;
  if (!wallet) return <Container sx={{ py: 4 }}><Skeleton variant="rectangular" height={200} /></Container>;

  return (
    <Container maxWidth="md" sx={{ py: 3 }}>
      <Paper sx={{ p: { xs: 3, sm: 4 }, mb: 2, textAlign: 'center' }}>
        <Typography variant="overline" color="text.secondary">Saldo da carteira</Typography>
        <Typography
          variant="h2"
          color="primary"
          sx={{ fontSize: { xs: '2.5rem', sm: '3.75rem' }, wordBreak: 'break-word' }}
        >
          {fmt(wallet.balance)}
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setTopUpOpen(true)}
          sx={{ mt: 2 }}
        >
          Recarregar saldo
        </Button>
      </Paper>

      <TableContainer component={Paper} sx={{ overflowX: 'auto' }}>
        <Typography variant="subtitle1" sx={{ p: 2 }}>Transações</Typography>
        {txs.length === 0 ? (
          <Typography sx={{ p: 2 }} color="text.secondary">Sem transações.</Typography>
        ) : (
          <Table sx={{ minWidth: 500 }}>
            <TableHead>
              <TableRow>
                <TableCell>Data</TableCell>
                <TableCell>Tipo</TableCell>
                <TableCell>Motivo</TableCell>
                <TableCell align="right">Valor</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {txs.map((t) => (
                <TableRow key={t.id}>
                  <TableCell>{t.createdAt ? new Date(t.createdAt).toLocaleString('pt-BR') : ''}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={t.type === 'CREDIT' ? 'Crédito' : 'Débito'}
                      color={t.type === 'CREDIT' ? 'success' : 'default'}
                    />
                  </TableCell>
                  <TableCell>{(t.reason && REASON_LABEL[t.reason]) || t.description || t.reason}</TableCell>
                  <TableCell align="right" sx={{ color: t.type === 'CREDIT' ? 'success.main' : 'text.primary' }}>
                    {t.type === 'CREDIT' ? '+' : '−'} {fmt(Math.abs(t.amount))}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </TableContainer>

      <Dialog open={topUpOpen} onClose={closeTopUp} maxWidth="sm" fullWidth>
        <DialogTitle>Recarregar carteira</DialogTitle>
        <DialogContent>
          {!pixQrCode ? (
            <Stack spacing={2} sx={{ mt: 1 }}>
              <TextField
                label="Valor (R$)"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                fullWidth
                inputProps={{ inputMode: 'decimal' }}
                placeholder="100,00"
                autoFocus
                disabled={generating}
              />
              <Typography variant="caption" color="text.secondary">
                Saldo atual: {fmt(wallet.balance)}
              </Typography>
            </Stack>
          ) : (
            <Stack spacing={2} sx={{ mt: 1, textAlign: 'center' }}>
              <Alert severity="info">
                Escaneie o QR Code no app do seu banco ou copie o código Pix abaixo. O saldo será atualizado automaticamente após a confirmação.
              </Alert>
              {pixQrCodeBase64 && (
                <Box
                  component="img"
                  src={pixQrCodeBase64.startsWith('data:')
                    ? pixQrCodeBase64
                    : `data:image/png;base64,${pixQrCodeBase64}`}
                  alt="QR Code Pix"
                  sx={{ width: 256, height: 256, alignSelf: 'center' }}
                />
              )}
              <TextField
                label="Pix Copia e Cola"
                value={pixQrCode}
                fullWidth
                multiline
                minRows={3}
                InputProps={{ readOnly: true }}
              />
              <Button
                variant="outlined"
                startIcon={<ContentCopyIcon />}
                onClick={copyPixCode}
              >
                Copiar código Pix
              </Button>
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={closeTopUp}>Fechar</Button>
          {!pixQrCode && (
            <Button variant="contained" onClick={generateQr} disabled={generating}>
              {generating ? (
                <>
                  <CircularProgress size={18} sx={{ mr: 1 }} />
                  Gerando…
                </>
              ) : 'Gerar QR Code'}
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Container>
  );
}
