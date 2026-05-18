import { createTheme, type Theme } from '@mui/material/styles';
import type { ThemeMode } from '../contexts/ThemeModeContext';

const ACCENT = '#A100FF';

export function buildTheme(mode: ThemeMode): Theme {
  return createTheme({
    palette: {
      mode,
      primary: { main: ACCENT },
      secondary: { main: mode === 'dark' ? '#B266FF' : '#7A00CC' },
    },
    shape: { borderRadius: 8 },
    typography: {
      fontFamily: 'Roboto, system-ui, -apple-system, sans-serif',
      h5: { fontWeight: 600 },
      h6: { fontWeight: 600 },
    },
  });
}
