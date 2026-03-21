import { useEffect, useState } from "react";
import AppShell from "../components/AppShell";
import SectionCard from "../components/SectionCard";
import { financeService } from "../services/financeService";
import { Account, Category, RecurringItem } from "../types/api";
import { useToast } from "../context/ToastContext";
import { useCurrency } from "../hooks/useCurrency";
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

export default function RecurringPage() {
  const [items, setItems] = useState<RecurringItem[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [form, setForm] = useState({ title: "", type: "EXPENSE", amount: "", categoryId: "", accountId: "", frequency: "MONTHLY", startDate: new Date().toISOString().slice(0, 10), endDate: "", autoCreateTransaction: true, paused: false });
  const { showToast } = useToast();
  const { formatCurrency } = useCurrency();

  const load = () => financeService.getRecurring().then(setItems).catch((error) => showToast(getApiErrorMessage(error, "Unable to load recurring items"), "error"));

  useEffect(() => {
    load();
    financeService.getAccounts().then(setAccounts).catch((error) => showToast(getApiErrorMessage(error, "Unable to load accounts"), "error"));
    financeService.getCategories().then(setCategories).catch((error) => showToast(getApiErrorMessage(error, "Unable to load categories"), "error"));
  }, []);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!form.title || !form.amount || !form.accountId) {
      showToast("Title, amount, and account are required", "error");
      return;
    }
    try {
      await financeService.saveRecurring({ ...form, amount: Number(form.amount) });
      showToast("Recurring item saved");
      setForm((current) => ({ ...current, title: "", amount: "" }));
      load();
    } catch (error) {
      showToast(getApiErrorMessage(error, "Unable to save recurring item"), "error");
    }
  };

  return (
    <AppShell title="Recurring transactions">
      <div className="grid gap-6 xl:grid-cols-[380px_1fr]">
        <SectionCard title="New recurring item">
          <form className="grid gap-4" onSubmit={submit}>
            <Field label="Title" required>
              <input value={form.title} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} placeholder="Netflix" className="app-input" />
            </Field>
            <div className="grid grid-cols-2 gap-4">
              <Field label="Type" required>
                <select value={form.type} onChange={(event) => setForm((current) => ({ ...current, type: event.target.value }))} className="app-select">
                  <option value="EXPENSE">Expense</option>
                  <option value="INCOME">Income</option>
                </select>
              </Field>
              <Field label="Amount" required>
                <input type="number" value={form.amount} onChange={(event) => setForm((current) => ({ ...current, amount: event.target.value }))} placeholder="Amount" className="app-input" />
              </Field>
            </div>
            <Field label="Account" required>
              <select value={form.accountId} onChange={(event) => setForm((current) => ({ ...current, accountId: event.target.value }))} className="app-select">
                <option value="">Choose account</option>
                {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
              </select>
            </Field>
            <Field label="Category">
              <select value={form.categoryId} onChange={(event) => setForm((current) => ({ ...current, categoryId: event.target.value }))} className="app-select">
                <option value="">Choose category</option>
                {categories.filter((item) => item.type === form.type).map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
              </select>
            </Field>
            <Field label="Frequency" required>
              <select value={form.frequency} onChange={(event) => setForm((current) => ({ ...current, frequency: event.target.value }))} className="app-select">
                <option value="DAILY">Daily</option>
                <option value="WEEKLY">Weekly</option>
                <option value="MONTHLY">Monthly</option>
                <option value="YEARLY">Yearly</option>
              </select>
            </Field>
            <Field label="Start date" required>
              <input type="date" value={form.startDate} onChange={(event) => setForm((current) => ({ ...current, startDate: event.target.value }))} className="app-input" />
            </Field>
            <button className="app-button-primary">Save recurring item</button>
          </form>
        </SectionCard>
        <SectionCard title="Upcoming bills and recurring income">
          <div className="space-y-4">
            {items.map((item) => (
              <div key={item.id} className="rounded-3xl bg-slate-50 p-5">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="text-lg font-semibold text-slate-800">{item.title}</p>
                    <p className="text-sm text-slate-500">{item.frequency} • next run {item.nextRunDate}</p>
                  </div>
                  <div className={`rounded-full px-3 py-1 text-xs font-semibold ${item.type === "EXPENSE" ? "bg-rose-100 text-rose-600" : "bg-emerald-100 text-emerald-600"}`}>
                    {formatCurrency(item.amount)}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </SectionCard>
      </div>
    </AppShell>
  );
}
