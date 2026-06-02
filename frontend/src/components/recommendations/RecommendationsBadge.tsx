import { pluralize } from '../../utils/pluralize';

interface Props {
  count: number;
}

export function RecommendationsBadge({ count }: Props) {
  if (count === 0) return null;
  const word = pluralize(count, 'рекомендация', 'рекомендации', 'рекомендаций');

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '0.3rem',
        padding: '0.25rem 0.65rem',
        background: 'rgba(232, 98, 44, 0.1)',
        color: 'var(--color-accent)',
        borderRadius: '999px',
        fontSize: '0.78rem',
        fontWeight: 600,
        width: 'fit-content',
      }}
    >
      <span
        className="material-symbols-rounded"
        style={{ fontSize: 14 }}
      >
        recommend
      </span>
      {count} {word}
    </span>
  );
}