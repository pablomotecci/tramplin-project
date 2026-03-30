import { Button } from '../ui/Button';
import styles from './AuthForms.module.css';

interface ForgotPasswordFormProps {
  onBackToLogin: () => void;
}

export function ForgotPasswordForm({ onBackToLogin }: ForgotPasswordFormProps) {
  return (
    <div className={styles.form}>
      <div style={{
        display: 'flex', flexDirection: 'column', alignItems: 'center',
        gap: '1rem', padding: '0.5rem 0',
      }}>
        <span className="material-symbols-rounded" style={{
          fontSize: '48px', color: 'var(--color-accent)', opacity: 0.8,
        }}>
          lock_reset
        </span>

        <p style={{
          textAlign: 'center', fontSize: '0.95rem',
          color: 'var(--color-text)', lineHeight: 1.6,
        }}>
          Сброс пароля выполняется <strong>куратором платформы</strong>.
        </p>

        <div style={{
          width: '100%', padding: '1rem',
          background: 'var(--color-bg-subtle)', borderRadius: 'var(--radius-md)',
        }}>
          <p style={{
            fontSize: '0.85rem', color: 'var(--color-text-secondary)',
            lineHeight: 1.6, margin: 0,
          }}>
            <strong style={{ color: 'var(--color-text)' }}>Как восстановить доступ:</strong><br />
            1. Обратитесь к куратору платформы по email или через контакты вуза<br />
            2. Куратор сбросит пароль и выдаст вам временный<br />
            3. После входа смените пароль в настройках личного кабинета
          </p>
        </div>

        <p style={{
          fontSize: '0.8rem', color: 'var(--color-text-muted)',
          textAlign: 'center',
        }}>
          Это обеспечивает безопасность — только верифицированный сотрудник может подтвердить вашу личность.
        </p>
      </div>

      <Button
        type="button" size="lg" variant="secondary"
        onClick={onBackToLogin}
        style={{ width: '100%' }}
      >
        ← Вернуться ко входу
      </Button>
    </div>
  );
}