import React from 'react';
import styles from './EmptyState.module.css';

interface EmptyStateProps {
  icon: string;
  title: string;            // основной текст
  description?: string;     // опциональное пояснение под заголовком
  action?: React.ReactNode;
}

export function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <div className={styles.empty}>
      <span className={`material-symbols-rounded ${styles.icon}`}>{icon}</span>
      <h3 className={styles.title}>{title}</h3>
      {description && <p className={styles.description}>{description}</p>}
      {action && <div className={styles.action}>{action}</div>}
    </div>
  );
}