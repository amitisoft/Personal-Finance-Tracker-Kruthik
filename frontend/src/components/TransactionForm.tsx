import { useState } from "react";
import { Account, Category, Transaction } from "../types/api";

const emptyForm = {
  type: "EXPENSE",
  amount: "",
  date: new Date().toISOString().slice(0, 10),
  accountId: "",
  categoryId: "",
  transferAccountId: "",
  merchant: "",
  note: "",
  paymentMethod: "",
  tags: "",
};

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

export default function TransactionForm({
  accounts,
  categories,
  initial,
  onSubmit,
}: {
  accounts: Account[];
  categories: Category[];
  initial?: Transaction | null;
  onSubmit: (payload: Record<string, unknown>) => Promise<void>;
}) {
  const [form, setForm] = useState(() => initial ? {
    type: initial.type,
    amount: String(initial.amount),
    date: initial.date,
    accountId: initial.accountId,
    categoryId: initial.categoryId || "",
    transferAccountId: "",
    merchant: initial.merchant || "",
    note: initial.note || "",
    paymentMethod: initial.paymentMethod || "",
    tags: initial.tags.join(", "),
  } : emptyForm);
  const [error, setError] = useState("");

  const typeCategories = categories.filter((category) => category.type === (form.type === "INCOME" ? "INCOME" : "EXPENSE"));
  const handleChange = (field: string, value: string) => setForm((current) => ({ ...current, [field]: value }));

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!form.amount || Number(form.amount) <= 0 || !form.accountId || !form.date) {
      setError("Amount, date, and account are required.");
      return;
    }
    if (form.type !== "TRANSFER" && !form.categoryId) {
      setError("Choose a category for income or expense.");
      return;
    }
    if (form.type === "TRANSFER" && !form.transferAccountId) {
      setError("Choose a destination account for transfers.");
      return;
    }
    setError("");
    await onSubmit({
      ...form,
      amount: Number(form.amount),
      tags: form.tags ? form.tags.split(",").map((tag) => tag.trim()).filter(Boolean) : [],
    });
  };

  return (
    <form className="grid gap-5" onSubmit={submit}>
      <div className="grid gap-4 sm:grid-cols-3">
        <Field label="Type" required>
          <select value={form.type} onChange={(event) => handleChange("type", event.target.value)} className="app-select">
            <option value="EXPENSE">Expense</option>
            <option value="INCOME">Income</option>
            <option value="TRANSFER">Transfer</option>
          </select>
        </Field>
        <Field label="Amount" required>
          <input value={form.amount} onChange={(event) => handleChange("amount", event.target.value)} type="number" min="0" step="0.01" className="app-input" />
        </Field>
        <Field label="Date" required>
          <input value={form.date} onChange={(event) => handleChange("date", event.target.value)} type="date" className="app-input" />
        </Field>
        <Field label="Account" required>
          <select value={form.accountId} onChange={(event) => handleChange("accountId", event.target.value)} className="app-select">
            <option value="">Select account</option>
            {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
          </select>
        </Field>
        {form.type !== "TRANSFER" ? (
          <Field label="Category" required>
            <select value={form.categoryId} onChange={(event) => handleChange("categoryId", event.target.value)} className="app-select">
              <option value="">Select category</option>
              {typeCategories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
            </select>
          </Field>
        ) : (
          <Field label="Destination account" required>
            <select value={form.transferAccountId} onChange={(event) => handleChange("transferAccountId", event.target.value)} className="app-select">
              <option value="">Select destination account</option>
              {accounts.filter((account) => account.id !== form.accountId).map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
            </select>
          </Field>
        )}
        <Field label="Merchant">
          <input value={form.merchant} onChange={(event) => handleChange("merchant", event.target.value)} className="app-input" />
        </Field>
        <Field label="Payment method">
          <input value={form.paymentMethod} onChange={(event) => handleChange("paymentMethod", event.target.value)} className="app-input" />
        </Field>
        <Field label="Tags">
          <input value={form.tags} onChange={(event) => handleChange("tags", event.target.value)} placeholder="family, groceries" className="app-input" />
        </Field>
        <div className="sm:col-span-3">
          <Field label="Note">
            <textarea value={form.note} onChange={(event) => handleChange("note", event.target.value)} rows={4} className="app-textarea" />
          </Field>
        </div>
      </div>
      {error ? <p className="text-sm font-medium text-rose-600">{error}</p> : null}
      <div className="flex justify-end">
        <button className="app-button-primary" type="submit">Save transaction</button>
      </div>
    </form>
  );
}
