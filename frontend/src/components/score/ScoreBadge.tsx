interface Props {
  percent: number;
}


function getColor(percent: number): { bg: string; text: string } {
  if (percent >= 80) return { bg: 'rgba(5, 150, 105, 0.15)',  text: '#059669' };
  if (percent >= 50) return { bg: 'rgba(232, 98, 44, 0.12)',  text: 'var(--color-accent)' };
  return                    { bg: 'rgba(107, 114, 128, 0.15)', text: '#6b7280' };
}

export function ScoreBadge({ percent }: Props) {
  const c = getColor(percent);
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '0.3rem',
        padding: '0.25rem 0.65rem',
        background: c.bg,
        color: c.text,
        borderRadius: '999px',
        fontSize: '0.78rem',
        fontWeight: 700,
        width: 'fit-content',
      }}
      title={`Совпадение по тегам: ${percent}%`}
    >
      <span className="material-symbols-rounded" style={{ fontSize: 14 }}>
        insights
      </span>
      {percent}%
    </span>
  );
}