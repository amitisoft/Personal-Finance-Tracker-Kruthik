import api from "./api";
import { AuthResponse } from "../types/api";

export const authService = {
  register: async (payload: { email: string; password: string; displayName: string }) => {
    const { data } = await api.post<AuthResponse>("/auth/register", payload);
    return data;
  },
  login: async (payload: { email: string; password: string }) => {
    const { data } = await api.post<AuthResponse>("/auth/login", payload);
    return data;
  },
  refresh: async (refreshToken: string) => {
    const { data } = await api.post<AuthResponse>("/auth/refresh", { refreshToken });
    return data;
  },
};
