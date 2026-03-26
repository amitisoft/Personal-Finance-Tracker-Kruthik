import { Download } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Bar, BarChart, CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import AppShell from "../components/AppShell";
import EmptyState from "../components/EmptyState";
import LoadingBlock from "../components/LoadingBlock";
import SectionCard from "../components/SectionCard";
import { useToast } from "../context/ToastContext";
import { useCurrency } from "../hooks/useCurrency";
import { financeService } from "../services/financeService";
import { Account, CategorySpendPoint, NetWorthReport, TrendPoint } from "../types/api";
import { currentMonthRange } from "../utils/date";
import { getApiErrorMessage } from "../utils/apiError";

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
  const [categorySpend, setCategorySpend] = useState<CategorySpendPoint[]>([]);
  const [incomeExpense, setIncomeExpense] = useState<TrendPoint[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [netWorth, setNetWorth] = useState<NetWorthReport | null>(null);
  const [loading, setLoading] = useState(true);
  const { formatCurrency } = useCurrency();
  const { showToast } = useToast();

  const load = () => {
    setLoading(true);
    Promise.allSettled([
      financeService.getCategorySpendReport(startDate, endDate),
      financeService.getIncomeExpenseReport(startDate, endDate),
      financeService.getAccounts(),
      financeService.getNetWorth(),
    ])
      .then(([categoryResult, trendResult, accountsResult, netWorthResult]) => {
        if (categoryResult.status === "fulfilled") {
          setCategorySpend(categoryResult.value);
        } else {
          setCategorySpend([]);
          showToast(getApiErrorMessage(categoryResult.reason, "Unable to load category report"), "error");
        }

        if (trendResult.status === "fulfilled") {
          setIncomeExpense(trendResult.value);
        } else {
          setIncomeExpense([]);
          showToast(getApiErrorMessage(trendResult.reason, "Unable to load income vs expense report"), "error");
        }

        if (accountsResult.status === "fulfilled") {
          setAccounts(accountsResult.value);
        } else {
          setAccounts([]);
          showToast(getApiErrorMessage(accountsResult.reason, "Unable to load account balances"), "error");
        }

        if (netWorthResult.status === "fulfilled") {
          setNetWorth(netWorthResult.value);
        } else {
          setNetWorth(null);
          showToast(getApiErrorMessage(netWorthResult.reason, "Unable to load net worth report"), "error");
        }
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const exportCsv = async () => {
    try {
      const csv = await financeService.exportCsv(startDate, endDate);
      navigator.clipboard.writeText(csv);
      showToast("CSV copied to clipboard");
    } catch (error) {
      showToast(getApiErrorMessage(error, "Unable to export CSV"), "error");
    }
  };

  const accountCards = useMemo(() => accounts.filter((account) => typeof account.currentBalance === "number"), [accounts]);

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
          {loading ? <LoadingBlock className="h-80 w-full" /> : categorySpend.length ? (
            <div className="h-80">
              <ResponsiveContainer>
                <BarChart data={categorySpend}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="category" />
                  <YAxis />
                  <Tooltip formatter={(value: number) => formatCurrency(value)} />
                  <Bar dataKey="amount" fill="#1e3a8a" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <EmptyState title="No category data" message="Expand the date range or add expense transactions in the selected period." />
          )}
        </SectionCard>

        <SectionCard title="Income vs Expense">
          {loading ? <LoadingBlock className="h-80 w-full" /> : incomeExpense.length ? (
            <div className="h-80">
              <ResponsiveContainer>
                <LineChart data={incomeExpense}>
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
            <EmptyState title="No trend data" message="Add transactions in the selected date range to see the trend line." />
          )}
        </SectionCard>
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
        <SectionCard title="Current net worth">
          {loading ? <LoadingBlock className="h-72 w-full" /> : netWorth ? (
            <div className="space-y-4">
              <div className="rounded-2xl bg-slate-50 p-5">
                <p className="text-sm text-slate-500">Current accessible net worth</p>
                <p className="mt-2 text-3xl font-semibold text-slate-900">{formatCurrency(netWorth.currentNetWorth)}</p>
              </div>
              {netWorth.history.length ? (
                <div className="h-56">
                  <ResponsiveContainer>
                    <LineChart data={netWorth.history}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="label" />
                      <YAxis />
                      <Tooltip formatter={(value: number) => formatCurrency(value)} />
                      <Line type="monotone" dataKey="netWorth" stroke="#7c3aed" strokeWidth={3} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              ) : null}
            </div>
          ) : (
            <EmptyState title="No net worth data" message="Net worth history will appear once accounts are available." />
          )}
        </SectionCard>

        <SectionCard title="Account balances">
          {loading ? (
            <div className="space-y-3">
              <LoadingBlock className="h-20 w-full" />
              <LoadingBlock className="h-20 w-full" />
            </div>
          ) : accountCards.length ? (
            <div className="grid gap-4 md:grid-cols-2">
              {accountCards.map((account) => (
                <div key={account.id} className="rounded-2xl border border-slate-200 bg-white p-5">
                  <p className="text-sm text-slate-500">{account.name}</p>
                  <p className="mt-2 text-2xl font-semibold text-slate-900">{formatCurrency(account.currentBalance)}</p>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title="No account balances" message="Create accounts and transactions to view balance reports." />
          )}
        </SectionCard>
      </div>
    </AppShell>
  );
}
