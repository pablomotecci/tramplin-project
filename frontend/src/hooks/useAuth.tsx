// Глобальное состояние авторизации и редирект в ЛК после логина
// Любой компонент вызывает useAuth() и получает: user, role, login, register, logout...


import { createContext, useContext, useState, useEffect, useCallback, type ReactNode, } from 'react';
import { useNavigate } from 'react-router-dom';
import type { UserResponse, RegisterRequest, LoginRequest, AuthResponse, UserRole, AccountStatus } from '../types';
import { registerUser, loginUser, getCurrentUser, getErrorMessage, clearTokens, } from '../api/client';

interface AuthContextType {
  user: UserResponse | null;
  isLoading: boolean;
  error: string | null;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Переход в зависимости от роли
function getDashboardPath(role: string): string {
  switch (role) {
    case 'APPLICANT': return '/profile';
    case 'EMPLOYER': return '/company';
    case 'CURATOR':
    case 'ADMIN': return '/curator';
    default: return '/';
  }
}

function authResponseToUser(res: AuthResponse): UserResponse {
  return {
    id: res.userId,
    email: res.email,
    displayName: res.displayName,
    role: res.role as UserRole,
    status: (res.status || 'ACTIVE') as AccountStatus,
    createdAt: '',
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const navigate = useNavigate();

  useEffect(() => {
    const initAuth = async () => {
      const token = localStorage.getItem('accessToken');
      if (token) {
        try {
          const authRes = await getCurrentUser();
          setUser(authResponseToUser(authRes));
        } catch {
          clearTokens();
        }
      }
      setIsLoading(false);
    };
    initAuth();
  }, []);

  const register = useCallback(async (data: RegisterRequest) => {
    setError(null);
    try {
      const response = await registerUser(data);
      localStorage.setItem('accessToken', response.accessToken);
      localStorage.setItem('refreshToken', response.refreshToken);
      const userObj = authResponseToUser(response);
      localStorage.setItem('user', JSON.stringify(userObj));
      setUser(userObj);
      // Редирект в ЛК по роли
      navigate(getDashboardPath(response.role));
    } catch (err) {
      const message = getErrorMessage(err);
      setError(message);
      throw new Error(message);
    }
  }, [navigate]);

  const login = useCallback(async (data: LoginRequest) => {
    setError(null);
    try {
      const response = await loginUser(data);
      localStorage.setItem('accessToken', response.accessToken);
      localStorage.setItem('refreshToken', response.refreshToken);
      const userObj = authResponseToUser(response);
      localStorage.setItem('user', JSON.stringify(userObj));
      setUser(userObj);
      navigate(getDashboardPath(response.role));
    } catch (err) {
      const message = getErrorMessage(err);
      setError(message);
      throw new Error(message);
    }
  }, [navigate]);

  const logout = useCallback(() => {
    clearTokens();
    setUser(null);
    setError(null);
    navigate('/');
  }, [navigate]);

  const clearError = useCallback(() => setError(null), []);

  return (
    <AuthContext.Provider
      value={{ user, isLoading, error, login, register, logout, clearError }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}