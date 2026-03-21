import {
  Bell,
  CreditCard,
  FolderKanban,
  Goal,
  LayoutDashboard,
  Menu,
  Repeat,
  Search,
  Wallet,
  X,
  BadgeIndianRupee,
  BarChart3,
  LogOut,
} from "lucide-react";
import { useState } from "react";
import { NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const links = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard },
  { to: "/transactions", label: "Transactions", icon: BadgeIndianRupee },
  { to: "/budgets", label: "Budgets", icon: FolderKanban },
  { to: "/goals", label: "Goals", icon: Goal },
  { to: "/reports", label: "Reports", icon: BarChart3 },
  { to: "/accounts", label: "Accounts", icon: Wallet },
  { to: "/categories", label: "Categories", icon: CreditCard },
  { to: "/recurring", label: "Recurring", icon: Repeat },
] as const;

export default function AppShell({ title, children }: { title: string; children: React.ReactNode }) {
  const { auth, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleLogout = () => {
    setMobileOpen(false);
    logout();
  };

  return (
    <div className="min-h-screen bg-slate-100 text-slate-900">
      <div className="flex min-h-screen">
        <aside className="hidden h-screen w-64 shrink-0 flex-col overflow-y-auto border-r border-slate-200 bg-slate-900 px-5 py-6 text-slate-100 lg:fixed lg:inset-y-0 lg:left-0 lg:flex">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">Finance Tracker</p>
            <h1 className="mt-3 text-2xl font-semibold">Personal Finance</h1>
          </div>
          <nav className="mt-8 space-y-1.5">
            {links.map((link) => {
              const Icon = link.icon;
              return (
                <NavLink
                  key={link.to}
                  to={link.to}
                  end={link.to === "/"}
                  className={({ isActive }) => `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition ${isActive ? "bg-slate-800 text-white" : "text-slate-300 hover:bg-slate-800/70 hover:text-white"}`}
                >
                  <Icon size={18} />
                  <span>{link.label}</span>
                </NavLink>
              );
            })}
          </nav>
          <div className="mt-auto pb-4 pt-8">
            <div className="rounded-xl border border-slate-800 bg-slate-950 p-4">
              <p className="text-sm font-semibold text-white">{auth?.user.displayName}</p>
              <p className="mt-1 text-xs text-slate-400">{auth?.user.email}</p>
              <button onClick={logout} className="app-button-secondary mt-4 w-full border-slate-700 bg-slate-900 text-slate-200 hover:bg-slate-800">
                Logout
              </button>
            </div>
          </div>
        </aside>

        <div className="flex-1 lg:ml-64">
          <header className="sticky top-0 z-30 border-b border-slate-200 bg-white">
            <div className="flex items-center gap-3 px-4 py-4 sm:px-6 lg:px-8">
              <button onClick={() => setMobileOpen((value) => !value)} className="app-button-secondary lg:hidden">
                {mobileOpen ? <X size={18} /> : <Menu size={18} />}
              </button>
              <div className="min-w-0 flex-1">
                <p className="text-sm text-slate-500">Overview</p>
                <h2 className="truncate text-2xl font-semibold text-slate-900">{title}</h2>
              </div>
              <div className="hidden max-w-md flex-1 items-center gap-2 rounded-xl border border-slate-300 bg-slate-50 px-3 py-2 lg:flex">
                <Search size={16} className="text-slate-400" />
                <input className="w-full bg-transparent text-sm outline-none placeholder:text-slate-400" placeholder="Search" />
              </div>
              <button className="app-button-secondary hidden md:inline-flex">
                <Bell size={16} />
              </button>
              <div className="flex items-center gap-3 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">
                <div className="flex h-9 w-9 items-center justify-center rounded-full bg-slate-900 text-sm font-semibold text-white">
                  {auth?.user.displayName?.slice(0, 1) || "U"}
                </div>
                <div className="hidden sm:block">
                  <p className="text-sm font-medium text-slate-900">{auth?.user.displayName}</p>
                  <p className="text-xs text-slate-500">User</p>
                </div>
              </div>
            </div>
          </header>

          {mobileOpen ? (
            <div className="fixed inset-0 z-40 lg:hidden">
              <div className="absolute inset-0 bg-slate-900/40" onClick={() => setMobileOpen(false)} />
              <div className="absolute bottom-4 left-0 top-4 w-[320px] max-w-[86vw] overflow-y-auto rounded-r-2xl bg-slate-900 px-4 pb-6 pt-4 text-slate-100 shadow-xl">
                <div>
                  <div className="flex items-center justify-between px-1 pb-4">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">Finance Tracker</p>
                      <h2 className="mt-2 text-xl font-semibold text-white">Personal Finance</h2>
                    </div>
                    <button onClick={() => setMobileOpen(false)} className="rounded-lg p-2 text-slate-300 hover:bg-slate-800 hover:text-white">
                      <X size={18} />
                    </button>
                  </div>

                  <nav className="mt-2 space-y-2">
                    {links.map((link) => {
                      const Icon = link.icon;
                      return (
                        <NavLink
                          key={link.to}
                          to={link.to}
                          end={link.to === "/"}
                          onClick={() => setMobileOpen(false)}
                          className={({ isActive }) => `flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-medium ${isActive ? "bg-slate-800 text-white" : "text-slate-300 hover:bg-slate-800/70 hover:text-white"}`}
                        >
                          <Icon size={18} />
                          {link.label}
                        </NavLink>
                      );
                    })}
                  </nav>

                  <div className="pb-4 pt-6">
                    <div className="rounded-xl border border-slate-800 bg-slate-950 p-4">
                      <p className="text-sm font-semibold text-white">{auth?.user.displayName}</p>
                      <p className="mt-1 text-xs text-slate-400">{auth?.user.email}</p>
                      <button onClick={handleLogout} className="mt-4 flex w-full items-center justify-center gap-2 rounded-xl border border-slate-700 bg-slate-900 px-4 py-3 text-sm font-medium text-white hover:bg-slate-800">
                        <LogOut size={16} />
                        Logout
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ) : null}

          <main className="px-4 py-6 sm:px-6 lg:px-8">
            {children}
          </main>
        </div>
      </div>
    </div>
  );
}
