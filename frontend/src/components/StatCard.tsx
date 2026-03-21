export default function StatCard({
  label,
  value,
  helper,
  icon,
  className = "",
}: {
  label: string;
  value: string;
  helper: string;
  icon?: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={`app-card rounded-2xl p-5 ${className}`}>
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-slate-500">{label}</p>
          <p className="mt-3 text-3xl font-semibold text-slate-900">{value}</p>
          <p className="mt-2 text-sm text-slate-500">{helper}</p>
        </div>
        {icon ? <div className="rounded-xl bg-slate-100 p-3 text-slate-700">{icon}</div> : null}
      </div>
    </div>
  );
}
