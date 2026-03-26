import { useEffect, useMemo, useState } from "react";
import AppShell from "../components/AppShell";
import EmptyState from "../components/EmptyState";
import SectionCard from "../components/SectionCard";
import { useToast } from "../context/ToastContext";
import { financeService } from "../services/financeService";
import { Account, Category, Rule } from "../types/api";
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

type ConditionType = "merchantContains" | "type" | "accountId" | "minAmount";
type ActionType = "categoryId" | "tag" | "appendNote";

type RuleForm = {
  id?: string;
  name: string;
  conditionType: ConditionType;
  conditionValue: string;
  actionType: ActionType;
  actionValue: string;
  isActive: boolean;
};

const emptyForm: RuleForm = {
  name: "",
  conditionType: "merchantContains",
  conditionValue: "",
  actionType: "categoryId",
  actionValue: "",
  isActive: true,
};

function parseRule(rule: Rule): RuleForm {
  let conditionType: ConditionType = "merchantContains";
  let conditionValue = "";
  let actionType: ActionType = "categoryId";
  let actionValue = "";

  try {
    const condition = JSON.parse(rule.conditionJson) as Record<string, string | number>;
    if (typeof condition.type === "string") {
      conditionType = "type";
      conditionValue = condition.type;
    } else if (typeof condition.accountId === "string") {
      conditionType = "accountId";
      conditionValue = condition.accountId;
    } else if (typeof condition.minAmount === "number") {
      conditionType = "minAmount";
      conditionValue = String(condition.minAmount);
    } else if (typeof condition.merchantContains === "string") {
      conditionType = "merchantContains";
      conditionValue = condition.merchantContains;
    }
  } catch {
    conditionValue = rule.conditionJson;
  }

  try {
    const action = JSON.parse(rule.actionJson) as Record<string, string>;
    if (typeof action.tag === "string") {
      actionType = "tag";
      actionValue = action.tag;
    } else if (typeof action.appendNote === "string") {
      actionType = "appendNote";
      actionValue = action.appendNote;
    } else if (typeof action.categoryId === "string") {
      actionType = "categoryId";
      actionValue = action.categoryId;
    }
  } catch {
    actionValue = rule.actionJson;
  }

  return {
    id: rule.id,
    name: rule.name,
    conditionType,
    conditionValue,
    actionType,
    actionValue,
    isActive: rule.isActive,
  };
}

function buildConditionJson(form: RuleForm) {
  if (form.conditionType === "minAmount") {
    return JSON.stringify({ minAmount: Number(form.conditionValue) });
  }
  return JSON.stringify({ [form.conditionType]: form.conditionValue.trim() });
}

function buildActionJson(form: RuleForm) {
  return JSON.stringify({ [form.actionType]: form.actionValue.trim() });
}

function summarizeRule(rule: Rule, categories: Category[], accounts: Account[]) {
  const form = parseRule(rule);
  const categoryName = categories.find((item) => item.id === form.actionValue)?.name || "selected category";
  const accountName = accounts.find((item) => item.id === form.conditionValue)?.name || "selected account";

  const conditionLabel =
    form.conditionType === "merchantContains" ? `merchant contains "${form.conditionValue}"`
      : form.conditionType === "type" ? `transaction type is ${form.conditionValue}`
        : form.conditionType === "accountId" ? `account is ${accountName}`
          : `amount is at least ${form.conditionValue}`;

  const actionLabel =
    form.actionType === "categoryId" ? `set category to ${categoryName}`
      : form.actionType === "tag" ? `add tag "${form.actionValue}"`
        : `append note "${form.actionValue}"`;

  return `${conditionLabel}, then ${actionLabel}`;
}

export default function RulesPage() {
  const [rules, setRules] = useState<Rule[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [form, setForm] = useState<RuleForm>(emptyForm);
  const { showToast } = useToast();

  const loadRules = () =>
    financeService.getRules().then(setRules).catch((error) => showToast(getApiErrorMessage(error, "Unable to load rules"), "error"));

  useEffect(() => {
    loadRules();
    financeService.getCategories().then(setCategories).catch((error) => showToast(getApiErrorMessage(error, "Unable to load categories"), "error"));
    financeService.getAccounts().then(setAccounts).catch((error) => showToast(getApiErrorMessage(error, "Unable to load accounts"), "error"));
  }, []);

  const categoryOptions = useMemo(() => categories, [categories]);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!form.name.trim() || !form.conditionValue.trim() || !form.actionValue.trim()) {
      showToast("Rule name, condition, and action are required", "error");
      return;
    }

    try {
      await financeService.saveRule(
        {
          name: form.name.trim(),
          conditionJson: buildConditionJson(form),
          actionJson: buildActionJson(form),
          isActive: form.isActive,
        },
        form.id,
      );
      showToast(form.id ? "Rule updated" : "Rule created");
      setForm(emptyForm);
      loadRules();
    } catch (error) {
      showToast(getApiErrorMessage(error, "Unable to save rule"), "error");
    }
  };

  const remove = async (id: string) => {
    try {
      await financeService.deleteRule(id);
      showToast("Rule deleted");
      if (form.id === id) {
        setForm(emptyForm);
      }
      loadRules();
    } catch (error) {
      showToast(getApiErrorMessage(error, "Unable to delete rule"), "error");
    }
  };

  const toggleActive = async (rule: Rule) => {
    try {
      await financeService.saveRule(
        {
          name: rule.name,
          conditionJson: rule.conditionJson,
          actionJson: rule.actionJson,
          isActive: !rule.isActive,
        },
        rule.id,
      );
      showToast(!rule.isActive ? "Rule enabled" : "Rule disabled");
      if (form.id === rule.id) {
        setForm((current) => ({ ...current, isActive: !rule.isActive }));
      }
      loadRules();
    } catch (error) {
      showToast(getApiErrorMessage(error, "Unable to update rule"), "error");
    }
  };

  return (
    <AppShell title="Rules">
      <div className="grid gap-6 xl:grid-cols-[380px_1fr]">
        <SectionCard title={form.id ? "Edit rule" : "Create rule"} eyebrow="Automation">
          <form className="grid gap-4" onSubmit={submit}>
            <Field label="Rule name" required>
              <input
                value={form.name}
                onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                placeholder="Auto-tag grocery payments"
                className="app-input"
              />
            </Field>
            <Field label="Condition" required>
              <select
                value={form.conditionType}
                onChange={(event) => setForm((current) => ({ ...current, conditionType: event.target.value as ConditionType, conditionValue: "" }))}
                className="app-select"
              >
                <option value="merchantContains">Merchant contains</option>
                <option value="type">Transaction type</option>
                <option value="accountId">Account</option>
                <option value="minAmount">Minimum amount</option>
              </select>
            </Field>
            <Field
              label={
                form.conditionType === "merchantContains" ? "Merchant text"
                  : form.conditionType === "type" ? "Transaction type"
                    : form.conditionType === "accountId" ? "Account"
                      : "Minimum amount"
              }
              required
            >
              {form.conditionType === "type" ? (
                <select
                  value={form.conditionValue}
                  onChange={(event) => setForm((current) => ({ ...current, conditionValue: event.target.value }))}
                  className="app-select"
                >
                  <option value="">Choose type</option>
                  <option value="INCOME">Income</option>
                  <option value="EXPENSE">Expense</option>
                  <option value="TRANSFER">Transfer</option>
                </select>
              ) : form.conditionType === "accountId" ? (
                <select
                  value={form.conditionValue}
                  onChange={(event) => setForm((current) => ({ ...current, conditionValue: event.target.value }))}
                  className="app-select"
                >
                  <option value="">Choose account</option>
                  {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
                </select>
              ) : (
                <input
                  type={form.conditionType === "minAmount" ? "number" : "text"}
                  value={form.conditionValue}
                  onChange={(event) => setForm((current) => ({ ...current, conditionValue: event.target.value }))}
                  placeholder={form.conditionType === "minAmount" ? "5000" : "Swiggy"}
                  className="app-input"
                />
              )}
            </Field>
            <Field label="Action" required>
              <select
                value={form.actionType}
                onChange={(event) => setForm((current) => ({ ...current, actionType: event.target.value as ActionType, actionValue: "" }))}
                className="app-select"
              >
                <option value="categoryId">Assign category</option>
                <option value="tag">Add tag</option>
                <option value="appendNote">Append note</option>
              </select>
            </Field>
            <Field label={form.actionType === "categoryId" ? "Category" : form.actionType === "tag" ? "Tag" : "Note text"} required>
              {form.actionType === "categoryId" ? (
                <select
                  value={form.actionValue}
                  onChange={(event) => setForm((current) => ({ ...current, actionValue: event.target.value }))}
                  className="app-select"
                >
                  <option value="">Choose category</option>
                  {categoryOptions.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
                </select>
              ) : (
                <input
                  value={form.actionValue}
                  onChange={(event) => setForm((current) => ({ ...current, actionValue: event.target.value }))}
                  placeholder={form.actionType === "tag" ? "groceries" : "Marked by auto-rule"}
                  className="app-input"
                />
              )}
            </Field>
            <label className="flex items-center gap-3 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
              <input
                type="checkbox"
                checked={form.isActive}
                onChange={(event) => setForm((current) => ({ ...current, isActive: event.target.checked }))}
                className="h-4 w-4 rounded border-slate-300"
              />
              Rule is active
            </label>
            <div className="flex gap-3">
              <button className="app-button-primary flex-1">{form.id ? "Update rule" : "Save rule"}</button>
              {form.id ? (
                <button type="button" onClick={() => setForm(emptyForm)} className="app-button-secondary">
                  Cancel
                </button>
              ) : null}
            </div>
          </form>
        </SectionCard>

        <SectionCard title="Active automation rules" eyebrow="Workflow">
          {rules.length ? (
            <div className="space-y-4">
              {rules.map((rule) => (
                <div key={rule.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-5">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="text-base font-semibold text-slate-900">{rule.name}</p>
                        <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${rule.isActive ? "bg-emerald-100 text-emerald-700" : "bg-slate-200 text-slate-600"}`}>
                          {rule.isActive ? "Active" : "Paused"}
                        </span>
                      </div>
                      <p className="mt-2 text-sm text-slate-600">{summarizeRule(rule, categories, accounts)}</p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <button type="button" onClick={() => toggleActive(rule)} className="app-button-secondary">
                        {rule.isActive ? "Disable" : "Enable"}
                      </button>
                      <button type="button" onClick={() => setForm(parseRule(rule))} className="app-button-secondary">
                        Edit
                      </button>
                      <button type="button" onClick={() => remove(rule.id)} className="rounded-xl border border-rose-200 px-4 py-2 text-sm font-medium text-rose-600 transition hover:bg-rose-50">
                        Delete
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              title="No rules configured"
              message="Create your first automation rule to auto-assign categories, tags, or notes during transaction entry."
            />
          )}
        </SectionCard>
      </div>
    </AppShell>
  );
}
