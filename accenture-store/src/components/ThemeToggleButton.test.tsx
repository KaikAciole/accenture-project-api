import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeModeProvider, useThemeMode } from '../contexts/ThemeModeContext';
import ThemeToggleButton from './ThemeToggleButton';

function Probe() {
  const { mode } = useThemeMode();
  return <span data-testid="mode">{mode}</span>;
}

describe('ThemeToggleButton', () => {
  it('renderiza ícone de lua no modo light', () => {
    localStorage.setItem('themeMode', 'light');
    render(
      <ThemeModeProvider>
        <ThemeToggleButton />
      </ThemeModeProvider>,
    );
    const btn = screen.getByRole('button', { name: /alternar tema/i });
    expect(btn).toBeInTheDocument();
  });

  it('alterna o modo ao clicar', async () => {
    const user = userEvent.setup();
    localStorage.setItem('themeMode', 'light');
    render(
      <ThemeModeProvider>
        <ThemeToggleButton />
        <Probe />
      </ThemeModeProvider>,
    );

    expect(screen.getByTestId('mode')).toHaveTextContent('light');
    await user.click(screen.getByRole('button', { name: /alternar tema/i }));
    expect(screen.getByTestId('mode')).toHaveTextContent('dark');
  });
});
