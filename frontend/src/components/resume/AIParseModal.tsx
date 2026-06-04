import { useState, useEffect, useMemo } from 'react';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { useToast } from '../ui/Toast';
import { parseResumeWithAI, updateApplicantTags } from '../../api/applicant';
import { getErrorMessage } from '../../api/client';
import type { SuggestedTagDto } from '../../types';
import styles from './AIParseModal.module.css';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  initialText?: string;
  currentTagIds: string[];
  onApplied: (mergedTagIds: string[]) => void;
}

const CATEGORY_LABELS: Record<string, string> = {
  LANGUAGE: 'Языки',
  FRAMEWORK: 'Фреймворки',
  DATABASE: 'Базы данных',
  TOOL: 'Инструменты',
  SPECIALIZATION: 'Специализация',
  LEVEL: 'Уровень',
  EMPLOYMENT_TYPE: 'Тип занятости',
};

const CATEGORY_ORDER = [
  'LANGUAGE', 'FRAMEWORK', 'DATABASE', 'TOOL',
  'SPECIALIZATION', 'LEVEL', 'EMPLOYMENT_TYPE',
];

const MAX_LENGTH = 10000;

function pluralizeTags(n: number): string {
  const mod10 = n % 10;
  const mod100 = n % 100;
  if (mod10 === 1 && mod100 !== 11) return 'тег';
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) return 'тега';
  return 'тегов';
}

export function AIParseModal({
  isOpen, onClose, initialText = '', currentTagIds, onApplied,
}: Props) {
  const { showToast } = useToast();
  const [text, setText] = useState(initialText);
  const [parsing, setParsing] = useState(false);
  const [applying, setApplying] = useState(false);
  const [suggestions, setSuggestions] = useState<SuggestedTagDto[] | null>(null);
  const [keptTagIds, setKeptTagIds] = useState<Set<string>>(new Set());
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setText(initialText);
      setSuggestions(null);
      setKeptTagIds(new Set());
      setErrorMsg(null);
    }
  }, [isOpen, initialText]);

  const groupedSuggestions = useMemo(() => {
    if (!suggestions) return [];
    const groups: Record<string, SuggestedTagDto[]> = {};
    for (const s of suggestions) {
      const cat = s.category || 'OTHER';
      if (!groups[cat]) groups[cat] = [];
      groups[cat].push(s);
    }
    const ordered = CATEGORY_ORDER
      .filter(cat => groups[cat])
      .map(cat => ({ category: cat, tags: groups[cat] }));
    const rest = Object.keys(groups)
      .filter(cat => !CATEGORY_ORDER.includes(cat))
      .map(cat => ({ category: cat, tags: groups[cat] }));
    return [...ordered, ...rest];
  }, [suggestions]);

  async function handleParse() {
    const trimmed = text.trim();
    if (!trimmed) {
      setErrorMsg('Введи текст резюме');
      return;
    }
    if (trimmed.length > MAX_LENGTH) {
      setErrorMsg(`Текст слишком длинный (максимум ${MAX_LENGTH} символов)`);
      return;
    }

    setErrorMsg(null);
    setParsing(true);
    try {
      const result = await parseResumeWithAI(trimmed);
      setSuggestions(result);
      setKeptTagIds(new Set(result.map(s => s.tagId)));
    } catch (err) {
      setErrorMsg(getErrorMessage(err));
    } finally {
      setParsing(false);
    }
  }

  function toggleTag(tagId: string) {
    setKeptTagIds(prev => {
      const next = new Set(prev);
      if (next.has(tagId)) next.delete(tagId);
      else next.add(tagId);
      return next;
    });
  }

  async function handleApply() {
    const merged = Array.from(new Set([...currentTagIds, ...Array.from(keptTagIds)]));

    setApplying(true);
    try {
      await updateApplicantTags(merged);
      showToast('Теги обновлены ✓', 'success');
      onApplied(merged);
      onClose();
    } catch (err) {
      showToast(getErrorMessage(err), 'error');
    } finally {
      setApplying(false);
    }
  }

  const hasSuggestions = suggestions !== null;
  const charCount = text.length;
  const isOverLimit = charCount > MAX_LENGTH;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Разобрать резюме через ИИ">
      {/* ввод текста */}
      {!hasSuggestions && (
        <>
          <p className={styles.subtitle}>
            Вставь текст резюме — ИИ предложит подходящие теги из словаря.
          </p>

          <div className={styles.field}>
            <label className={styles.label}>Текст резюме</label>
            <textarea
              className={styles.textarea}
              rows={10}
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="Опыт работы, технологии, проекты, навыки..."
              disabled={parsing}
            />
            <span className={`${styles.charCount} ${isOverLimit ? styles.charCountOver : ''}`}>
              {charCount} / {MAX_LENGTH}
            </span>
          </div>

          {errorMsg && <div className={styles.errorBox}>{errorMsg}</div>}

          <div className={styles.actions}>
            <Button variant="secondary" onClick={onClose} disabled={parsing}>
              Отмена
            </Button>
            <Button
              onClick={handleParse}
              isLoading={parsing}
              disabled={!text.trim() || isOverLimit}
            >
              <span
                className="material-symbols-rounded"
                style={{ fontSize: 16, marginRight: 4, verticalAlign: 'middle' }}
              >
                auto_awesome
              </span>
              Разобрать
            </Button>
          </div>
        </>
      )}

      {/* пустой результат */}
      {hasSuggestions && suggestions!.length === 0 && (
        <>
          <div className={styles.emptyResult}>
            <span
              className="material-symbols-rounded"
              style={{ fontSize: 48, color: 'var(--color-text-secondary)', opacity: 0.4 }}
            >
              search_off
            </span>
            <p>
              ИИ не распознал теги из резюме. Подробнее опиши
              технологии и опыт, либо выбери теги вручную.
            </p>
          </div>
          <div className={styles.actions}>
            <Button variant="secondary" onClick={() => setSuggestions(null)}>
              Изменить текст
            </Button>
            <Button onClick={onClose}>Закрыть</Button>
          </div>
        </>
      )}

      {/* есть подсказки - чипы */}
      {hasSuggestions && suggestions!.length > 0 && (
        <>
          <p className={styles.subtitle}>
            ИИ нашёл <strong>{suggestions!.length}</strong>{' '}
            {pluralizeTags(suggestions!.length)}. Сними отметки с тех, что не подходят, и нажми "Применить".
          </p>

          <div className={styles.suggestionsList}>
            {groupedSuggestions.map(({ category, tags }) => (
              <div key={category} className={styles.categoryGroup}>
                <div className={styles.categoryLabel}>
                  {CATEGORY_LABELS[category] || category}
                </div>
                <div className={styles.chips}>
                  {tags.map(tag => {
                    const isKept = keptTagIds.has(tag.tagId);
                    return (
                      <button
                        key={tag.tagId}
                        type="button"
                        onClick={() => toggleTag(tag.tagId)}
                        className={`${styles.chip} ${isKept ? styles.chipActive : ''}`}
                      >
                        {isKept && (
                          <span
                            className="material-symbols-rounded"
                            style={{ fontSize: 14 }}
                          >
                            check
                          </span>
                        )}
                        {tag.name}
                      </button>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>

          <div className={styles.summary}>
            Будет добавлено в профиль:{' '}
            <strong>{keptTagIds.size}</strong> {pluralizeTags(keptTagIds.size)}
            {keptTagIds.size > 0 && currentTagIds.length > 0 && (
              <span className={styles.summaryHint}>
                {' '}(к уже имеющимся, дубликаты игнорируются)
              </span>
            )}
          </div>

          <div className={styles.actions}>
            <Button
              variant="secondary"
              onClick={() => setSuggestions(null)}
              disabled={applying}
            >
              Изменить текст
            </Button>
            <Button
              onClick={handleApply}
              isLoading={applying}
              disabled={keptTagIds.size === 0}
            >
              Применить ({keptTagIds.size})
            </Button>
          </div>
        </>
      )}
    </Modal>
  );
}