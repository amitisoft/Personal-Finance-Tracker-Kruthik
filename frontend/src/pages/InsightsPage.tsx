import { useEffect, useState } from "react";
import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis, Bar, BarChart } from "recharts";
import AppShell from "../components/AppShell";
import EmptyState from "../components/EmptyState";
import LoadingBlock from "../components/LoadingBlock";
import SectionCard from "../components/SectionCard";
import StatCard from "../components/StatCard";
import { financeService } from "../services/financeService";
import { InsightsResponse } from "../types/api";
import { useCurrency } from "../hooks/useCurrency";

export default function InsightsPage() {
  const [data, setData] = useState<InsightsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const { formatCurrency } = useCurrency();

  useEffect(() => {
    financeService.getInsights().then(setData).finally(() => setLoading(false));
  }, []);

  return (
    <AppShell title="Insights">
      <div className="grid gap-4 md:grid-cols-3">
        <StatCard label="Health Score" value={data ? `${data.healthScore.score}/100` : "0/100"} helper={data?.healthScore.status || "Overall financial resilience"} />
        <StatCard label="Top Savings Month" value={formatCurrency(Math.max(...(data?.savingsTrend.map((item) => item.savings) || [0])))} helper="Best month from recent savings trend" />
        <StatCard label="Highlights" value={String(data?.highlights.length || 0)} helper="Automated trend observations" />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <SectionCard title="Insight highlights">
          {loading ? <LoadingBlock className="h-64 w-full" /> : data?.highlights.length ? (
            <div className="space-y-3">
              {data.highlights.map((highlight) => (
                <div key={`${highlight.title}-${highlight.message}`} className={`rounded-xl border p-4 ${highlight.tone === "positive" ? "border-emerald-200 bg-emerald-50" : highlight.tone === "warning" ? "border-amber-200 bg-amber-50" : "border-slate-200 bg-slate-50"}`}>
                  <p className="font-semibold text-slate-900">{highlight.title}</p>
                  <p className="mt-2 text-sm text-slate-600">{highlight.message}</p>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title="No insights yet" message="Add more financial activity to unlock automated insights." />
          )}
        </SectionCard>

        <SectionCard title="Recommendations">
          {loading ? <LoadingBlock className="h-64 w-full" /> : data?.healthScore.suggestions.length ? (
            <div className="space-y-3">
              {data.healthScore.suggestions.map((suggestion) => (
                <div key={suggestion} className="rounded-xl bg-slate-50 p-4 text-sm text-slate-600">{suggestion}</div>
              ))}
            </div>
          ) : (
            <EmptyState title="No suggestions" message="Recommendations will appear once enough data is available." />
          )}
        </SectionCard>
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-2">
        <SectionCard title="Income vs expense trend">
          {loading ? <LoadingBlock className="h-80 w-full" /> : data?.incomeExpenseTrend.length ? (
            <div className="h-80">
              <ResponsiveContainer>
                <LineChart data={data.incomeExpenseTrend}>
                  <CartesianGrid strokeDasharray="3 3" />
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
            <EmptyState title="No trend data" message="Recent income and expense trends will appear here." />
          )}
        </SectionCard>

        <SectionCard title="Savings trend">
          {loading ? <LoadingBlock className="h-80 w-full" /> : data?.savingsTrend.length ? (
            <div className="h-80">
              <ResponsiveContainer>
                <BarChart data={data.savingsTrend}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="label" />
                  <YAxis />
                  <Tooltip formatter={(value: number) => formatCurrency(value)} />
                  <Bar dataKey="savings" fill="#1d4ed8" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <EmptyState title="No savings trend" message="Savings trends will appear as you add income and expense history." />
          )}
        </SectionCard>
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
        <SectionCard title="Health breakdown">
          {loading ? <LoadingBlock className="h-64 w-full" /> : data?.healthScore.breakdown.length ? (
            <div className="space-y-3">
              {data.healthScore.breakdown.map((item) => (
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
            <EmptyState title="No breakdown available" message="The score breakdown will appear here once enough data exists." />
          )}
        </SectionCard>

        <SectionCard title="Category trends">
          {loading ? <LoadingBlock className="h-80 w-full" /> : data?.categoryTrends.length ? (
            <div className="h-80">
              <ResponsiveContainer>
                <LineChart data={data.categoryTrends}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="label" />
                  <YAxis />
                  <Tooltip formatter={(value: number) => formatCurrency(value)} />
                  <Legend />
                  <Line type="monotone" dataKey="amount" name="Category spend" stroke="#7c3aed" strokeWidth={3} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <EmptyState title="No category trend data" message="More expense history is needed to show category movements over time." />
          )}
        </SectionCard>
      </div>
    </AppShell>
  );
}
