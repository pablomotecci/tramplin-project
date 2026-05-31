import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchApplicants } from '../api/applicant';
import { getTags } from '../api/tags';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import type { ApplicantSearchResult, Tag } from '../types';
import type { ApplicantSearchParams } from '../api/applicant';

const PAGE_SIZE = 20;

// параметры отвечают за фильтрацию
type Filters = Pick<ApplicantSearchParams, 'query' | 'tagIds' | 'university' | 'graduationYearMin' | 'graduationYearMax'>;

export function ApplicantsSearchPage() {
  const navigate = useNavigate();

  // Поле поиска и фильтры
  const [query, setQuery] = useState('');
  const [selectedTagIds, setSelectedTagIds] = useState<string[]>([]);
  const [university, setUniversity] = useState('');
  const [yearMin, setYearMin] = useState('');
  const [yearMax, setYearMax] = useState('');

  // Справочник тегов грузится один раз
  const [tags, setTags] = useState<Tag[]>([]);

  // Результаты и пагинация
  const [results, setResults] = useState<ApplicantSearchResult[]>([]);
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // фильтры из текущего состояния undefined = не отправляем
  function currentFilters(): Filters {
    return {
      query: query.trim() || undefined,
      tagIds: selectedTagIds.length > 0 ? selectedTagIds : undefined,
      university: university.trim() || undefined,
      graduationYearMin: yearMin ? Number(yearMin) : undefined,
      graduationYearMax: yearMax ? Number(yearMax) : undefined,
    };
  }

  // загрузка страницы с переданными фильтрами; append=true — догрузка к существующим
  async function fetchPage(filters: Filters, pageToLoad: number, append: boolean) {
    setLoading(true);
    setError(null);
    try {
      const data = await searchApplicants({ ...filters, page: pageToLoad, size: PAGE_SIZE });
      setResults(prev => (append ? [...prev, ...data.content] : data.content));
      setPage(data.number);
      setTotalElements(data.totalElements);
    } catch (e: any) {
      setError(e?.response?.data?.error?.message || 'Не удалось выполнить поиск');
    } finally {
      setLoading(false);
    }
  }

  // стартовая загрузка -  справочник тегов + первая страница без филтров
  useEffect(() => {
    getTags().then(setTags).catch(() => {});
    fetchPage({}, 0, false);
  }, []);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchPage(currentFilters(), 0, false);
  };

  // сброс - очистка полей + загрузка списка без фильтров
  function handleReset() {
    setQuery('');
    setSelectedTagIds([]);
    setUniversity('');
    setYearMin('');
    setYearMax('');
    fetchPage({}, 0, false);
  }

  function toggleTag(id: string) {
    setSelectedTagIds(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);
  }

  const hasMore = results.length < totalElements;
  const filtersActive = !!(query || selectedTagIds.length || university || yearMin || yearMax);

  // стили
  const wrap = { maxWidth: 760, margin: '2rem auto', padding: '0 1rem' } as const;
  const filterBox = {
    background: 'var(--color-bg-subtle, transparent)',
    border: '1px solid var(--color-border)',
    borderRadius: 12, padding: '1rem', marginBottom: '1rem',
  } as const;
  const filterLabel = { fontSize: '0.85rem', color: 'var(--color-text-secondary)', marginBottom: 6 } as const;
  const card = {
    display: 'flex', gap: '0.75rem', alignItems: 'center', width: '100%', textAlign: 'left',
    padding: '0.85rem', marginBottom: '0.6rem', borderRadius: 10,
    border: '1px solid var(--color-border)', background: 'var(--color-bg-subtle, transparent)',
    cursor: 'pointer', color: 'inherit', font: 'inherit',
  } as const;
  const meta = { color: 'var(--color-text-secondary)', fontSize: '0.85rem' } as const;

  return (
    <div style={wrap}>
      <h1 style={{ marginBottom: '1.25rem' }}>Соискатели</h1>

      <form onSubmit={handleSubmit}>
        {/* Строка поиска */}
        <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
          <input
            value={query}
            onChange={e => setQuery(e.target.value)}
            placeholder="Имя, фамилия или вуз"
            style={{ flex: 1, minWidth: 0, padding: '0.6rem 0.75rem', borderRadius: 8, border: '1px solid var(--color-border)', background: 'transparent', color: 'inherit', font: 'inherit' }}
          />
          <Button type="submit" variant="primary" isLoading={loading}>Найти</Button>
        </div>

        {/* Блок фильтров */}
        <div style={filterBox}>
          {/* Теги */}
          <div style={filterLabel}>Навыки {selectedTagIds.length > 0 && <>· выбрано: {selectedTagIds.length}</>}</div>
          {tags.length === 0 ? (
            <div style={meta}>Загрузка справочника…</div>
          ) : (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: '0.75rem', maxHeight: 140, overflowY: 'auto' }}>
              {tags.map(t => {
                const active = selectedTagIds.includes(t.id);
                return (
                  <button type="button" key={t.id} onClick={() => toggleTag(t.id)} style={{
                    fontSize: '0.8rem', padding: '4px 10px', borderRadius: 999,
                    border: '1px solid var(--color-accent, #E8622C)',
                    background: active ? 'var(--color-accent, #E8622C)' : 'transparent',
                    color: active ? '#fff' : 'var(--color-accent, #E8622C)',
                    cursor: 'pointer', font: 'inherit',
                  }}>{t.name}</button>
                );
              })}
            </div>
          )}

          {/* Вуз и годы */}
          <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
            <div style={{ flex: 2, minWidth: 200 }}>
              <Input label="Вуз" placeholder="МГТУ, ИТМО…" value={university} onChange={e => setUniversity(e.target.value)} />
            </div>
            <div style={{ flex: 1, minWidth: 100 }}>
              <Input label="Год выпуска от" type="number" placeholder="2024" value={yearMin} onChange={e => setYearMin(e.target.value)} />
            </div>
            <div style={{ flex: 1, minWidth: 100 }}>
              <Input label="до" type="number" placeholder="2026" value={yearMax} onChange={e => setYearMax(e.target.value)} />
            </div>
          </div>

          {filtersActive && (
            <div style={{ marginTop: '0.75rem' }}>
              <button type="button" onClick={handleReset}
                style={{ background: 'none', border: 'none', color: 'var(--color-accent, #E8622C)', cursor: 'pointer', padding: 0, font: 'inherit', fontSize: '0.85rem' }}>
                Сбросить фильтры
              </button>
            </div>
          )}
        </div>
      </form>

      {error && <p style={{ color: '#dc2626' }}>{error}</p>}

      {!loading && results.length === 0 && !error && (
        <p style={meta}>Никого не найдено.</p>
      )}

      {results.map(a => (
        <button key={a.userId} style={card} onClick={() => navigate(`/applicant/${a.userId}`)}>
          {a.avatarUrl
            ? <img src={a.avatarUrl} alt="" style={{ width: 44, height: 44, borderRadius: '50%', objectFit: 'cover', flexShrink: 0 }} />
            : <div style={{ width: 44, height: 44, borderRadius: '50%', background: 'var(--color-accent, #E8622C)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, flexShrink: 0 }}>
                {(a.firstName || a.lastName || '?').charAt(0).toUpperCase()}
              </div>
          }
          <div style={{ minWidth: 0 }}>
            <div style={{ fontWeight: 600 }}>{a.lastName} {a.firstName}</div>
            <div style={meta}>{[a.university, a.graduationYear].filter(Boolean).join(' · ') || 'Вуз не указан'}</div>
            {a.topTags.length > 0 && (
              <div style={{ display: 'flex', gap: 6, marginTop: 4, flexWrap: 'wrap' }}>
                {a.topTags.map((t, i) => (
                  <span key={i} style={{ fontSize: '0.75rem', padding: '2px 8px', borderRadius: 999, background: 'rgba(232,98,44,0.12)', color: 'var(--color-accent, #E8622C)' }}>{t}</span>
                ))}
              </div>
            )}
          </div>
        </button>
      ))}

      {hasMore && (
        <div style={{ textAlign: 'center', marginTop: '1rem' }}>
          <Button variant="secondary" isLoading={loading} onClick={() => fetchPage(currentFilters(), page + 1, true)}>
            Загрузить ещё
          </Button>
        </div>
      )}
    </div>
  );
}