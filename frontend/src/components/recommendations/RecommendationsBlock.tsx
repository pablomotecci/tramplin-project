import { Link } from 'react-router-dom';
import type { RecommendationSummary } from '../../types';
import styles from './RecommendationsBlock.module.css';

interface Props {
  recommendations: RecommendationSummary[];
}

export function RecommendationsBlock({ recommendations }: Props) {
  if (recommendations.length === 0) return null;

  return (
    <div className={styles.block}>
      <h4 className={styles.title}>
        <span
          className="material-symbols-rounded"
          style={{ fontSize: 18, color: 'var(--color-accent)' }}
        >
          recommend
        </span>
        Рекомендации ({recommendations.length})
      </h4>

      <div className={styles.list}>
        {recommendations.map((rec) => (
          <div key={rec.recommenderUserId} className={styles.card}>
            {rec.recommenderAvatarUrl ? (
              <img
                src={rec.recommenderAvatarUrl}
                alt={rec.recommenderName}
                className={styles.avatar}
              />
            ) : (
              <div className={styles.avatarPlaceholder}>
                {rec.recommenderName.charAt(0).toUpperCase()}
              </div>
            )}

            <div className={styles.body}>
              <div className={styles.headerRow}>
                <Link
                  to={`/applicant/${rec.recommenderUserId}`}
                  className={styles.name}
                >
                  {rec.recommenderName}
                </Link>
                <span className={styles.date}>
                  {new Date(rec.createdAt).toLocaleDateString('ru-RU', {
                    day: '2-digit',
                    month: '2-digit',
                    year: 'numeric',
                  })}
                </span>
              </div>
              {rec.message && <p className={styles.message}>{rec.message}</p>}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}