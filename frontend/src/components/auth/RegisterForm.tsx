import { useState, type FormEvent } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import styles from './AuthForms.module.css';


interface RegisterFormProps {
  onSuccess: () => void;
  onSwitchToLogin: () => void;
}

export function RegisterForm({ onSuccess, onSwitchToLogin }: RegisterFormProps) {
  const { register, error, clearError } = useAuth();

  const [formData, setFormData] = useState({
    email: '',
    displayName: '',
    password: '',
    confirmPassword: '',
    role: '' as 'APPLICANT' | 'EMPLOYER' | '',
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

    //  Валидация на клиенте
    if (!formData.role) {
      setLocalError('Выберите роль');
      return;
    }
    if (!formData.email || !formData.displayName || !formData.password) {
      setLocalError('Заполните все поля');
      return;
    }
    if (formData.password.length < 8) {
      setLocalError('Пароль должен быть не менее 8 символов');
      return;
    }
    if (formData.password !== formData.confirmPassword) {
      setLocalError('Пароли не совпадают');
      return;
    }

    setIsLoading(true);
    try {
      await register({
        email: formData.email,
        displayName: formData.displayName,
        password: formData.password,
        role: formData.role,
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
      {/* Выбор роли */}
      <div className={styles.roleSection}>
        <div className={styles.roleToggle}>
          <button
            type="button"
            className={`${styles.roleBtn} ${formData.role === 'APPLICANT' ? styles.roleBtnActive : ''}`}
            onClick={() => handleChange('role', 'APPLICANT')}
          >
            <span className={styles.roleIcon}><span className="material-symbols-rounded">person_search</span></span>
            Соискатель
          </button>
          <button
            type="button"
            className={`${styles.roleBtn} ${formData.role === 'EMPLOYER' ? styles.roleBtnActive : ''}`}
            onClick={() => handleChange('role', 'EMPLOYER')}
          >
            <span className={styles.roleIcon}><span className="material-symbols-rounded">work</span></span>
            Работодатель
          </button>
        </div>
      </div>

      {/* Поля ввода */}
      <Input
        label="Имя и фамилия"
        placeholder="Влад Киреев"
        value={formData.displayName}
        onChange={(e) => handleChange('displayName', e.target.value)}
        autoComplete="name"
      />

      <Input
        label="Электронная почта"
        type="email"
        placeholder="vlad@mail.com"
        value={formData.email}
        onChange={(e) => handleChange('email', e.target.value)}
        autoComplete="email"
      />

      <Input
        label="Пароль"
        type="password"
        placeholder="Минимум 8 символов"
        value={formData.password}
        onChange={(e) => handleChange('password', e.target.value)}
        autoComplete="new-password"
      />

      <Input
        label="Подтвердите пароль"
        type="password"
        placeholder="Повторите пароль"
        value={formData.confirmPassword}
        onChange={(e) => handleChange('confirmPassword', e.target.value)}
        autoComplete="new-password"
      />

      {/* Ошибка */}
      {displayError && (
        <div className={styles.errorBlock}>
          {displayError}
        </div>
      )}

      {/* Кнопка регистрации */}
      <Button type="submit" size="lg" isLoading={isLoading} style={{ width: '100%' }}>
        Зарегистрироваться
      </Button>

      {/* Переключатель на логин */}
      <p className={styles.switchText}>
        Уже есть аккаунт?{' '}
        <button type="button" className={styles.switchLink} onClick={onSwitchToLogin}>
          Войти
        </button>
      </p>
    </form>
  );
}
