// Модальное окно авторизации

import { useState, useEffect } from 'react';
import { Modal } from '../ui/Modal';
import { RegisterForm } from './RegisterForm';
import { LoginForm } from './LoginForm';
import { ForgotPasswordForm } from './ForgotPasswordForm';
import { useAuth } from '../../hooks/useAuth';

type AuthMode = 'login' | 'register' | 'forgot';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialMode?: AuthMode;
}

export function AuthModal({ isOpen, onClose, initialMode = 'login' }: AuthModalProps) {
  const [mode, setMode] = useState<AuthMode>(initialMode);

  useEffect(() => {
    if (isOpen) setMode(initialMode);
  }, [initialMode, isOpen]);

  const { clearError } = useAuth();

  const switchToLogin = () => { clearError(); setMode('login'); };
  const switchToRegister = () => { clearError(); setMode('register'); };
  const switchToForgot = () => { clearError(); setMode('forgot'); };
  const handleClose = () => { clearError(); onClose(); };

  const titles: Record<AuthMode, string> = {
    login: 'Вход',
    register: 'Регистрация',
    forgot: 'Восстановление пароля',
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title={titles[mode]}>
      {mode === 'login' && (
        <LoginForm
          onSuccess={handleClose}
          onSwitchToRegister={switchToRegister}
          onForgotPassword={switchToForgot}
        />
      )}
      {mode === 'register' && (
        <RegisterForm onSuccess={handleClose} onSwitchToLogin={switchToLogin} />
      )}
      {mode === 'forgot' && (
        <ForgotPasswordForm onBackToLogin={switchToLogin} />
      )}
    </Modal>
  );
}