import { useEffect, useMemo, useState } from "react";
import AppShell from "../components/AppShell";
import EmptyState from "../components/EmptyState";
import LoadingBlock from "../components/LoadingBlock";
import SectionCard from "../components/SectionCard";
import { useCurrency } from "../hooks/useCurrency";
import { financeService } from "../services/financeService";
import { Budget, Category } from "../types/api";
import { useToast } from "../context/ToastContext";
import { getApiErrorMessage } from "../utils/apiError";

function Field({ label, required = false, children }: { label: string; required?: boolean; children: React.ReactNode }) {
  return (
    <label className="grid gap-2 text-sm font-medium text-slate-700">
      <span>
        {label}
        {required ? <span className="ml-1 text-rose-600">*</span> : null}
      </span>
      {children}
    </label>
  );
}

export default function BudgetsPage() {
  const today = new Date();
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState({ categoryId: "", month: today.getMonth() + 1, year: today.getFullYear(), amount: "", alertThresholdPercent: 80 });
  const { formatCurrency } = useCurrency();
  const { showToast } = useToast();

  const load = () => {
    setLoading(true);
    financeService.getBudgets(form.month, form.year)
      .then(setBudgets)
      .catch((error) => showToast(getApiErrorMessage(error, "Unable to load budgets"), "error"))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    financeService.getCategories()
      .then((data) => setCategories(data.filter((item) => item.type === "EXPENSE")))
      .catch((error) => showToast(getApiErrorMessage(error, "Unable to load categories"), "error"));
  }, []);

  useEffect(() => {
    load();
  }, [form.month, form.year]);

  const summary = useMemo(() => ({
    planned: budgets.reduce((sum, budget) => sum + budget.amount, 0),
    spent: budgets.reduce((sum, budget) => sum + budget.spent, 0),
  }), [budgets]);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!form.categoryId || !form.amount || Number(form.amount) <= 0) {
      showToast("Budget form is incomplete", "error");
      return;
    }
    try {
      await financeService.saveBudget({ ...form, amount: Number(form.amount) });
      showToast("Budget saved");
      setForm((current) => ({ ...current, amount: "" }));
      load();
    } catch (error) {
      showToast(getApiErrorMessage(error, "Unable to save budget"), "error");
    }
  };

  return (
    <AppShell title="Budgets">
      <div className="mb-6 grid gap-4 md:grid-cols-3">
        <Stat title="Planned" value={formatCurrency(summary.planned)} />
        <Stat title="Spent" value={formatCurrency(summary.spent)} />
        <Stat title="Budget categories" value={String(budgets.length)} />
      </div>

      <div className="grid gap-6 xl:grid-cols-[340px_1fr]">
        <SectionCard title="Set budget">
          <form className="grid gap-4" onSubmit={submit}>
            <Field label="Category" required>
              <select value={form.categoryId} onChange={(event) => setForm((current) => ({ ...current, categoryId: event.target.value }))} className="app-select">
                <option value="">Choose category</option>
                {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
              </select>
            </Field>
            <div className="grid grid-cols-2 gap-4">
              <Field label="Month" required>
                <input type="number" value={form.month} onChange={(event) => setForm((current) => ({ ...current, month: Number(event.target.value) }))} className="app-input" />
              </Field>
              <Field label="Year" required>
                <input type="number" value={form.year} onChange={(event) => setForm((current) => ({ ...current, year: Number(event.target.value) }))} className="app-input" />
              </Field>
            </div>
            <Field label="Amount" required>
              <input type="number" min="0" step="0.01" value={form.amount} onChange={(event) => setForm((current) => ({ ...current, amount: event.target.value }))} className="app-input" />
            </Field>
            <Field label="Alert threshold %">
              <input type="number" min="1" max="120" value={form.alertThresholdPercent} onChange={(event) => setForm((current) => ({ ...current, alertThresholdPercent: Number(event.target.value) }))} className="app-input" />
            </Field>
            <button className="app-button-primary">Save budget</button>
          </form>
        </SectionCard>

        <SectionCard title="Budget tracking">
          {loading ? (
            <div className="space-y-4">
              <LoadingBlock className="h-24 w-full" />
              <LoadingBlock className="h-24 w-full" />
            </div>
          ) : !budgets.length ? (
            <EmptyState title="No budgets set" message="Create a monthly budget to track spending against plan." />
          ) : (
            <div className="space-y-4">
              {budgets.map((budget) => (
                <div key={budget.id} className="app-panel rounded-xl p-4">
                  <div className="mb-3 flex items-center justify-between gap-3">
                    <div>
                      <p className="font-medium text-slate-900">{budget.categoryName}</p>
                      <p className="text-sm text-slate-500">{formatCurrency(budget.spent)} spent of {formatCurrency(budget.amount)}</p>
                    </div>
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold ${budget.utilizationPercentage > 100 ? "bg-rose-100 text-rose-600" : "bg-slate-100 text-slate-700"}`}>{budget.utilizationPercentage.toFixed(0)}%</span>
                  </div>
                  <div className="h-2 rounded-full bg-slate-200">
                    <div className={`h-2 rounded-full ${budget.utilizationPercentage > 100 ? "bg-rose-500" : "bg-slate-900"}`} style={{ width: `${Math.min(100, budget.utilizationPercentage)}%` }} />
                  </div>
                  <p className="mt-3 text-sm text-slate-500">Remaining: {formatCurrency(budget.remaining)}</p>
                </div>
              ))}
            </div>
          )}
        </SectionCard>
      </div>
    </AppShell>
  );
}

function Stat({ title, value }: { title: string; value: string }) {
  return (
    <SectionCard title={title} className="p-5">
      <p className="text-3xl font-semibold text-slate-900">{value}</p>
    </SectionCard>
  );
}
