export default function SectionCard({
  title,
  eyebrow,
  action,
  children,
  className = "",
}: {
  title: string;
  eyebrow?: string;
  action?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <section className={`app-card rounded-2xl p-5 sm:p-6 ${className}`}>
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div>
          {eyebrow ? <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{eyebrow}</p> : null}
          <h3 className="mt-1 text-lg font-semibold text-slate-900">{title}</h3>
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}
