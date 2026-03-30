import { useNavigate } from 'react-router-dom';

export default function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div style={{
      minHeight: 'calc(100vh - 72px)',
      display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center',
      gap: '1.5rem', padding: '2rem',
      textAlign: 'center',
    }}>
      <span className="material-symbols-rounded" style={{
        fontSize: '64px', color: 'var(--color-text-secondary)', opacity: 0.4,
      }}>
        explore_off
      </span>
      <h1 style={{
        fontSize: '3rem', fontWeight: 800, margin: 0,
        background: 'linear-gradient(135deg, var(--color-accent) 0%, #F5945B 100%)',
        WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
      }}>
        404
      </h1>
      <p style={{ fontSize: '1.1rem', color: 'var(--color-text-secondary)', margin: 0 }}>
        Страница не найдена
      </p>
      <button
        onClick={() => navigate('/')}
        style={{
          padding: '0.6rem 1.5rem', borderRadius: '10px',
          border: '1px solid var(--color-border)', background: 'var(--color-surface)',
          color: 'var(--color-text)', fontSize: '0.95rem', fontWeight: 600,
          cursor: 'pointer', transition: 'border-color 0.2s',
        }}
      >
        На главную
      </button>
    </div>
  );
}
