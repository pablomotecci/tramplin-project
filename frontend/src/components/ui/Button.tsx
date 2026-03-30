import { type ButtonHTMLAttributes, type ReactNode } from 'react';
import styles from './Button.module.css';

/*
Варианты кнопок:
  primary  — оранжевая, для основных действий: Зарегистрироваться, Войти
  secondary — прозрачная с рамкой, для второстепенных
  ghost    — прозрачная без рамки, для текста

size:
  sm — маленькая (в шапке)
  md — средняя (в формах)
  lg — большая
*/

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
  children: ReactNode;
}

export function Button({
  variant = 'primary',
  size = 'md',
  isLoading = false,
  children,
  disabled,
  className = '',
  ...props
}: ButtonProps) {
  return (
    <button
      className={`${styles.button} ${styles[variant]} ${styles[size]} ${className}`}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? (
        <span className={styles.spinner} aria-label="Загрузка" />
      ) : (
        children
      )}
    </button>
  );
}
