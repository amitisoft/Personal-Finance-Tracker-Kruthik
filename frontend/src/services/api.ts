import axios from "axios";

const STORAGE_KEY = "finance-tracker-auth";

const api = axios.create({
  baseURL: "/api",
});

function readAuthStorage() {
  const sessionValue = sessionStorage.getItem(STORAGE_KEY);
  const localValue = localStorage.getItem(STORAGE_KEY);
  const rawValue = sessionValue || localValue;

  if (!rawValue) {
    return null;
  }

  try {
    return JSON.parse(rawValue) as { accessToken?: string };
  } catch {
    return null;
  }
}

api.interceptors.request.use((config) => {
  const auth = readAuthStorage();
  if (auth?.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`;
  }
  return config;
});

export default api;
