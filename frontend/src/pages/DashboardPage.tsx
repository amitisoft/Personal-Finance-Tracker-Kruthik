import { useEffect, useState } from "react";
import { Cell, Legend, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { ArrowDownCircle, ArrowUpCircle, ShieldCheck, Sparkles, Wallet } from "lucide-react";
import AppShell from "../components/AppShell";
import EmptyState from "../components/EmptyState";
import LoadingBlock from "../components/LoadingBlock";
import SectionCard from "../components/SectionCard";
import StatCard from "../components/StatCard";
import { financeService } from "../services/financeService";
import { DailyForecast, Dashboard, ForecastMonth, HealthScore } from "../types/api";
import { useCurrency } from "../hooks/useCurrency";

const colors = ["#1d4ed8", "#0f766e", "#7c3aed", "#ea580c", "#dc2626"];

export default function DashboardPage() {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [monthForecast, setMonthForecast] = useState<ForecastMonth | null>(null);
  const [dailyForecast, setDailyForecast] = useState<DailyForecast | null>(null);
  const [healthScore, setHealthScore] = useState<HealthScore | null>(null);
  const [loading, setLoading] = useState(true);
  const { formatCurrency } = useCurrency();

  useEffect(() => {
    setLoading(true);
    Promise.allSettled([
      financeService.getDashboard(),
      financeService.getForecastMonth(),
      financeService.getForecastDaily(),
      financeService.getHealthScore(),
    ])
      .then(([dashboardResult, monthResult, dailyResult, healthResult]) => {
        if (dashboardResult.status === "fulfilled") {
          setDashboard(dashboardResult.value);
        }
        setMonthForecast(monthResult.status === "fulfilled" ? monthResult.value : null);
        setDailyForecast(dailyResult.status === "fulfilled" ? dailyResult.value : null);
        setHealthScore(healthResult.status === "fulfilled" ? healthResult.value : null);
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <AppShell title="Dashboard">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <StatCard label="Total Balance" value={formatCurrency(dashboard?.summary.balance || 0)} helper="Sum of all account balances" icon={<Wallet size={20} />} />
        <StatCard label="Income" value={formatCurrency(dashboard?.summary.income || 0)} helper="Total income this month" icon={<ArrowUpCircle size={20} />} />
        <StatCard label="Expenses" value={formatCurrency(dashboard?.summary.expense || 0)} helper="Total expenses this month" icon={<ArrowDownCircle size={20} />} />
        <StatCard label="Projected Balance" value={formatCurrency(monthForecast?.projectedEndBalance || dashboard?.summary.balance || 0)} helper={monthForecast ? `${monthForecast.daysRemaining} days remaining this month` : "Projection unavailable, showing current balance"} icon={<Sparkles size={20} />} />
        <StatCard label="Health Score" value={healthScore ? `${healthScore.score}/100` : "--"} helper={healthScore?.status || "Needs more history to score"} icon={<ShieldCheck size={20} />} />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-2">
        <SectionCard title="Forecast outlook" eyebrow="V2 Decision Support">
          {loading ? <LoadingBlock className="h-80 w-full" /> : dailyForecast?.projections.length ? (
            <div className="space-y-5">
              <div className="grid gap-4 md:grid-cols-4">
                <div className="rounded-xl bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Current balance</p>
                  <p className="mt-2 text-xl font-semibold text-slate-900">{formatCurrency(monthForecast?.currentBalance || dashboard?.summary.balance || 0)}</p>
                </div>
                <div className="rounded-xl bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Projected income</p>
                  <p className="mt-2 text-xl font-semibold text-emerald-600">{formatCurrency(monthForecast?.projectedIncome || dashboard?.summary.income || 0)}</p>
                </div>
                <div className="rounded-xl bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Projected expense</p>
                  <p className="mt-2 text-xl font-semibold text-rose-600">{formatCurrency(monthForecast?.projectedExpense || dashboard?.summary.expense || 0)}</p>
                </div>
                <div className="rounded-xl bg-slate-50 p-4">
                  <p className="text-sm text-slate-500">Safe to spend</p>
                  <p className="mt-2 text-xl font-semibold text-slate-900">{formatCurrency(monthForecast?.safeToSpend || 0)}</p>
                </div>
              </div>
              <div className={`rounded-xl border px-4 py-3 text-sm ${monthForecast?.riskLevel === "HIGH" ? "border-rose-200 bg-rose-50 text-rose-700" : monthForecast?.riskLevel === "MEDIUM" ? "border-amber-200 bg-amber-50 text-amber-700" : "border-emerald-200 bg-emerald-50 text-emerald-700"}`}>
                {monthForecast?.warning || "Projection indicates a stable month-end balance."}
              </div>
              <div className="h-64">
                <ResponsiveContainer>
                  <LineChart data={dailyForecast.projections}>
                    <XAxis dataKey="date" />
                    <YAxis />
                    <Tooltip formatter={(value: number) => formatCurrency(value)} />
                    <Line type="monotone" dataKey="projectedBalance" stroke="#1d4ed8" strokeWidth={3} dot={false} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </div>
          ) : (
            <EmptyState title="Forecast unavailable" message="Current dashboard data is still available. Add more history or retry after backend restart." />
          )}
        </SectionCard>

        <SectionCard title="Category spending">
          {loading ? <LoadingBlock className="h-80 w-full" /> : dashboard?.categorySpending.length ? (
            <div className="h-80">
              <ResponsiveContainer>
                <PieChart>
                  <Pie data={dashboard.categorySpending} dataKey="amount" nameKey="category" innerRadius={70} outerRadius={110}>
                    {dashboard.categorySpending.map((_, index) => <Cell key={index} fill={colors[index % colors.length]} />)}
                  </Pie>
                  <Tooltip formatter={(value: number) => formatCurrency(value)} />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <EmptyState title="No spending data" message="Add expense transactions to see a category breakdown." />
          )}
        </SectionCard>
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <SectionCard title="Income vs Expense">
          {loading ? <LoadingBlock className="h-80 w-full" /> : dashboard?.incomeVsExpense.length ? (
            <div className="h-80">
              <ResponsiveContainer>
                <LineChart data={dashboard.incomeVsExpense}>
                  <XAxis dataKey="label" />
                  <YAxis />
                  <Tooltip formatter={(value: number) => formatCurrency(value)} />
                  <Legend />
                  <Line type="monotone" dataKey="income" stroke="#0f766e" strokeWidth={3} />
                  <Line type="monotone" dataKey="expense" stroke="#1d4ed8" strokeWidth={3} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <EmptyState title="No trend data" message="Add transactions over time to see your income and expense chart." />
          )}
        </SectionCard>

        <SectionCard title="Health breakdown">
          {loading ? <LoadingBlock className="h-80 w-full" /> : healthScore ? (
            <div className="space-y-3">
              {healthScore.breakdown.map((item) => (
                <div key={item.metric} className="rounded-xl bg-slate-50 p-4">
                  <div className="flex items-center justify-between gap-3">
                    <p className="font-medium text-slate-900">{item.metric}</p>
                    <span className="text-sm font-semibold text-slate-600">{item.score}/100</span>
                  </div>
                  <p className="mt-2 text-sm text-slate-500">{item.detail}</p>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title="Health score unavailable" message="Your dashboard still works; score will appear once the insight service responds successfully." />
          )}
        </SectionCard>
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <SectionCard title="Recent transactions">
          {loading ? <LoadingBlock className="h-72 w-full" /> : dashboard?.recentTransactions.length ? (
            <div className="overflow-auto rounded-xl border border-slate-200">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Merchant</th>
                    <th>Category</th>
                    <th>Account</th>
                    <th>Date</th>
                    <th>Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {dashboard.recentTransactions.map((transaction) => (
                    <tr key={transaction.id}>
                      <td><div className="font-medium text-slate-900">{transaction.merchant || "Untitled"}</div></td>
                      <td>{transaction.categoryName || transaction.type}</td>
                      <td>{transaction.accountName}</td>
                      <td>{transaction.date}</td>
                      <td className={transaction.type === "EXPENSE" ? "text-rose-600" : "text-emerald-600"}>{formatCurrency(transaction.amount)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState title="No recent transactions" message="Recent transactions will appear here once you start logging entries." />
          )}
        </SectionCard>

        <SectionCard title="Upcoming recurring">
          {loading ? <LoadingBlock className="h-72 w-full" /> : dashboard?.upcomingRecurring.length ? (
            <div className="space-y-3">
              {dashboard.upcomingRecurring.map((item) => (
                <div key={item.id} className="app-panel rounded-xl p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="font-medium text-slate-900">{item.title}</p>
                      <p className="mt-1 text-sm text-slate-500">{item.frequency}</p>
                    </div>
                    <span className="text-sm text-slate-500">{item.nextRunDate}</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title="No recurring items" message="Recurring salaries or bills will appear here when available." />
          )}
        </SectionCard>
      </div>
    </AppShell>
  );
}
