import { useEffect, useMemo, useState } from 'react';
import {
  Grid, Card, CardContent, Typography, Skeleton, Box, ToggleButton, ToggleButtonGroup, Stack,
} from '@mui/material';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import { LineChart } from '@mui/x-charts/LineChart';
import { PieChart } from '@mui/x-charts/PieChart';
import AdminLayout from '../../components/AdminLayout';
import { listAllOrders } from '../../api/orders';
import { COMPANY_OWNER_ID, getWalletByOwner } from '../../api/wallets';
import type { Order } from '../../api/types';

const fmt = (v: number | undefined) =>
  Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

const fmtShort = (v: number) =>
  v >= 1000 ? `${(v / 1000).toFixed(1)}k` : String(Math.round(v));

type PeriodKey = '7' | '30' | '90' | 'all';

const PERIODS: { key: PeriodKey; label: string; days: number | null }[] = [
  { key: '7', label: '7 dias', days: 7 },
  { key: '30', label: '30 dias', days: 30 },
  { key: '90', label: '90 dias', days: 90 },
  { key: 'all', label: 'Tudo', days: null },
];

const orderAmount = (o: Order) => o.totalAmount ?? o.total ?? o.amount ?? 0;
const orderDate = (o: Order) => (o.createdAt ? new Date(o.createdAt) : null);

function extractList<T>(raw: unknown): T[] {
  if (Array.isArray(raw)) return raw as T[];
  const r = raw as { data?: T[]; content?: T[]; items?: T[] } | null;
  return r?.data || r?.content || r?.items || [];
}

interface Metrics {
  totalOrders: number;
  averageTicket: number;
  companyBalance: number;
}

interface SalesPoint { day: string; date: Date; revenue: number; count: number }

export default function AdminDashboard() {
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [companyBalance, setCompanyBalance] = useState<number>(0);
  const [period, setPeriod] = useState<PeriodKey>('30');

  useEffect(() => {
    Promise.all([
      listAllOrders(0, 200),
      getWalletByOwner(COMPANY_OWNER_ID, 'COMPANY').catch(() => null),
    ]).then(([ordersData, wallet]) => {
      setOrders(extractList<Order>(ordersData));
      setCompanyBalance(wallet?.balance ?? 0);
    });
  }, []);

  const filteredOrders = useMemo(() => {
    if (!orders) return [];
    const cfg = PERIODS.find((p) => p.key === period);
    if (!cfg?.days) return orders;
    const cutoff = Date.now() - cfg.days * 86_400_000;
    return orders.filter((o) => {
      const d = orderDate(o);
      return d ? d.getTime() >= cutoff : false;
    });
  }, [orders, period]);

  const metrics = useMemo<Metrics | null>(() => {
    if (!orders) return null;
    const paid = filteredOrders.filter((o) => o.status === 'PAID');
    const revenue = paid.reduce((s, o) => s + orderAmount(o), 0);
    return {
      totalOrders: filteredOrders.length,
      averageTicket: paid.length ? revenue / paid.length : 0,
      companyBalance,
    };
  }, [orders, filteredOrders, companyBalance]);

  const salesSeries = useMemo<SalesPoint[]>(() => {
    const buckets = new Map<string, SalesPoint>();
    filteredOrders
      .filter((o) => o.status === 'PAID')
      .forEach((o) => {
        const d = orderDate(o);
        if (!d) return;
        const key = d.toISOString().slice(0, 10);
        const bucket = buckets.get(key) || { day: key, date: new Date(key), revenue: 0, count: 0 };
        bucket.revenue += orderAmount(o);
        bucket.count += 1;
        buckets.set(key, bucket);
      });
    return Array.from(buckets.values()).sort((a, b) => a.date.getTime() - b.date.getTime());
  }, [filteredOrders]);

  const statusBreakdown = useMemo(() => {
    const counts = new Map<string, number>();
    filteredOrders.forEach((o) => {
      const s = String(o.status || 'DESCONHECIDO');
      counts.set(s, (counts.get(s) || 0) + 1);
    });
    return Array.from(counts.entries()).map(([label, value], i) => ({
      id: i,
      label,
      value,
    }));
  }, [filteredOrders]);

  const cards = [
    { label: 'Total de pedidos', value: metrics ? String(metrics.totalOrders) : null, icon: <ShoppingCartIcon fontSize="large" color="primary" /> },
    { label: 'Ticket médio', value: metrics ? fmt(metrics.averageTicket) : null, icon: <TrendingUpIcon fontSize="large" color="primary" /> },
    { label: 'Saldo da empresa', value: metrics ? fmt(metrics.companyBalance) : null, icon: <AccountBalanceWalletIcon fontSize="large" color="primary" /> },
  ];

  return (
    <AdminLayout>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2, flexWrap: 'wrap', gap: 2 }}>
        <Typography variant="h5">Dashboard</Typography>
        <ToggleButtonGroup
          value={period}
          exclusive
          onChange={(_, v: PeriodKey | null) => v && setPeriod(v)}
          size="small"
          aria-label="período"
        >
          {PERIODS.map((p) => (
            <ToggleButton key={p.key} value={p.key} aria-label={`período ${p.label}`}>
              {p.label}
            </ToggleButton>
          ))}
        </ToggleButtonGroup>
      </Stack>

      <Grid container spacing={2}>
        {cards.map((c) => (
          <Grid key={c.label} item xs={12} sm={6} md={4}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1 }}>
                  {c.icon}
                  <Typography variant="overline" color="text.secondary">{c.label}</Typography>
                </Box>
                {c.value === null ? (
                  <Skeleton width={120} height={48} />
                ) : (
                  <Typography variant="h4" sx={{ fontWeight: 600 }}>{c.value}</Typography>
                )}
              </CardContent>
            </Card>
          </Grid>
        ))}

        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1 }}>Vendas no tempo</Typography>
              {!orders ? (
                <Skeleton variant="rounded" height={280} />
              ) : salesSeries.length === 0 ? (
                <Box sx={{ height: 280, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Typography color="text.secondary">Sem pedidos pagos no período</Typography>
                </Box>
              ) : (
                <LineChart
                  height={280}
                  xAxis={[{
                    data: salesSeries.map((s) => s.date),
                    scaleType: 'time',
                    valueFormatter: (d: Date) => d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }),
                  }]}
                  yAxis={[
                    { id: 'revenue', valueFormatter: (v: number) => `R$ ${fmtShort(v)}` },
                    { id: 'count', valueFormatter: (v: number) => String(v) },
                  ]}
                  series={[
                    { id: 'revenue', label: 'Receita (R$)', data: salesSeries.map((s) => s.revenue), area: true, yAxisKey: 'revenue', valueFormatter: (v) => fmt(v ?? 0) },
                    { id: 'count', label: 'Pedidos', data: salesSeries.map((s) => s.count), yAxisKey: 'count' },
                  ]}
                  rightAxis="count"
                />
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 1 }}>Status dos pedidos</Typography>
              {!orders ? (
                <Skeleton variant="rounded" height={280} />
              ) : statusBreakdown.length === 0 ? (
                <Box sx={{ height: 280, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Typography color="text.secondary">Sem pedidos no período</Typography>
                </Box>
              ) : (
                <PieChart
                  height={280}
                  series={[{
                    innerRadius: 50,
                    paddingAngle: 2,
                    cornerRadius: 4,
                    data: statusBreakdown,
                  }]}
                />
              )}
            </CardContent>
          </Card>
        </Grid>

      </Grid>
    </AdminLayout>
  );
}
