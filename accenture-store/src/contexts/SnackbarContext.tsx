import { createContext, useCallback, useContext, useState, type ReactNode, type SyntheticEvent } from 'react';
import { Snackbar, Alert, type AlertColor, type SnackbarCloseReason } from '@mui/material';

interface SnackbarContextValue {
  notify: (message: string, severity?: AlertColor) => void;
}

interface SnackbarState {
  open: boolean;
  message: string;
  severity: AlertColor;
}

const SnackbarContext = createContext<SnackbarContextValue>({ notify: () => {} });

export function SnackbarProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<SnackbarState>({ open: false, message: '', severity: 'info' });

  const notify = useCallback((message: string, severity: AlertColor = 'info') => {
    setState({ open: true, message, severity });
  }, []);

  const handleClose = (_event: Event | SyntheticEvent, reason?: SnackbarCloseReason) => {
    if (reason === 'clickaway') return;
    setState((s) => ({ ...s, open: false }));
  };

  return (
    <SnackbarContext.Provider value={{ notify }}>
      {children}
      <Snackbar
        open={state.open}
        autoHideDuration={4000}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={() => setState((s) => ({ ...s, open: false }))} severity={state.severity} variant="filled" sx={{ width: '100%' }}>
          {state.message}
        </Alert>
      </Snackbar>
    </SnackbarContext.Provider>
  );
}

export const useSnackbar = () => useContext(SnackbarContext);
