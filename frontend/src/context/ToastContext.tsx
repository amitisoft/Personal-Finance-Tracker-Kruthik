import { createContext, useContext, useState } from "react";
import { CheckCircle2, CircleAlert } from "lucide-react";

type Toast = { id: number; message: string; type: "success" | "error" };

type ToastContextValue = {
  showToast: (message: string, type?: Toast["type"]) => void;
};

const ToastContext = createContext<ToastContextValue | undefined>(undefined);

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const showToast = (message: string, type: Toast["type"] = "success") => {
    const id = Date.now();
    setToasts((current) => [...current, { id, message, type }]);
    setTimeout(() => setToasts((current) => current.filter((toast) => toast.id !== id)), 3000);
  };

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="fixed right-4 top-4 z-[60] space-y-3">
        {toasts.map((toast) => (
          <div key={toast.id} className="app-card flex min-w-[240px] items-center gap-3 rounded-xl px-4 py-3">
            <div className={`${toast.type === "success" ? "text-emerald-600" : "text-rose-600"}`}>
              {toast.type === "success" ? <CheckCircle2 size={18} /> : <CircleAlert size={18} />}
            </div>
            <p className="text-sm font-medium text-slate-700">{toast.message}</p>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) throw new Error("useToast must be used within ToastProvider");
  return context;
}
