import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { AuthModal } from '../auth/AuthModal';
import { Button } from '../ui/Button';
import styles from './Header.module.css';
import { getApplicantProfile } from '../../api/applicant';
import { getCompanyProfile } from '../../api/employer';

function useTheme() {
  const [theme, setTheme] = useState<'dark' | 'light'>(() => {
    return (localStorage.getItem('theme') as 'dark' | 'light') || 'dark';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggle = () => setTheme(prev => prev === 'dark' ? 'light' : 'dark');

  return { theme, toggle };
}

/*
 Шапка сайта с двумя состояниями:
  Не авторизован - отоброжение кнопки Вход и Регистрация
  Авторизован - ссылка на ЛК, иконка профиля, имя + кнопка ВЫЙТИ
*/


// Путь в ЛК по роли
function getDashboardPath(role: string): string {
  switch (role) {
    case 'APPLICANT': return '/profile';
    case 'EMPLOYER': return '/company';
    case 'CURATOR':
    case 'ADMIN': return '/curator';
    default: return '/';
  }
}

// Русское название роли
const roleLabels: Record<string, string> = {
  APPLICANT: 'Соискатель',
  EMPLOYER: 'Работодатель',
  CURATOR: 'Куратор',
  ADMIN: 'Администратор',
};

export function Header() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [authModal, setAuthModal] = useState<{
    isOpen: boolean;
    mode: 'login' | 'register';
  }>({ isOpen: false, mode: 'login' });

  const { theme, toggle: toggleTheme } = useTheme();

  const [avatarUrl, setAvatarUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!user) { setAvatarUrl(null); return; }

    if (user.role === 'APPLICANT') {
      getApplicantProfile()
        .then(p => setAvatarUrl(p.avatarUrl || null))
        .catch(() => {});
    } else if (user.role === 'EMPLOYER') {
      getCompanyProfile()
        .then(p => setAvatarUrl(p.logoUrl || null))
        .catch(() => {});
    }
  }, [user]);

  const openLogin = () => setAuthModal({ isOpen: true, mode: 'login' });
  const openRegister = () => setAuthModal({ isOpen: true, mode: 'register' });
  const closeModal = () => setAuthModal({ isOpen: false, mode: 'login' });

  return (
    <>
      <header className={styles.header}>
        <div className={styles.container}>
          {/* Логотип — ведёт на главную */}
          <Link to="/" className={styles.logo}>
            <span className="material-symbols-rounded" style={{ fontSize: '32px', fill: 'orange' }}>footprint</span>
            <span className={styles.logoText}>Трамплин</span>
          </Link>

          <div className={styles.actions}>
            <button
              onClick={toggleTheme}
              title={theme === 'dark' ? 'Светлая тема' : 'Тёмная тема'}
              style={{
                background: 'none', border: '1px solid var(--color-border)',
                borderRadius: '50%', width: 36, height: 36,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                cursor: 'pointer', color: 'var(--color-text-secondary)',
                transition: 'border-color 0.2s, color 0.2s',
              }}
            >
              <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>
                {theme === 'dark' ? 'light_mode' : 'dark_mode'}
              </span>
            </button>
            {user ? (
              <div className={styles.profile}>
                {/* Клик на аватар/имя - переход в ЛК */}
                <div
                  className={styles.profileLink}
                  onClick={() => navigate(getDashboardPath(user.role))}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') navigate(getDashboardPath(user.role));
                  }}
                >
                  <div className={styles.avatar}>
                    {avatarUrl
                      ? <img src={avatarUrl} alt="" style={{
                          width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover',
                        }} />
                      : (user.displayName || user.email || '?').charAt(0).toUpperCase()
                    }
                  </div>
                  <div className={styles.profileInfo}>
                    <span className={styles.profileName}>{user.displayName}</span>
                    <span className={styles.profileRole}>
                      {roleLabels[user.role] || user.role}
                    </span>
                  </div>
                </div>

                <Button variant="ghost" size="sm" onClick={logout}>
                  Выйти
                </Button>
              </div>
            ) : (
              <div className={styles.authButtons}>
                <Button variant="ghost" size="sm" onClick={openLogin}>
                  Вход
                </Button>
                <Button variant="primary" size="sm" onClick={openRegister}>
                  Регистрация
                </Button>
              </div>
            )}
          </div>
        </div>
      </header>

      <AuthModal
        isOpen={authModal.isOpen}
        onClose={closeModal}
        initialMode={authModal.mode}
      />
    </>
  );
}