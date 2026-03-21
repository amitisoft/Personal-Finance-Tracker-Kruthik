export type UserSummary = {
  id: string;
  email: string;
  displayName: string;
};

export type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
};

export type Category = {
  id: string;
  name: string;
  type: "INCOME" | "EXPENSE";
  color?: string;
  icon?: string;
  isArchived: boolean;
};

export type Account = {
  id: string;
  name: string;
  type: string;
  openingBalance: number;
  currentBalance: number;
  institutionName?: string;
  createdAt?: string;
};

export type Transaction = {
  id: string;
  type: "INCOME" | "EXPENSE" | "TRANSFER";
  amount: number;
  date: string;
  accountId: string;
  accountName: string;
  categoryId?: string;
  categoryName?: string;
  merchant?: string;
  note?: string;
  paymentMethod?: string;
  tags: string[];
  createdAt?: string;
};

export type TransactionPage = {
  content: Transaction[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type Budget = {
  id: string;
  categoryId: string;
  categoryName: string;
  month: number;
  year: number;
  amount: number;
  spent: number;
  remaining: number;
  alertThresholdPercent: number;
  utilizationPercentage: number;
};

export type Goal = {
  id: string;
  name: string;
  targetAmount: number;
  currentAmount: number;
  targetDate?: string;
  linkedAccountId?: string;
  icon?: string;
  color?: string;
  status: string;
  progressPercentage: number;
};

export type RecurringItem = {
  id: string;
  title: string;
  type: "INCOME" | "EXPENSE" | "TRANSFER";
  amount: number;
  categoryId?: string;
  categoryName?: string;
  accountId?: string;
  accountName?: string;
  frequency: string;
  startDate: string;
  endDate?: string;
  nextRunDate: string;
  autoCreateTransaction: boolean;
  paused: boolean;
};

export type CategorySpendPoint = {
  category: string;
  amount: number;
};

export type TrendPoint = {
  label: string;
  income: number;
  expense: number;
};

export type AccountBalancePoint = {
  account: string;
  balance: number;
};

export type Dashboard = {
  summary: {
    income: number;
    expense: number;
    balance: number;
  };
  categorySpending: CategorySpendPoint[];
  incomeVsExpense: TrendPoint[];
  recentTransactions: Transaction[];
  upcomingRecurring: RecurringItem[];
  goals: Goal[];
  budgets: Budget[];
};

export type FilteredReport = {
  startDate: string;
  endDate: string;
  categorySpend: CategorySpendPoint[];
  incomeVsExpense: TrendPoint[];
  accountBalances: AccountBalancePoint[];
};
