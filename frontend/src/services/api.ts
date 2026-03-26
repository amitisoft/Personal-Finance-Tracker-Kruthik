import axios from "axios";

const STORAGE_KEY = "finance-tracker-auth";

type StoredAuth = {
  accessToken?: string;
  refreshToken?: string;
  user?: unknown;
};

type RetryableConfig = {
  _retry?: boolean;
  headers?: Record<string, string>;
};

function getApiBaseUrl() {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();
  if (!configuredBaseUrl) {
    return "/api";
  }

  const normalizedBaseUrl = configuredBaseUrl.replace(/\/+$/, "");
  return normalizedBaseUrl.endsWith("/api") ? normalizedBaseUrl : `${normalizedBaseUrl}/api`;
}

const api = axios.create({
  baseURL: getApiBaseUrl(),
});

function readAuthStorage() {
  const sessionValue = sessionStorage.getItem(STORAGE_KEY);
  const localValue = localStorage.getItem(STORAGE_KEY);
  const rawValue = sessionValue || localValue;

  if (!rawValue) {
    return null;
  }

  try {
    return JSON.parse(rawValue) as StoredAuth;
  } catch {
    return null;
  }
}

function persistAuth(auth: StoredAuth | null) {
  if (auth) {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
  } else {
    sessionStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem(STORAGE_KEY);
  }
}

function redirectToLogin() {
  persistAuth(null);
  if (window.location.pathname !== "/login") {
    window.location.href = "/login";
  }
}

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken() {
  const auth = readAuthStorage();
  if (!auth?.refreshToken) {
    redirectToLogin();
    return null;
  }

  if (!refreshPromise) {
    refreshPromise = axios
      .post(`${getApiBaseUrl()}/auth/refresh`, { refreshToken: auth.refreshToken })
      .then((response) => {
        const nextAuth: StoredAuth = {
          accessToken: response.data.accessToken,
          refreshToken: response.data.refreshToken,
          user: response.data.user,
        };
        persistAuth(nextAuth);
        return nextAuth.accessToken || null;
      })
      .catch(() => {
        redirectToLogin();
        return null;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

api.interceptors.request.use((config) => {
  const auth = readAuthStorage();
  if (auth?.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status as number | undefined;
    const originalRequest = error.config as RetryableConfig | undefined;
    const isAuthEndpoint = typeof error.config?.url === "string" && error.config.url.includes("/auth/");

    if ((status === 401 || status === 403) && originalRequest && !originalRequest._retry && !isAuthEndpoint) {
      originalRequest._retry = true;
      const refreshedAccessToken = await refreshAccessToken();
      if (refreshedAccessToken) {
        originalRequest.headers = originalRequest.headers || {};
        originalRequest.headers.Authorization = `Bearer ${refreshedAccessToken}`;
        return api(originalRequest);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
