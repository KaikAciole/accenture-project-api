import { describe, it, expect, beforeEach, vi } from 'vitest';
import userEvent from '@testing-library/user-event';
import { renderWithProviders, screen, waitFor } from '../test/utils';
import AddressForm from './AddressForm';
import { ApiError } from '../api/client';

vi.mock('../api/cep', () => ({ lookupCep: vi.fn() }));
import { lookupCep } from '../api/cep';
const lookupCepMock = lookupCep as unknown as ReturnType<typeof vi.fn>;

const initial = {
  zipCode: '01310-100', street: 'Av. Paulista', number: '1000', complement: '',
  neighborhood: 'Bela Vista', city: 'São Paulo', state: 'SP',
};

describe('AddressForm', () => {
  beforeEach(() => lookupCepMock.mockReset());

  it('formata CEP enquanto digita (00000-000)', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AddressForm onSubmit={vi.fn()} />);
    const cep = screen.getByLabelText('CEP') as HTMLInputElement;
    await user.type(cep, '01310100');
    expect(cep.value).toBe('01310-100');
  });

  it('botão Buscar fica disabled enquanto CEP não tem 8 dígitos', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AddressForm onSubmit={vi.fn()} />);
    const cep = screen.getByLabelText('CEP');
    await user.type(cep, '0131');
    expect(screen.getByRole('button', { name: /buscar/i })).toBeDisabled();
    await user.type(cep, '0100');
    expect(screen.getByRole('button', { name: /buscar/i })).not.toBeDisabled();
  });

  it('buscar CEP preenche os campos e revela os demais inputs', async () => {
    lookupCepMock.mockResolvedValue({
      street: 'Av. Paulista', neighborhood: 'Bela Vista', city: 'São Paulo', state: 'SP',
    });
    const user = userEvent.setup();
    renderWithProviders(<AddressForm onSubmit={vi.fn()} />);

    await user.type(screen.getByLabelText('CEP'), '01310100');
    await user.click(screen.getByRole('button', { name: /buscar/i }));

    expect(await screen.findByLabelText('Logradouro')).toHaveValue('Av. Paulista');
    expect(screen.getByLabelText('Bairro')).toHaveValue('Bela Vista');
    expect(screen.getByLabelText('Cidade')).toHaveValue('São Paulo');
    expect(screen.getByLabelText('UF')).toHaveValue('SP');
  });

  it('aceita aliases do backend (logradouro/localidade/bairro/uf)', async () => {
    lookupCepMock.mockResolvedValue({
      logradouro: 'Rua A', bairro: 'Centro', localidade: 'Rio', uf: 'RJ',
    });
    const user = userEvent.setup();
    renderWithProviders(<AddressForm onSubmit={vi.fn()} />);
    await user.type(screen.getByLabelText('CEP'), '20000000');
    await user.click(screen.getByRole('button', { name: /buscar/i }));
    expect(await screen.findByLabelText('Logradouro')).toHaveValue('Rua A');
    expect(screen.getByLabelText('Bairro')).toHaveValue('Centro');
    expect(screen.getByLabelText('Cidade')).toHaveValue('Rio');
    expect(screen.getByLabelText('UF')).toHaveValue('RJ');
  });

  it('botão salvar desabilitado quando CEP não foi buscado', () => {
    const onSubmit = vi.fn();
    renderWithProviders(<AddressForm onSubmit={onSubmit} />);

    const saveBtn = screen.getByRole('button', { name: /salvar endereço/i });
    expect(saveBtn).toBeDisabled();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('renderiza com initial e permite editar; salva CEP só com dígitos', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderWithProviders(<AddressForm initial={initial} onSubmit={onSubmit} />);

    expect(screen.getByLabelText(/Número/)).toHaveValue('1000');
    await user.click(screen.getByRole('button', { name: /salvar endereço/i }));

    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
    expect(onSubmit.mock.calls[0][0]).toMatchObject({
      zipCode: '01310100', street: 'Av. Paulista', number: '1000', city: 'São Paulo', state: 'SP',
    });
  });

  it('valida número vazio antes de chamar onSubmit', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<AddressForm initial={{ ...initial, number: '' }} onSubmit={onSubmit} />);

    await user.click(screen.getByRole('button', { name: /salvar endereço/i }));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(await screen.findByText('Informe o número')).toBeInTheDocument();
  });

  it('mostra mensagem específica em CEP 404', async () => {
    lookupCepMock.mockRejectedValue(new ApiError('not found', 404, null));
    const user = userEvent.setup();
    renderWithProviders(<AddressForm onSubmit={vi.fn()} />);
    await user.type(screen.getByLabelText('CEP'), '99999999');
    await user.click(screen.getByRole('button', { name: /buscar/i }));
    expect(await screen.findByText('CEP não encontrado, verifique o número')).toBeInTheDocument();
  });

  it('mostra mensagem em CEP 502', async () => {
    lookupCepMock.mockRejectedValue(new ApiError('bad gw', 502, null));
    const user = userEvent.setup();
    renderWithProviders(<AddressForm onSubmit={vi.fn()} />);
    await user.type(screen.getByLabelText('CEP'), '99999999');
    await user.click(screen.getByRole('button', { name: /buscar/i }));
    expect(await screen.findByText(/Não conseguimos consultar o CEP/i)).toBeInTheDocument();
  });

  it('outros erros mostram message do Error', async () => {
    lookupCepMock.mockRejectedValue(new Error('crash'));
    const user = userEvent.setup();
    renderWithProviders(<AddressForm onSubmit={vi.fn()} />);
    await user.type(screen.getByLabelText('CEP'), '99999999');
    await user.click(screen.getByRole('button', { name: /buscar/i }));
    expect(await screen.findByText('crash')).toBeInTheDocument();
  });

  it('cancelar dispara onCancel', async () => {
    const onCancel = vi.fn();
    const user = userEvent.setup();
    renderWithProviders(<AddressForm initial={initial} onCancel={onCancel} onSubmit={vi.fn()} />);
    await user.click(screen.getByRole('button', { name: /cancelar/i }));
    expect(onCancel).toHaveBeenCalled();
  });
});
