export default function SimpleModal({ open, title, onClose, children }: { open: boolean; title: string; onClose: () => void; children: React.ReactNode }) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 px-4 py-6">
      <div className="app-card max-h-[90vh] w-full max-w-3xl overflow-auto rounded-2xl p-6 sm:p-8">
        <div className="mb-6 flex items-center justify-between gap-4">
          <h3 className="text-xl font-semibold text-slate-900">{title}</h3>
          <button onClick={onClose} className="app-button-secondary">Close</button>
        </div>
        {children}
      </div>
    </div>
  );
}
