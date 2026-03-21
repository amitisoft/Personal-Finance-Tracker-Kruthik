type CategoryKind = "INCOME" | "EXPENSE";

type CategoryStyle = {
  color: string;
  icon: string;
};

const expenseRules: Array<{ match: RegExp; style: CategoryStyle }> = [
  { match: /(food|grocery|dining|restaurant|meal)/i, style: { color: "#f59e0b", icon: "utensils" } },
  { match: /(rent|home|house|mortgage)/i, style: { color: "#ef4444", icon: "home" } },
  { match: /(transport|travel|fuel|uber|bus|metro|car)/i, style: { color: "#3b82f6", icon: "car" } },
  { match: /(health|medical|doctor|medicine)/i, style: { color: "#10b981", icon: "heart" } },
  { match: /(shopping|clothes|fashion)/i, style: { color: "#8b5cf6", icon: "bag" } },
  { match: /(bill|utility|electric|water|internet|phone|subscription)/i, style: { color: "#6366f1", icon: "receipt" } },
  { match: /(education|course|book|study)/i, style: { color: "#14b8a6", icon: "book" } },
];

const incomeRules: Array<{ match: RegExp; style: CategoryStyle }> = [
  { match: /(salary|payroll|job)/i, style: { color: "#22c55e", icon: "wallet" } },
  { match: /(freelance|client|project)/i, style: { color: "#0ea5e9", icon: "briefcase" } },
  { match: /(bonus|reward)/i, style: { color: "#f59e0b", icon: "gift" } },
  { match: /(investment|interest|dividend)/i, style: { color: "#8b5cf6", icon: "trending-up" } },
  { match: /(refund|cashback)/i, style: { color: "#14b8a6", icon: "refresh" } },
];

const defaultStyles: Record<CategoryKind, CategoryStyle> = {
  EXPENSE: { color: "#64748b", icon: "circle" },
  INCOME: { color: "#16a34a", icon: "wallet" },
};

export function getCategoryDefaults(name: string, type: CategoryKind): CategoryStyle {
  const rules = type === "EXPENSE" ? expenseRules : incomeRules;
  const normalized = name.trim();

  for (const rule of rules) {
    if (rule.match.test(normalized)) {
      return rule.style;
    }
  }

  return defaultStyles[type];
}
