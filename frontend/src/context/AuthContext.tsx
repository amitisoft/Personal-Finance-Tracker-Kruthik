import { createContext, useContext, useEffect, useState } from "react";
import { authService } from "../services/authService";
import { AuthResponse, UserSummary } from "../types/api";

type AuthState = {
  accessToken: string;
  refreshToken: string;
  user: UserSummary;
};

type AuthContextValue = {
  auth: AuthState | null;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const STORAGE_KEY = "finance-tracker-auth";

function toAuthState(payload: AuthResponse): AuthState {
  return {
    accessToken: payload.accessToken,
    refreshToken: payload.refreshToken,
    user: payload.user,
  };
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [auth, setAuth] = useState<AuthState | null>(null);

  useEffect(() => {
    localStorage.removeItem(STORAGE_KEY);
    const saved = sessionStorage.getItem(STORAGE_KEY);
    if (saved) {
      setAuth(JSON.parse(saved));
    }
  }, []);

  const persist = (state: AuthState | null) => {
    setAuth(state);
    if (state) {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } else {
      sessionStorage.removeItem(STORAGE_KEY);
      localStorage.removeItem(STORAGE_KEY);
    }
  };

  const login = async (email: string, password: string) => {
    const response = await authService.login({ email, password });
    persist(toAuthState(response));
  };

  const register = async (displayName: string, email: string, password: string) => {
    const response = await authService.register({ displayName, email, password });
    persist(toAuthState(response));
  };

  const logout = () => persist(null);

  return <AuthContext.Provider value={{ auth, login, register, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
