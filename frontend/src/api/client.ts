import axios from 'axios';
import type {
  ApiResponse,
  AuthResponse,
  RegisterRequest,
  LoginRequest,
} from '../types';

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor запросов, подставляем accessToken
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach((p) => {
    if (token) p.resolve(token);
    else p.reject(error);
  });
  failedQueue = [];
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/refresh')
    ) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({
            resolve: (token: string) => {
              originalRequest.headers.Authorization = `Bearer ${token}`;
              resolve(api(originalRequest));
            },
            reject,
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        clearTokens();
        isRefreshing = false;
        return Promise.reject(error);
      }

      try {
        const { data } = await axios.post<ApiResponse<AuthResponse>>(
          '/api/v1/auth/refresh',
          { refreshToken }
        );

        const newAccess = data.data!.accessToken;
        const newRefresh = data.data!.refreshToken;

        localStorage.setItem('accessToken', newAccess);
        localStorage.setItem('refreshToken', newRefresh);

        processQueue(null, newAccess);
        originalRequest.headers.Authorization = `Bearer ${newAccess}`;
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        clearTokens();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

// Очистка токенов
function clearTokens() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
}

// Auth API 

export async function registerUser(data: RegisterRequest): Promise<AuthResponse> {
  const response = await api.post<ApiResponse<AuthResponse>>('/auth/register', data);
  return response.data.data!;
}

export async function loginUser(data: LoginRequest): Promise<AuthResponse> {
  const response = await api.post<ApiResponse<AuthResponse>>('/auth/login', data);
  return response.data.data!;
}


export async function getCurrentUser(): Promise<AuthResponse> {
  const response = await api.get<ApiResponse<AuthResponse>>('/auth/me');
  return response.data.data!;
}

// Обработка ошибок
export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const serverError = error.response?.data as ApiResponse<unknown> | undefined;
    if (serverError?.error?.message) {
      return serverError.error.message;
    }
    if (!error.response) {
      return 'Сервер недоступен. Проверьте подключение.';
    }
  }
  return 'Произошла неизвестная ошибка';
}

export { clearTokens };

// СМЕНА ПАРОЛЯ!!!!!
export async function changePassword(oldPassword: string, newPassword: string): Promise<void> {
  await api.put('/auth/change-password', { oldPassword, newPassword });
}

export default api;