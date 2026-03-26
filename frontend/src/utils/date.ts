export function formatLocalDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function currentMonthRange() {
  const today = new Date();
  const start = formatLocalDate(new Date(today.getFullYear(), today.getMonth(), 1));
  const end = formatLocalDate(new Date(today.getFullYear(), today.getMonth() + 1, 0));
  return { start, end };
}
