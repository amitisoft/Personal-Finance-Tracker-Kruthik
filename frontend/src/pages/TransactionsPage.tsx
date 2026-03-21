import { useEffect, useMemo, useState } from "react";
import { Plus, Search } from "lucide-react";
import AppShell from "../components/AppShell";
import EmptyState from "../components/EmptyState";
import LoadingBlock from "../components/LoadingBlock";
import SectionCard from "../components/SectionCard";
import SimpleModal from "../components/SimpleModal";
import TransactionForm from "../components/TransactionForm";
import { useToast } from "../context/ToastContext";
import { useCurrency } from "../hooks/useCurrency";
import { financeService } from "../services/financeService";
import { Account, Category, Transaction, TransactionPage } from "../types/api";
import { getApiErrorMessage } from "../utils/apiError";

export default function TransactionsPage() {
  const [transactions, setTransactions] = useState<TransactionPage | null>(null);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [selected, setSelected] = useState<Transaction | null>(null);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [optionsLoading, setOptionsLoading] = useState(false);
  const [filters, setFilters] = useState({ search: "", type: "", page: 0 });
  const { showToast } = useToast();
  const { formatCurrency } = useCurrency();

  const loadTransactions = () => {
    setLoading(true);
    financeService.getTransactions({ page: filters.page, size: 10, search: filters.search || undefined, type: filters.type || undefined })
      .then(setTransactions)
      .catch((error) => showToast(getApiErrorMessage(error, "Unable to load transactions"), "error"))
      .finally(() => setLoading(false));
  };

  const loadOptions = async () => {
    setOptionsLoading(true);
    try {
      const [accountsData, categoriesData] = await Promise.all([
        financeService.getAccounts(),
        financeService.getCategories(),
      ]);
      setAccounts(accountsData);
      setCategories(categoriesData);
      return { accountsData, categoriesData };
    } catch (error) {
      showToast(getApiErrorMessage(error, "Unable to load accounts or categories"), "error");
      return { accountsData: [], categoriesData: [] };
    } finally {
      setOptionsLoading(false);
    }
  };

  useEffect(() => {
    loadTransactions();
  }, [filters.page, filters.search, filters.type]);

  useEffect(() => {
    loadOptions();
  }, []);

  const openCreateModal = async () => {
    setSelected(null);
    await loadOptions();
    setOpen(true);
  };

  const openEditModal = async (transaction: Transaction) => {
    setSelected(transaction);
    await loadOptions();
    setOpen(true);
  };

  const totals = useMemo(() => ({
    count: transactions?.content.length || 0,
    income: (transactions?.content || []).filter((item) => item.type === "INCOME").reduce((sum, item) => sum + item.amount, 0),
    expense: (transactions?.content || []).filter((item) => item.type === "EXPENSE").reduce((sum, item) => sum + item.amount, 0),
  }), [transactions]);

  const handleSubmit = async (payload: Record<string, unknown>) => {
    try {
      if (selected) {
        await financeService.updateTransaction(selected.id, payload);
        showToast("Transaction updated");
      } else {
        await financeService.createTransaction(payload);
        showToast("Transaction saved");
      }
      setOpen(false);
      setSelected(null);
      loadTransactions();
      loadOptions();
    } catch (error) {
      showToast(getApiErrorMessage(error, "Unable to save transaction"), "error");
      throw error;
    }
  };

  const remove = async (id: string) => {
    try {
      await financeService.deleteTransaction(id);
      showToast("Transaction deleted");
      loadTransactions();
    } catch (error) {
      showToast(getApiErrorMessage(error, "Unable to delete transaction"), "error");
    }
  };

  return (
    <AppShell title="Transactions">
      <div className="mb-6 grid gap-4 md:grid-cols-3">
        <SectionCard title="Records" className="p-5">
          <p className="text-3xl font-semibold text-slate-900">{totals.count}</p>
        </SectionCard>
        <SectionCard title="Income" className="p-5">
          <p className="text-3xl font-semibold text-emerald-600">{formatCurrency(totals.income)}</p>
        </SectionCard>
        <SectionCard title="Expenses" className="p-5">
          <p className="text-3xl font-semibold text-rose-600">{formatCurrency(totals.expense)}</p>
        </SectionCard>
      </div>

      <SectionCard
        title="Transaction list"
        action={<button onClick={openCreateModal} className="app-button-primary"><Plus size={16} /> Add transaction</button>}
      >
        <div className="mb-5 grid gap-4 lg:grid-cols-[1fr_220px_180px]">
          <div className="flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-3 py-2">
            <Search size={16} className="text-slate-400" />
            <input value={filters.search} onChange={(event) => setFilters((current) => ({ ...current, search: event.target.value, page: 0 }))} placeholder="Search merchant or note" className="w-full bg-transparent text-sm outline-none" />
          </div>
          <select value={filters.type} onChange={(event) => setFilters((current) => ({ ...current, type: event.target.value, page: 0 }))} className="app-select">
            <option value="">All types</option>
            <option value="INCOME">Income</option>
            <option value="EXPENSE">Expense</option>
            <option value="TRANSFER">Transfer</option>
          </select>
          <div className="flex items-center rounded-xl border border-slate-200 bg-slate-50 px-3 text-sm text-slate-500">
            Page {(transactions?.page || 0) + 1} of {transactions?.totalPages || 1}
          </div>
        </div>

        {loading ? (
          <div className="space-y-3">
            <LoadingBlock className="h-14 w-full" />
            <LoadingBlock className="h-14 w-full" />
            <LoadingBlock className="h-14 w-full" />
          </div>
        ) : !(transactions?.content.length) ? (
          <EmptyState title="No transactions found" message="Try a different filter or add a new transaction." action={<button onClick={openCreateModal} className="app-button-primary">Add transaction</button>} />
        ) : (
          <div className="overflow-auto rounded-xl border border-slate-200">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Merchant</th>
                  <th>Category</th>
                  <th>Account</th>
                  <th>Type</th>
                  <th>Amount</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {transactions.content.map((transaction) => (
                  <tr key={transaction.id}>
                    <td>{transaction.date}</td>
                    <td>
                      <div className="font-medium text-slate-900">{transaction.merchant || "Untitled"}</div>
                    </td>
                    <td>{transaction.categoryName || transaction.type}</td>
                    <td>{transaction.accountName}</td>
                    <td>{transaction.type}</td>
                    <td className={transaction.type === "EXPENSE" ? "text-rose-600" : "text-emerald-600"}>{formatCurrency(transaction.amount)}</td>
                    <td>
                      <div className="flex gap-2">
                        <button onClick={() => openEditModal(transaction)} className="app-button-secondary">Edit</button>
                        <button onClick={() => remove(transaction.id)} className="app-button-danger">Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="mt-5 flex justify-end gap-3">
          <button disabled={(transactions?.page || 0) === 0} onClick={() => setFilters((current) => ({ ...current, page: current.page - 1 }))} className="app-button-secondary">Previous</button>
          <button disabled={!transactions || transactions.page >= transactions.totalPages - 1} onClick={() => setFilters((current) => ({ ...current, page: current.page + 1 }))} className="app-button-secondary">Next</button>
        </div>
      </SectionCard>

      <SimpleModal open={open} title={selected ? "Edit transaction" : "Add transaction"} onClose={() => setOpen(false)}>
        {optionsLoading ? (
          <div className="space-y-3">
            <LoadingBlock className="h-12 w-full" />
            <LoadingBlock className="h-12 w-full" />
            <LoadingBlock className="h-12 w-full" />
          </div>
        ) : (
          <TransactionForm key={selected?.id || "new"} accounts={accounts} categories={categories} initial={selected} onSubmit={handleSubmit} />
        )}
      </SimpleModal>
    </AppShell>
  );
}
