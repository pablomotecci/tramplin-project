import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchApplicants } from '../api/applicant';
import { Button } from '../components/ui/Button';
import type { ApplicantSearchResult } from '../types';

const PAGE_SIZE = 20;

export function ApplicantsSearchPage() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<ApplicantSearchResult[]>([]);
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function fetchPage(pageToLoad: number, append: boolean) {
    setLoading(true);
    setError(null);
    try {
      const data = await searchApplicants({ query: query.trim() || undefined, page: pageToLoad, size: PAGE_SIZE });
      setResults(prev => (append ? [...prev, ...data.content] : data.content));
      setPage(data.number);
      setTotalElements(data.totalElements);
    } catch (e: any) {
      setError(e?.response?.data?.error?.message || 'Не удалось выполнить поиск');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { fetchPage(0, false); }, []);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchPage(0, false);
  };

  const hasMore = results.length < totalElements;

  const wrap = { maxWidth: 760, margin: '2rem auto', padding: '0 1rem' } as const;
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

      <form onSubmit={handleSubmit} style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
        <input
          value={query}
          onChange={e => setQuery(e.target.value)}
          placeholder="Имя, фамилия или вуз"
          style={{ flex: 1, minWidth: 0, padding: '0.6rem 0.75rem', borderRadius: 8, border: '1px solid var(--color-border)', background: 'transparent', color: 'inherit', font: 'inherit' }}
        />
        <Button type="submit" variant="primary" isLoading={loading}>Найти</Button>
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
          <Button variant="secondary" isLoading={loading} onClick={() => fetchPage(page + 1, true)}>
            Загрузить ещё
          </Button>
        </div>
      )}
    </div>
  );
}