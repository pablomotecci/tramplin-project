import { useEffect, useCallback, type ReactNode } from 'react';
import styles from './Modal.module.css';


interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  children: ReactNode;
  title?: string;
}

export function Modal({ isOpen, onClose, children, title }: ModalProps) {
  // Закрытие по esc
  const handleEscape = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    },
    [onClose]
  );

  useEffect(() => {
    if (isOpen) {
      document.addEventListener('keydown', handleEscape);
      document.body.style.overflow = 'hidden'; // Блокируем скролл
    }
    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = ''; // Восстанавливаем скролл
    };
  }, [isOpen, handleEscape]);

  if (!isOpen) return null;

  return (
    <div className={styles.overlay} onClick={onClose}>
      {/* stopPropagation — клик внутри модалки не закрывает её */}
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <div className={styles.header}>
          {title && <h2 className={styles.title}>{title}</h2>}
          <button className={styles.closeBtn} onClick={onClose} aria-label="Закрыть">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path
                d="M15 5L5 15M5 5L15 15"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
              />
            </svg>
          </button>
        </div>
        <div className={styles.content}>{children}</div>
      </div>
    </div>
  );
}
