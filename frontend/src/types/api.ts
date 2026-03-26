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
  accessRole?: string;
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
  accountId?: string;
  accountName?: string;
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
  linkedAccountName?: string;
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

export type NetWorthPoint = {
  label: string;
  netWorth: number;
};

export type NetWorthReport = {
  currentNetWorth: number;
  history: NetWorthPoint[];
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
  netWorth: NetWorthReport;
};

export type ForecastMonth = {
  currentBalance: number;
  projectedEndBalance: number;
  projectedIncome: number;
  projectedExpense: number;
  projectedNet: number;
  safeToSpend: number;
  daysRemaining: number;
  confidence: string;
  riskLevel: string;
  warning: string;
};

export type DailyForecastPoint = {
  date: string;
  projectedBalance: number;
};

export type DailyForecast = {
  currentBalance: number;
  projectedEndBalance: number;
  projections: DailyForecastPoint[];
};

export type HealthBreakdown = {
  metric: string;
  score: number;
  detail: string;
};

export type HealthScore = {
  score: number;
  status: string;
  breakdown: HealthBreakdown[];
  suggestions: string[];
};

export type SavingsTrendPoint = {
  label: string;
  savings: number;
  savingsRate: number;
};

export type CategoryTrendPoint = {
  label: string;
  category: string;
  amount: number;
};

export type InsightHighlight = {
  title: string;
  message: string;
  tone: string;
};

export type InsightsResponse = {
  healthScore: HealthScore;
  highlights: InsightHighlight[];
  incomeExpenseTrend: TrendPoint[];
  savingsTrend: SavingsTrendPoint[];
  categoryTrends: CategoryTrendPoint[];
};

export type TrendReport = {
  incomeExpenseTrend: TrendPoint[];
  savingsTrend: SavingsTrendPoint[];
  categoryTrends: CategoryTrendPoint[];
};

export type Rule = {
  id: string;
  name: string;
  conditionJson: string;
  actionJson: string;
  isActive: boolean;
  createdAt?: string;
};

export type RulePayload = {
  name: string;
  conditionJson: string;
  actionJson: string;
  isActive: boolean;
};

export type AccountMember = {
  userId: string;
  email: string;
  displayName: string;
  role: "OWNER" | "EDITOR" | "VIEWER";
};

export type AccountMemberList = {
  accountId: string;
  accountName: string;
  members: AccountMember[];
};

export type AccountInvitePayload = {
  email: string;
  role: "OWNER" | "EDITOR" | "VIEWER";
};

export type AccountMemberUpdatePayload = {
  role: "OWNER" | "EDITOR" | "VIEWER";
};

export type AccountActivity = {
  id: string;
  accountId: string;
  actorName: string;
  actorEmail: string;
  action: string;
  resourceType: string;
  resourceName: string;
  details?: string;
  createdAt?: string;
};

export type AccountActivityFeed = {
  accountId: string;
  accountName: string;
  activities: AccountActivity[];
};
