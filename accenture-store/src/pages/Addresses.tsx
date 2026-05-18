import { useEffect, useState } from 'react';
import {
  Container, Typography, Paper, Stack, Button, Skeleton, Alert, Grid, Dialog, DialogTitle,
  DialogContent, DialogActions, IconButton, Box, Avatar, Divider,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import HomeOutlinedIcon from '@mui/icons-material/HomeOutlined';
import { useAuth } from '../contexts/AuthContext';
import { useSnackbar } from '../contexts/SnackbarContext';
import {
  listAddresses, createAddress, updateAddress, deleteAddress,
} from '../api/customers';
import AddressForm from '../components/AddressForm';
import type { Address } from '../api/types';

export default function Addresses() {
  const { user } = useAuth();
  const { notify } = useSnackbar();
  const [items, setItems] = useState<Address[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Address | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Address | null>(null);
  const [deleting, setDeleting] = useState(false);

  const reload = () => {
    if (!user?.customerId) return;
    setItems(null);
    listAddresses(user.customerId)
      .then((data) => setItems(Array.isArray(data) ? data : data?.content || []))
      .catch((e: Error) => setError(e.message));
  };
  useEffect(reload, [user?.customerId]);

  const openCreate = () => { setEditing(null); setFormOpen(true); };
  const openEdit = (a: Address) => { setEditing(a); setFormOpen(true); };

  const submitAddress = async (address: Address) => {
    if (!user?.customerId) return;
    try {
      if (editing?.id) {
        await updateAddress(user.customerId, editing.id, address);
        notify('Endereço atualizado', 'success');
      } else {
        await createAddress(user.customerId, address);
        notify('Endereço cadastrado', 'success');
      }
      setFormOpen(false);
      setEditing(null);
      reload();
    } catch (e) {
      notify(e instanceof Error ? e.message : 'Erro ao salvar endereço', 'error');
    }
  };

  const handleDelete = async () => {
    if (!user?.customerId || !confirmDelete?.id) return;
    setDeleting(true);
    try {
      await deleteAddress(user.customerId, confirmDelete.id);
      notify('Endereço excluído', 'success');
      setConfirmDelete(null);
      reload();
    } catch (e) {
      notify(e instanceof Error ? e.message : 'Erro ao excluir', 'error');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 3 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Stack direction="row" spacing={1.5} alignItems="center">
          <LocationOnIcon color="primary" />
          <Typography variant="h5">Endereços</Typography>
        </Stack>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
          Adicionar endereço
        </Button>
      </Stack>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {items === null ? (
        <Skeleton variant="rectangular" height={150} />
      ) : items.length === 0 ? (
        <Paper sx={{ p: { xs: 4, sm: 6 }, textAlign: 'center' }}>
          <HomeOutlinedIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 1.5 }} />
          <Typography variant="h6" gutterBottom>Nenhum endereço cadastrado</Typography>
          <Typography color="text.secondary" sx={{ mb: 3 }}>
            Cadastre um endereço para agilizar suas próximas compras.
          </Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Adicionar endereço
          </Button>
        </Paper>
      ) : (
        <Grid container spacing={2}>
          {items.map((a) => (
            <Grid key={a.id} item xs={12} md={6}>
              <Paper
                sx={{
                  p: 2.5,
                  height: '100%',
                  transition: 'transform 0.2s ease, box-shadow 0.2s ease',
                  borderLeft: 4,
                  borderColor: 'primary.main',
                  '&:hover': {
                    transform: 'translateY(-2px)',
                    boxShadow: 4,
                  },
                }}
              >
                <Stack direction="row" spacing={2} alignItems="flex-start">
                  <Avatar sx={{ bgcolor: 'primary.main', color: 'primary.contrastText' }}>
                    <LocationOnIcon />
                  </Avatar>
                  <Stack sx={{ flex: 1, minWidth: 0 }} spacing={0.5}>
                    <Typography variant="subtitle1" sx={{ fontWeight: 600, wordBreak: 'break-word' }}>
                      {a.street}, {a.number}
                    </Typography>
                    {a.complement && (
                      <Typography variant="body2" color="text.secondary" sx={{ wordBreak: 'break-word' }}>
                        {a.complement}
                      </Typography>
                    )}
                    <Typography variant="body2" color="text.secondary" sx={{ wordBreak: 'break-word' }}>
                      {a.neighborhood || a.district} — {a.city}/{a.state}
                    </Typography>
                    <Box
                      component="span"
                      sx={{
                        display: 'inline-block',
                        fontFamily: 'monospace',
                        fontSize: '0.8rem',
                        bgcolor: 'action.hover',
                        px: 1,
                        py: 0.25,
                        borderRadius: 1,
                        mt: 0.5,
                        alignSelf: 'flex-start',
                      }}
                    >
                      CEP {a.zipCode}
                    </Box>
                  </Stack>
                </Stack>
                <Divider sx={{ my: 1.5 }} />
                <Stack direction="row" justifyContent="flex-end" spacing={0.5}>
                  <IconButton
                    onClick={() => openEdit(a)}
                    aria-label="editar"
                    size="small"
                    color="primary"
                  >
                    <EditIcon fontSize="small" />
                  </IconButton>
                  <IconButton
                    onClick={() => setConfirmDelete(a)}
                    aria-label="excluir"
                    size="small"
                    color="error"
                  >
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Stack>
              </Paper>
            </Grid>
          ))}
        </Grid>
      )}

      <Dialog open={formOpen} onClose={() => setFormOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editing ? 'Editar endereço' : 'Novo endereço'}</DialogTitle>
        <DialogContent>
          <AddressForm
            initial={editing || undefined}
            onCancel={() => setFormOpen(false)}
            onSubmit={submitAddress}
          />
        </DialogContent>
      </Dialog>

      <Dialog open={!!confirmDelete} onClose={() => setConfirmDelete(null)}>
        <DialogTitle>Excluir endereço</DialogTitle>
        <DialogContent>
          <Typography>Tem certeza que deseja excluir este endereço?</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmDelete(null)}>Cancelar</Button>
          <Button color="error" variant="contained" onClick={handleDelete} disabled={deleting}>
            {deleting ? 'Excluindo…' : 'Excluir'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}
