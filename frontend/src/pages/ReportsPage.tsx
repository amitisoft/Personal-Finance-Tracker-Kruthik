import { useEffect, useState } from "react";
import { Bar, BarChart, CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Download } from "lucide-react";
import AppShell from "../components/AppShell";
import EmptyState from "../components/EmptyState";
import LoadingBlock from "../components/LoadingBlock";
import SectionCard from "../components/SectionCard";
import { useCurrency } from "../hooks/useCurrency";
import { financeService } from "../services/financeService";
import { FilteredReport } from "../types/api";
import { currentMonthRange } from "../utils/date";
import { useToast } from "../context/ToastContext";

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="grid gap-2 text-sm font-medium text-slate-700">
      <span>{label}</span>
      {children}
    </label>
  );
}

export default function ReportsPage() {
  const range = currentMonthRange();
  const [startDate, setStartDate] = useState(range.start);
  const [endDate, setEndDate] = useState(range.end);
  const [report, setReport] = useState<FilteredReport | null>(null);
  const [loading, setLoading] = useState(true);
  const { formatCurrency } = useCurrency();
  const { showToast } = useToast();

  const load = () => {
    setLoading(true);
    financeService.getReportSummary(startDate, endDate).then(setReport).finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const exportCsv = async () => {
    const csv = await financeService.exportCsv(startDate, endDate);
    navigator.clipboard.writeText(csv);
    showToast("CSV copied to clipboard");
  };

  return (
    <AppShell title="Reports">
      <SectionCard title="Filters" action={<button onClick={exportCsv} className="app-button-secondary"><Download size={16} /> Export CSV</button>}>
        <div className="grid gap-4 lg:grid-cols-[1fr_1fr_auto]">
          <Field label="Start date">
            <input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} className="app-input" />
          </Field>
          <Field label="End date">
            <input type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} className="app-input" />
          </Field>
          <div className="flex items-end">
            <button onClick={load} className="app-button-primary w-full lg:w-auto">Apply filters</button>
          </div>
        </div>
      </SectionCard>

      <div className="mt-6 grid gap-6 xl:grid-cols-2">
        <SectionCard title="Category spending">
          {loading ? <LoadingBlock className="h-80 w-full" /> : report?.categorySpend.length ? (
            <div className="h-80">
              <ResponsiveContainer>
                <BarChart data={report.categorySpend}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="category" />
                  <YAxis />
                  <Tooltip formatter={(value: number) => formatCurrency(value)} />
                  <Bar dataKey="amount" fill="#1e3a8a" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <EmptyState title="No category data" message="Expand the date range to load report data." />
          )}
        </SectionCard>

        <SectionCard title="Income vs Expense">
          {loading ? <LoadingBlock className="h-80 w-full" /> : report?.incomeVsExpense.length ? (
            <div className="h-80">
              <ResponsiveContainer>
                <LineChart data={report.incomeVsExpense}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="label" />
                  <YAxis />
                  <Tooltip formatter={(value: number) => formatCurrency(value)} />
                  <Legend />
                  <Line type="monotone" dataKey="income" stroke="#0f766e" strokeWidth={3} />
                  <Line type="monotone" dataKey="expense" stroke="#1e3a8a" strokeWidth={3} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <EmptyState title="No trend data" message="Once transactions exist in the selected range, the chart will appear here." />
          )}
        </SectionCard>
      </div>

      <div className="mt-6 grid gap-4 md:grid-cols-3">
        {loading ? (
          <>
            <LoadingBlock className="h-32 w-full" />
            <LoadingBlock className="h-32 w-full" />
            <LoadingBlock className="h-32 w-full" />
          </>
        ) : report?.accountBalances.length ? (
          report.accountBalances.map((account) => (
            <SectionCard key={account.account} title={account.account} className="p-5">
              <p className="text-3xl font-semibold text-slate-900">{formatCurrency(account.balance)}</p>
            </SectionCard>
          ))
        ) : (
          <div className="md:col-span-3">
            <EmptyState title="No account balances" message="Create accounts and transactions to view balance reports." />
          </div>
        )}
      </div>
    </AppShell>
  );
}
