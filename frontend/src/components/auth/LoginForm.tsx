import { useState, type FormEvent } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import styles from './AuthForms.module.css';


interface LoginFormProps {
  onSuccess: () => void;
  onSwitchToRegister: () => void;
  onForgotPassword: () => void;
}

export function LoginForm({ onSuccess, onSwitchToRegister, onForgotPassword }: LoginFormProps) {
  const { login, error, clearError } = useAuth();


  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });
  const [isLoading, setIsLoading] = useState(false);
  const [localError, setLocalError] = useState('');


  const handleChange = (field: string, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setLocalError('');
    clearError();
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLocalError('');

    if (!formData.email || !formData.password) {
      setLocalError('Заполните все поля');
      return;
    }

    setIsLoading(true);
    try {
      await login({
        email: formData.email,
        password: formData.password,
      });
      onSuccess();
    } catch {
      
    } finally {
      setIsLoading(false);
    }
  };

  const displayError = localError || error;

  return (
    <form onSubmit={handleSubmit} className={styles.form}>
      <Input
        label="Электронная почта"
        type="email"
        placeholder="ivan@example.com"
        value={formData.email}
        onChange={(e) => handleChange('email', e.target.value)}
        autoComplete="email"
      />

      <Input
        label="Пароль"
        type="password"
        placeholder="Введите пароль"
        value={formData.password}
        onChange={(e) => handleChange('password', e.target.value)}
        autoComplete="current-password"
      />

      {displayError && (
        <div className={styles.errorBlock}>
          {displayError}
        </div>
      )}

      <Button type="submit" size="lg" isLoading={isLoading} style={{ width: '100%' }}>
        Войти
      </Button>

      <p className={styles.switchText}>
        <button type="button" className={styles.switchLink} onClick={onForgotPassword}
          style={{ fontSize: '0.8rem', opacity: 0.8 }}>
          Забыли пароль?
        </button>
      </p>

      <p className={styles.switchText}>
        Нет аккаунта?{' '}
        <button type="button" className={styles.switchLink} onClick={onSwitchToRegister}>
          Зарегистрироваться
        </button>
      </p>
    </form>
  );
}
