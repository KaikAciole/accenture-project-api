import { IconButton, Tooltip } from '@mui/material';
import Brightness4Icon from '@mui/icons-material/Brightness4';
import Brightness7Icon from '@mui/icons-material/Brightness7';
import { useThemeMode } from '../contexts/ThemeModeContext';

export default function ThemeToggleButton() {
  const { mode, toggleMode } = useThemeMode();
  return (
    <Tooltip title={mode === 'dark' ? 'Modo claro' : 'Modo escuro'}>
      <IconButton color="inherit" onClick={toggleMode} aria-label="alternar tema">
        {mode === 'dark' ? <Brightness7Icon /> : <Brightness4Icon />}
      </IconButton>
    </Tooltip>
  );
}
