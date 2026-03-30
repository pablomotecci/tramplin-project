import { useState, useEffect, useCallback, createContext, useContext, type ReactNode } from 'react';

type ToastType = 'success' | 'error' | 'info';

interface Toast {
  id: number;
  message: string;
  type: ToastType;
}

interface ToastContextValue {
  showToast: (message: string, type?: ToastType) => void;
}

const ToastContext = createContext<ToastContextValue>({ showToast: () => {} });

export function useToast() {
  return useContext(ToastContext);
}

let nextId = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const showToast = useCallback((message: string, type: ToastType = 'info') => {
    const id = nextId++;
    setToasts(prev => [...prev, { id, message, type }]);
  }, []);

  const removeToast = useCallback((id: number) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div style={{
        position: 'fixed', bottom: '1.5rem', right: '1.5rem',
        display: 'flex', flexDirection: 'column', gap: '0.5rem',
        zIndex: 9999, pointerEvents: 'none',
      }}>
        {toasts.map(toast => (
          <ToastItem key={toast.id} toast={toast} onRemove={removeToast} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

const COLORS: Record<ToastType, { bg: string; border: string; text: string }> = {
  success: { bg: 'rgba(52,211,153,0.12)', border: 'rgba(52,211,153,0.3)', text: '#059669' },
  error: { bg: 'rgba(248,113,113,0.12)', border: 'rgba(248,113,113,0.3)', text: '#dc2626' },
  info: { bg: 'rgba(96,165,250,0.12)', border: 'rgba(96,165,250,0.3)', text: '#2563eb' },
};

const ICONS: Record<ToastType, string> = {
  success: 'check_circle',
  error: 'error',
  info: 'info',
};

function ToastItem({ toast, onRemove }: { toast: Toast; onRemove: (id: number) => void }) {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    requestAnimationFrame(() => setVisible(true));
    const timer = setTimeout(() => {
      setVisible(false);
      setTimeout(() => onRemove(toast.id), 300);
    }, 3500);
    return () => clearTimeout(timer);
  }, [toast.id, onRemove]);

  const c = COLORS[toast.type];

  return (
    <div style={{
      pointerEvents: 'auto',
      display: 'flex', alignItems: 'center', gap: '0.6rem',
      padding: '0.75rem 1.25rem', borderRadius: '12px',
      background: c.bg, border: `1px solid ${c.border}`,
      color: c.text, fontSize: '0.9rem', fontWeight: 500,
      boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
      opacity: visible ? 1 : 0,
      transform: visible ? 'translateX(0)' : 'translateX(20px)',
      transition: 'opacity 0.3s ease, transform 0.3s ease',
      maxWidth: '400px',
    }}>
      <span className="material-symbols-rounded" style={{ fontSize: '20px' }}>
        {ICONS[toast.type]}
      </span>
      {toast.message}
    </div>
  );
}
