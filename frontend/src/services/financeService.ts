import api from "./api";
import { Account, Budget, Category, Dashboard, FilteredReport, Goal, RecurringItem, TransactionPage } from "../types/api";

export const financeService = {
  getDashboard: async () => (await api.get<Dashboard>("/dashboard")).data,
  getTransactions: async (params: Record<string, string | number | undefined>) => (await api.get<TransactionPage>("/transactions", { params })).data,
  createTransaction: async (payload: unknown) => (await api.post("/transactions", payload)).data,
  updateTransaction: async (id: string, payload: unknown) => (await api.put(`/transactions/${id}`, payload)).data,
  deleteTransaction: async (id: string) => (await api.delete(`/transactions/${id}`)).data,
  getCategories: async () => (await api.get<Category[]>("/categories")).data,
  saveCategory: async (payload: unknown, id?: string) => id ? (await api.put(`/categories/${id}`, payload)).data : (await api.post("/categories", payload)).data,
  deleteCategory: async (id: string) => (await api.delete(`/categories/${id}`)).data,
  getAccounts: async () => (await api.get<Account[]>("/accounts")).data,
  saveAccount: async (payload: unknown, id?: string) => id ? (await api.put(`/accounts/${id}`, payload)).data : (await api.post("/accounts", payload)).data,
  transfer: async (payload: unknown) => (await api.post("/accounts/transfer", payload)).data,
  getBudgets: async (month: number, year: number) => (await api.get<Budget[]>("/budgets", { params: { month, year } })).data,
  saveBudget: async (payload: unknown, id?: string) => id ? (await api.put(`/budgets/${id}`, payload)).data : (await api.post("/budgets", payload)).data,
  deleteBudget: async (id: string) => (await api.delete(`/budgets/${id}`)).data,
  getGoals: async () => (await api.get<Goal[]>("/goals")).data,
  saveGoal: async (payload: unknown, id?: string) => id ? (await api.put(`/goals/${id}`, payload)).data : (await api.post("/goals", payload)).data,
  contributeGoal: async (id: string, payload: unknown, withdraw = false) => (await api.post(`/goals/${id}/${withdraw ? "withdraw" : "contribute"}`, payload)).data,
  getRecurring: async () => (await api.get<RecurringItem[]>("/recurring")).data,
  saveRecurring: async (payload: unknown, id?: string) => id ? (await api.put(`/recurring/${id}`, payload)).data : (await api.post("/recurring", payload)).data,
  deleteRecurring: async (id: string) => (await api.delete(`/recurring/${id}`)).data,
  getReportSummary: async (startDate: string, endDate: string) => (await api.get<FilteredReport>("/reports/summary", { params: { startDate, endDate } })).data,
  exportCsv: async (startDate: string, endDate: string) => (await api.get<string>("/reports/export", { params: { startDate, endDate } })).data,
};
