import { useEffect, useMemo, useState } from "react";
import AppShell from "../components/AppShell";
import EmptyState from "../components/EmptyState";
import LoadingBlock from "../components/LoadingBlock";
import SectionCard from "../components/SectionCard";
import { useCurrency } from "../hooks/useCurrency";
import { financeService } from "../services/financeService";
import { Account, Goal } from "../types/api";
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

export default function GoalsPage() {
  const [goals, setGoals] = useState<Goal[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState({ name: "", targetAmount: "", targetDate: "", linkedAccountId: "", icon: "target", color: "#3563e9" });
  const { formatCurrency } = useCurrency();
  const { showToast } = useToast();

  const load = () => {
    setLoading(true);
    financeService.getGoals()
      .then(setGoals)
      .catch((error) => showToast(getApiErrorMessage(error, "Unable to load goals"), "error"))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    financeService.getAccounts().then(setAccounts).catch((error) => showToast(getApiErrorMessage(error, "Unable to load accounts"), "error"));
  }, []);

  const totalSaved = useMemo(() => goals.reduce((sum, goal) => sum + goal.currentAmount, 0), [goals]);

  const saveGoal = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!form.name || !form.targetAmount) {
      showToast("Goal name and target are required", "error");
      return;
    }
    try {
      await financeService.saveGoal({ ...form, targetAmount: Number(form.targetAmount) });
      showToast("Goal saved");
      setForm({ name: "", targetAmount: "", targetDate: "", linkedAccountId: "", icon: "target", color: "#3563e9" });
      load();
    } catch (error) {
      showToast(getApiErrorMessage(error, "Unable to save goal"), "error");
    }
  };

  const contribute = async (id: string) => {
    const amount = window.prompt("Contribution amount");
    if (!amount || Number(amount) <= 0) return;
    try {
      await financeService.contributeGoal(id, { amount: Number(amount) });
      showToast("Goal contribution saved");
      load();
    } catch (error) {
      showToast(getApiErrorMessage(error, "Unable to save contribution"), "error");
    }
  };

  return (
    <AppShell title="Goals">
      <div className="mb-6 grid gap-4 md:grid-cols-3">
        <Stat title="Active goals" value={String(goals.length)} />
        <Stat title="Total saved" value={formatCurrency(totalSaved)} />
        <Stat title="Completed" value={String(goals.filter((goal) => goal.status === "COMPLETED").length)} />
      </div>

      <div className="grid gap-6 xl:grid-cols-[340px_1fr]">
        <SectionCard title="Create goal">
          <form className="grid gap-4" onSubmit={saveGoal}>
            <Field label="Name" required>
              <input value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} className="app-input" />
            </Field>
            <Field label="Target amount" required>
              <input type="number" value={form.targetAmount} onChange={(event) => setForm((current) => ({ ...current, targetAmount: event.target.value }))} className="app-input" />
            </Field>
            <Field label="Target date">
              <input type="date" value={form.targetDate} onChange={(event) => setForm((current) => ({ ...current, targetDate: event.target.value }))} className="app-input" />
            </Field>
            <Field label="Linked account">
              <select value={form.linkedAccountId} onChange={(event) => setForm((current) => ({ ...current, linkedAccountId: event.target.value }))} className="app-select">
                <option value="">Optional linked account</option>
                {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
              </select>
            </Field>
            <button className="app-button-primary">Save goal</button>
          </form>
        </SectionCard>

        <SectionCard title="Savings progress">
          {loading ? (
            <div className="grid gap-4 md:grid-cols-2">
              <LoadingBlock className="h-52 w-full" />
              <LoadingBlock className="h-52 w-full" />
            </div>
          ) : !goals.length ? (
            <EmptyState title="No goals yet" message="Create a savings goal to start tracking progress." />
          ) : (
            <div className="grid gap-4 md:grid-cols-2">
              {goals.map((goal) => (
                <div key={goal.id} className="app-panel rounded-xl p-5">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <p className="text-lg font-semibold text-slate-900">{goal.name}</p>
                      <p className="mt-1 text-sm text-slate-500">Due: {goal.targetDate || "Flexible"}</p>
                    </div>
                    <button onClick={() => contribute(goal.id)} className="app-button-secondary">Add contribution</button>
                  </div>
                  <p className="mt-4 text-sm text-slate-500">{formatCurrency(goal.currentAmount)} of {formatCurrency(goal.targetAmount)}</p>
                  <div className="mt-3 h-2 rounded-full bg-slate-200">
                    <div className="h-2 rounded-full bg-slate-900" style={{ width: `${Math.min(goal.progressPercentage, 100)}%` }} />
                  </div>
                  <p className="mt-3 text-sm font-medium text-slate-700">{goal.progressPercentage.toFixed(0)}% complete</p>
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
