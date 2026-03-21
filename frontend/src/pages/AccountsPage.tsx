import { useEffect, useState } from "react";
import AppShell from "../components/AppShell";
import SectionCard from "../components/SectionCard";
import { financeService } from "../services/financeService";
import { Account } from "../types/api";
import { useCurrency } from "../hooks/useCurrency";
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

export default function AccountsPage() {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [form, setForm] = useState({ name: "", type: "BANK", openingBalance: "", institutionName: "" });
  const { formatCurrency } = useCurrency();
  const { showToast } = useToast();

  const load = () => financeService.getAccounts().then(setAccounts).catch((error) => showToast(getApiErrorMessage(error, "Unable to load accounts"), "error"));

  useEffect(() => { load(); }, []);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!form.name || !form.openingBalance) {
      showToast("Account name and balance are required", "error");
      return;
    }
    try {
      await financeService.saveAccount({ ...form, openingBalance: Number(form.openingBalance) });
      showToast("Account saved");
      setForm({ name: "", type: "BANK", openingBalance: "", institutionName: "" });
      load();
    } catch (error) {
      showToast(getApiErrorMessage(error, "Unable to save account"), "error");
    }
  };

  return (
    <AppShell title="Accounts">
      <div className="grid gap-6 xl:grid-cols-[360px_1fr]">
        <SectionCard title="Create account">
          <form className="grid gap-4" onSubmit={submit}>
            <Field label="Account name" required>
              <input value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} placeholder="Primary bank" className="app-input" />
            </Field>
            <Field label="Account type" required>
              <select value={form.type} onChange={(event) => setForm((current) => ({ ...current, type: event.target.value }))} className="app-select">
                <option value="BANK">Bank</option>
                <option value="CREDIT_CARD">Credit card</option>
                <option value="CASH">Cash</option>
                <option value="SAVINGS">Savings</option>
              </select>
            </Field>
            <Field label="Opening balance" required>
              <input type="number" value={form.openingBalance} onChange={(event) => setForm((current) => ({ ...current, openingBalance: event.target.value }))} placeholder="Opening balance" className="app-input" />
            </Field>
            <Field label="Institution name">
              <input value={form.institutionName} onChange={(event) => setForm((current) => ({ ...current, institutionName: event.target.value }))} placeholder="Institution name" className="app-input" />
            </Field>
            <button className="app-button-primary">Save account</button>
          </form>
        </SectionCard>
        <SectionCard title="Balances by account">
          <div className="grid gap-4 md:grid-cols-2">
            {accounts.map((account) => (
              <div key={account.id} className="rounded-3xl bg-slate-50 p-5">
                <p className="text-sm uppercase tracking-[0.22em] text-slate-400">{account.type.replace("_", " ")}</p>
                <p className="mt-2 text-xl font-semibold text-slate-900">{account.name}</p>
                <p className="mt-4 text-3xl font-semibold text-brand-700">{formatCurrency(account.currentBalance)}</p>
                <p className="mt-2 text-sm text-slate-500">{account.institutionName || "No institution specified"}</p>
              </div>
            ))}
          </div>
        </SectionCard>
      </div>
    </AppShell>
  );
}
