import type { ApplicationScoreSummary } from '../../types';
import styles from './ScoreBlock.module.css';

interface Props {
  summary: ApplicationScoreSummary;
}

const CATEGORY_LABELS: Record<string, string> = {
  LANGUAGE: 'Язык',
  FRAMEWORK: 'Фреймворк',
  LEVEL: 'Уровень',
  SPECIALIZATION: 'Специализация',
  EMPLOYMENT_TYPE: 'Тип занятости',
  TOOL: 'Инструмент',
  DATABASE: 'База данных',
};

const MATCH_TYPE_LABELS: Record<string, string> = {
  EXACT:     'точное совпадение',
  SYNONYM:   'синоним',
  SIBLING:   'близкая категория',
  HIERARCHY: 'дочерний/родительский тег',
};

const MATCH_TYPE_STYLE: Record<string, { icon: string; color: string }> = {
  EXACT:     { icon: 'check_circle',    color: '#059669' },              // зелёная галка
  SYNONYM:   { icon: 'swap_horiz',      color: '#2563eb' },              // синяя стрелки-обмен
  SIBLING:   { icon: 'change_history',  color: 'var(--color-accent)' },  // оранж треугольник
  HIERARCHY: { icon: 'account_tree',    color: 'var(--color-accent)' },  // оранж дерево
};

function getScoreColor(percent: number): string {
  if (percent >= 80) return '#059669';
  if (percent >= 50) return 'var(--color-accent)';
  return '#6b7280';
}

export function ScoreBlock({ summary }: Props) {
  const { scorePercent, matchedTags, totalRequiredTags } = summary;
  const scoreColor = getScoreColor(scorePercent);

  return (
    <div className={styles.block} style={{ borderLeftColor: scoreColor }}>
      <h4 className={styles.title}>
        <span
          className="material-symbols-rounded"
          style={{ fontSize: 18, color: scoreColor }}
        >
          insights
        </span>
        Совместимость:{' '}
        <span style={{ color: scoreColor, fontWeight: 700 }}>{scorePercent}%</span>
        <span className={styles.subtitle}>
          (совпало {matchedTags.length} из {totalRequiredTags})
        </span>
      </h4>

      {matchedTags.length === 0 ? (
        <p className={styles.empty}>
          Ни один требуемый тег вакансии не совпал с навыками соискателя.
        </p>
      ) : (
        <ul className={styles.tags}>
          {matchedTags.map((tag, idx) => {
            const style = MATCH_TYPE_STYLE[tag.matchType] || MATCH_TYPE_STYLE.EXACT;
            return (
              <li key={`${tag.tagName}-${idx}`} className={styles.tagRow}>
                <span
                  className="material-symbols-rounded"
                  style={{ fontSize: 16, color: style.color }}
                >
                  {style.icon}
                </span>
                <span className={styles.tagName}>{tag.tagName}</span>
                <span className={styles.tagCategory}>
                  {CATEGORY_LABELS[tag.category] || tag.category}
                </span>
                <span className={styles.tagMatchType} style={{ color: style.color }}>
                  {MATCH_TYPE_LABELS[tag.matchType] || tag.matchType}
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}