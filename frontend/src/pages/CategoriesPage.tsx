import { useEffect, useState } from "react";
import AppShell from "../components/AppShell";
import EmptyState from "../components/EmptyState";
import SectionCard from "../components/SectionCard";
import { useToast } from "../context/ToastContext";
import { financeService } from "../services/financeService";
import { Category } from "../types/api";
import { getCategoryDefaults } from "../utils/categoryDefaults";
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

export default function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [form, setForm] = useState({ name: "", type: "EXPENSE" as "EXPENSE" | "INCOME" });
  const { showToast } = useToast();

  const load = () => financeService.getCategories().then(setCategories).catch((error) => showToast(getApiErrorMessage(error, "Unable to load categories"), "error"));

  useEffect(() => {
    load();
  }, []);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!form.name.trim()) {
      showToast("Category name is required", "error");
      return;
    }

    const defaults = getCategoryDefaults(form.name, form.type);
    try {
      await financeService.saveCategory({
        name: form.name.trim(),
        type: form.type,
        color: defaults.color,
        icon: defaults.icon,
        isArchived: false,
      });

      showToast("Category saved");
      setForm({ name: "", type: "EXPENSE" });
      load();
    } catch (error) {
      showToast(getApiErrorMessage(error, "Unable to save category"), "error");
    }
  };

  return (
    <AppShell title="Categories">
      <div className="grid gap-6 xl:grid-cols-[340px_1fr]">
        <SectionCard title="Add category">
          <form className="grid gap-4" onSubmit={submit}>
            <Field label="Category name" required>
              <input
                value={form.name}
                onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                placeholder="Food"
                className="app-input"
              />
            </Field>
            <Field label="Type" required>
              <select
                value={form.type}
                onChange={(event) => setForm((current) => ({ ...current, type: event.target.value as "EXPENSE" | "INCOME" }))}
                className="app-select"
              >
                <option value="EXPENSE">Expense</option>
                <option value="INCOME">Income</option>
              </select>
            </Field>
            <button className="app-button-primary">Save category</button>
          </form>
        </SectionCard>

        <SectionCard title="Category library">
          {categories.length ? (
            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
              {categories.map((category) => (
                <div key={category.id} className="app-panel rounded-xl p-5">
                  <div className="flex items-center gap-3">
                    <span className="h-4 w-4 rounded-full" style={{ backgroundColor: category.color || "#64748b" }} />
                    <p className="font-semibold text-slate-900">{category.name}</p>
                  </div>
                  <p className="mt-3 text-sm text-slate-500">{category.type === "EXPENSE" ? "Expense" : "Income"}</p>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState title="No categories yet" message="Create your first category and the app will style it automatically." />
          )}
        </SectionCard>
      </div>
    </AppShell>
  );
}
