import { describe, it, expect } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SnackbarProvider, useSnackbar } from './SnackbarContext';

function Trigger({ msg, severity }: { msg: string; severity?: 'info' | 'success' | 'error' | 'warning' }) {
  const { notify } = useSnackbar();
  return <button onClick={() => notify(msg, severity)}>open</button>;
}

describe('SnackbarContext', () => {
  it('notify abre snackbar com mensagem e severidade padrão (info)', async () => {
    const user = userEvent.setup();
    render(
      <SnackbarProvider>
        <Trigger msg="Olá" />
      </SnackbarProvider>,
    );
    await user.click(screen.getByRole('button', { name: 'open' }));
    expect(await screen.findByText('Olá')).toBeInTheDocument();
  });

  it('notify aceita severity customizada', async () => {
    const user = userEvent.setup();
    render(
      <SnackbarProvider>
        <Trigger msg="erro!" severity="error" />
      </SnackbarProvider>,
    );
    await user.click(screen.getByRole('button', { name: 'open' }));
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('erro!');
  });

  it('botão close fecha o snackbar', async () => {
    const user = userEvent.setup();
    render(
      <SnackbarProvider>
        <Trigger msg="fechar" />
      </SnackbarProvider>,
    );
    await user.click(screen.getByRole('button', { name: 'open' }));
    expect(await screen.findByText('fechar')).toBeInTheDocument();

    const closeButton = screen.getByRole('button', { name: /close/i });
    await user.click(closeButton);

    await act(async () => { await new Promise((r) => setTimeout(r, 200)); });
  });

  it('default context (sem provider) não quebra ao chamar notify', () => {
    function Probe() {
      const { notify } = useSnackbar();
      notify('x');
      return <div>ok</div>;
    }
    render(<Probe />);
    expect(screen.getByText('ok')).toBeInTheDocument();
  });
});
