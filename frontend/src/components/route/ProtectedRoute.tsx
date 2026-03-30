import { Navigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import type { UserRole } from '../../types';

interface ProtectedRouteProps {
  children: React.ReactNode;
  // Какие роли допущены к этому маршруту.
  // Если не указано то достаточно просто быть авторизованным
  allowedRoles?: UserRole[];
}

export function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const { user, isLoading } = useAuth();

  // Пока идёт проверка токена — показывается индикатор загрузки
  if (isLoading) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '60vh',
        color: 'var(--color-text-secondary)',
        fontSize: '1.1rem',
      }}>
        Загрузка...
      </div>
    );
  }

  // Не авторизован — отправка на главную страницу
  if (!user) {
    return <Navigate to="/" replace />;
  }

  // Авторизован, но роль не подходит — показывается запрет
  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        height: '60vh',
        gap: '1rem',
      }}>
        <span style={{ fontSize: '3rem' }}>🚫</span>
        <h2 style={{ color: 'var(--color-text-primary)', margin: 0 }}>
          Доступ запрещён
        </h2>
        <p style={{ color: 'var(--color-text-secondary)', margin: 0 }}>
          У вашей роли нет доступа к этому разделу.
        </p>
      </div>
    );
  }

  return <>{children}</>;
}