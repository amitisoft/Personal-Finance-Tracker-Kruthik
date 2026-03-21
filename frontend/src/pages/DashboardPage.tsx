import { useEffect, useState } from "react";
import { Cell, Legend, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { ArrowDownCircle, ArrowUpCircle, Wallet } from "lucide-react";
import AppShell from "../components/AppShell";
import EmptyState from "../components/EmptyState";
import LoadingBlock from "../components/LoadingBlock";
import SectionCard from "../components/SectionCard";
import StatCard from "../components/StatCard";
import { financeService } from "../services/financeService";
import { Dashboard } from "../types/api";
import { useCurrency } from "../hooks/useCurrency";

const colors = ["#1d4ed8", "#0f766e", "#7c3aed", "#ea580c", "#dc2626"];

export default function DashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const { formatCurrency } = useCurrency();

  useEffect(() => {
    financeService.getDashboard().then(setData).finally(() => setLoading(false));
  }, []);

  return (
    <AppShell title="Dashboard">
      <div className="grid gap-4 md:grid-cols-3">
        <StatCard label="Total Balance" value={formatCurrency(data?.summary.balance || 0)} helper="Sum of all account balances" icon={<Wallet size={20} />} />
        <StatCard label="Income" value={formatCurrency(data?.summary.income || 0)} helper="Total income this month" icon={<ArrowUpCircle size={20} />} />
        <StatCard label="Expenses" value={formatCurrency(data?.summary.expense || 0)} helper="Total expenses this month" icon={<ArrowDownCircle size={20} />} />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-2">
        <SectionCard title="Category spending">
          {loading ? <LoadingBlock className="h-80 w-full" /> : data?.categorySpending.length ? (
            <div className="h-80">
              <ResponsiveContainer>
                <PieChart>
                  <Pie data={data.categorySpending} dataKey="amount" nameKey="category" innerRadius={70} outerRadius={110}>
                    {data.categorySpending.map((_, index) => <Cell key={index} fill={colors[index % colors.length]} />)}
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

        <SectionCard title="Income vs Expense">
          {loading ? <LoadingBlock className="h-80 w-full" /> : data?.incomeVsExpense.length ? (
            <div className="h-80">
              <ResponsiveContainer>
                <LineChart data={data.incomeVsExpense}>
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
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <SectionCard title="Recent transactions">
          {loading ? <LoadingBlock className="h-72 w-full" /> : data?.recentTransactions.length ? (
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
                  {data.recentTransactions.map((transaction) => (
                    <tr key={transaction.id}>
                      <td>
                        <div className="font-medium text-slate-900">{transaction.merchant || "Untitled"}</div>
                      </td>
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
          {loading ? <LoadingBlock className="h-72 w-full" /> : data?.upcomingRecurring.length ? (
            <div className="space-y-3">
              {data.upcomingRecurring.map((item) => (
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
