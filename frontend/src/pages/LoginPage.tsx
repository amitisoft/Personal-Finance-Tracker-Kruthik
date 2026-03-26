import { useState } from "react";
import { Navigate } from "react-router-dom";
import { LockKeyhole, ShieldCheck } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { getApiErrorMessage } from "../utils/apiError";

function Field({ label, required = false, children }: { label: string; required?: boolean; children: React.ReactNode }) {
  return (
    <label className="grid gap-2 text-sm font-medium text-slate-700">
      <span>
        {label}
        {required ? <span className="ml-1 text-rose-600">*</span> : null}
      </span>
      {children}
    </label>
  );
}

export default function LoginPage() {
  const { auth, login, register } = useAuth();
  const { showToast } = useToast();
  const [isRegister, setIsRegister] = useState(false);
  const [form, setForm] = useState({ displayName: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  if (auth) return <Navigate to="/" replace />;

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!form.email || !form.password || (isRegister && !form.displayName)) {
      setError("Please complete the required fields.");
      return;
    }

    setLoading(true);
    setError("");
    try {
      if (isRegister) {
        await register(form.displayName, form.email, form.password);
        showToast("Account created successfully");
      } else {
        await login(form.email, form.password);
        showToast("Welcome back");
      }
    } catch (error) {
      const message = getApiErrorMessage(error, "Authentication failed. Check your credentials and backend connection.");
      setError(message);
      showToast(message, "error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-100 px-4 py-10">
      <div className="mx-auto grid min-h-[calc(100vh-5rem)] max-w-6xl items-center gap-10 lg:grid-cols-[1fr_440px]">
        <div className="hidden lg:block">
          <p className="text-sm font-semibold uppercase tracking-[0.22em] text-slate-500">Finance Tracker</p>
          <h1 className="mt-4 max-w-xl text-5xl font-semibold leading-tight text-slate-900">
            Personal finance, organized in one clean workspace.
          </h1>
          <p className="mt-5 max-w-xl text-lg text-slate-600">
            Track income and expenses, manage budgets, monitor savings goals, and review reports with a simple dashboard built for daily use.
          </p>
          <div className="mt-10 grid gap-4 sm:grid-cols-2">
            <div className="app-card rounded-2xl p-5">
              <div className="mb-4 inline-flex rounded-xl bg-slate-100 p-3 text-slate-700">
                <ShieldCheck size={20} />
              </div>
              <h2 className="text-lg font-semibold text-slate-900">Secure access</h2>
              <p className="mt-2 text-sm text-slate-500">Protected authentication with personal data scoped to your account.</p>
            </div>
            <div className="app-card rounded-2xl p-5">
              <div className="mb-4 inline-flex rounded-xl bg-slate-100 p-3 text-slate-700">
                <LockKeyhole size={20} />
              </div>
              <h2 className="text-lg font-semibold text-slate-900">Clear insights</h2>
              <p className="mt-2 text-sm text-slate-500">See balances, budgets, goals, and reports without clutter or distractions.</p>
            </div>
          </div>
        </div>

        <div className="app-card rounded-2xl p-6 sm:p-8">
          <p className="text-sm font-semibold uppercase tracking-[0.22em] text-slate-500">Secure access</p>
          <h2 className="mt-3 text-3xl font-semibold text-slate-900">{isRegister ? "Create account" : "Welcome back"}</h2>
          <p className="mt-2 text-sm text-slate-500">
            {isRegister ? "Create your account to start managing your finances." : "Sign in to continue to your dashboard."}
          </p>

          <form className="mt-8 grid gap-4" onSubmit={submit}>
            {isRegister ? (
              <Field label="Display name" required>
                <input
                  value={form.displayName}
                  onChange={(event) => setForm((current) => ({ ...current, displayName: event.target.value }))}
                  className="app-input"
                  placeholder="Enter your name"
                />
              </Field>
            ) : null}
            <Field label="Email" required>
              <input
                value={form.email}
                onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
                className="app-input"
                placeholder="Enter your email"
              />
            </Field>
            <Field label="Password" required>
              <input
                value={form.password}
                onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
                type="password"
                className="app-input"
                placeholder="Enter your password"
              />
            </Field>
            {error ? <p className="text-sm font-medium text-rose-600">{error}</p> : null}
            <button disabled={loading} className="app-button-primary w-full disabled:opacity-60">
              {loading ? "Please wait..." : isRegister ? "Create account" : "Log in"}
            </button>
          </form>

          <button onClick={() => setIsRegister((current) => !current)} className="mt-5 text-sm font-medium text-slate-700 hover:text-slate-900">
            {isRegister ? "Already have an account? Log in" : "Need an account? Sign up"}
          </button>
        </div>
      </div>
    </div>
  );
}
