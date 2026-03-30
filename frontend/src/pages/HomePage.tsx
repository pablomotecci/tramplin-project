import { useState, useEffect, useRef, } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { getOpportunities } from '../api/opportunities';
import MapView from '../components/map/Map';
import type { OpportunityResponse, OpportunityFilters } from '../types';
import { OpportunityType, WorkFormat } from '../types';
import styles from './HomePage.module.css';
import { getTags } from '../api/tags';
import type { Tag } from '../types';
import { getPlatformStats, type PlatformStats } from '../api/stats';
import { SkeletonCard } from '../components/ui/Skeleton';

/*
  Два режима отображения: карта с маркерами и список
  Фильтры: тип, формат работы, город, зарплата.
*/

const TYPE_LABELS: Record<string, string> = {
  VACANCY: 'Вакансия',
  INTERNSHIP: 'Стажировка',
  MENTORSHIP: 'Менторство',
  EVENT: 'Мероприятие',
};

const FORMAT_ICONS: Record<string, string> = {
  OFFICE: 'apartment',
  HYBRID: 'sync_alt',
  REMOTE: 'home',
};

const FORMAT_LABELS: Record<string, string> = {
  OFFICE: 'Офис',
  HYBRID: 'Гибрид',
  REMOTE: 'Удалённо',
};

function formatSalary(min: number | null, max: number | null): string {
  if (min && max) 
    return `${min.toLocaleString('ru-RU')} – ${max.toLocaleString('ru-RU')} ₽`;
  if (min) 
    return `от ${min.toLocaleString('ru-RU')} ₽`;
  if (max) 
    return `до ${max.toLocaleString('ru-RU')} ₽`;
  return 'По договорённости';
}

function formatDate(iso: string | null): string {
  if (!iso) return '';
  return new Date(iso).toLocaleDateString('ru-RU', {
    day: 'numeric',
    month: 'short',
  });
}

type ViewMode = 'map' | 'list';






function AnimatedNumber({ value }: { value: number }) {
  const [current, setCurrent] = useState(0);

  useEffect(() => {
    let frame: number;
    const duration = 1500;
    const start = performance.now();

    function animate(now: number) {
      const progress = Math.min((now - start) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 4);
      setCurrent(Math.floor(eased * value));
      if (progress < 1) {
        frame = requestAnimationFrame(animate);
      }
    }

    frame = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(frame);
  }, [value]);

  return <>{current.toLocaleString('ru-RU')}</>;
}






export function HomePage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  // Режим отображения: карта или список
  const [viewMode, setViewMode] = useState<ViewMode>('map');

  // Фильтры
  const [filters, setFilters] = useState<OpportunityFilters>({
    page: 0,
    size: 20,
  });

  // Данные ленты
  const [opportunities, setOpportunities] = useState<OpportunityResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [listLoading, setListLoading] = useState(false);

  // Счётчик маркеров на карте
  const [markerCount, setMarkerCount] = useState<number | null>(null);

  // Теги для фильтра
  const [allTags, setAllTags] = useState<Tag[]>([]);
  const [tagsLoading, setTagsLoading] = useState(false);


  // Статистика платформы
  const [stats, setStats] = useState<PlatformStats | null>(null);

  useEffect(() => {
    getPlatformStats()
      .then(setStats)
      .catch(() => {
        // Бэкенд может не иметь /stats/public — показываем нули
        setStats({ companiesCount: 0, opportunitiesCount: 0, internshipsCount: 0, applicantsCount: 0 });
      });
  }, []);

  // Загрузка тегов при монтировании
  useEffect(() => {
    async function loadTags() {
      setTagsLoading(true);
      try {
        const data = await getTags();
        setAllTags(data);
      } catch (err) {
        console.error('Ошибка загрузки тегов:', err);
      } finally {
        setTagsLoading(false);
      }
    }
    loadTags();
  }, []);

  // Обработчик выбора/снятия тега
  function handleTagToggle(tagId: string) {
    setFilters(prev => {
      const current = prev.tagIds || [];
      const next = current.includes(tagId)
        ? current.filter(id => id !== tagId)
        : [...current, tagId];
      return { ...prev, tagIds: next.length > 0 ? next : undefined, page: 0 };
    });
  }

  // Загрузка ленты при переключении на список или изменении фильтров
  useEffect(() => {
    if (viewMode === 'list') {
      loadList();
    }
  }, [viewMode, filters]);

  async function loadList() {
    setListLoading(true);
    try {
      const data = await getOpportunities(filters);
      if (Array.isArray(data)) {
        setOpportunities(data as unknown as OpportunityResponse[]);
        setTotalPages(1);
      } else {
        setOpportunities(data.content || []);
        setTotalPages(data.totalPages || 1);
      }
    } catch (err) {
      console.error('Ошибка загрузки вакансий:', err);
    } finally {
      setListLoading(false);
    }
  }

  function handleFilterChange(key: keyof OpportunityFilters, value: string) {
    setFilters(prev => ({
      ...prev,
      [key]: value || undefined,
      page: 0, // сброс страницы при смене фильтра
    }));
  }

  function handlePageChange(newPage: number) {
    setFilters(prev => ({ ...prev, page: newPage }));
  }

  function clearFilters() {
    setFilters({ page: 0, size: 20 });
  }

  const hasActiveFilters = !!(filters.type || filters.workFormat || filters.city || filters.salaryMin || filters.search || (filters.tagIds && filters.tagIds.length > 0));





  const statsRef = useRef<HTMLDivElement>(null);
  const [statsVisible, setStatsVisible] = useState(false);

  useEffect(() => {
    if (!statsRef.current) return;
    const observer = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) setStatsVisible(true); },
      { threshold: 0.3 }
    );
    observer.observe(statsRef.current);
    return () => observer.disconnect();
  }, []);



  return (
    <main className={styles.main}>
      <section className={styles.hero}>
        <div className={styles.heroBgOrb1} />
        <div className={styles.heroBgOrb2} />

        <div className={styles.heroContent}>
          <h1 className={styles.heroTitle}>
            Начни свою карьеру{' '}
            <span className={styles.heroAccent}>здесь</span>
          </h1>
          <p className={styles.heroSubtitle}>
            Находи стажировки, вакансии и менторов в&nbsp;IT.
            Начни свой профессиональный путь с нами!
          </p>

          {/* Hero search bar */}
          <div className={styles.heroSearch}>
            <span className="material-symbols-rounded" style={{ fontSize: '22px', color: 'var(--color-text-secondary)' }}>search</span>
            <input
              type="text"
              className={styles.heroSearchInput}
              placeholder="Найти стажировку, вакансию, ментора..."
              value={filters.search || ''}
              onChange={(e) => handleFilterChange('search', e.target.value)}
              onFocus={() => {
                document.getElementById('controls-section')?.scrollIntoView({ behavior: 'smooth' });
              }}
            />
          </div>

          {user && (
            <div className={styles.welcomeBadge}>
              <span><span className="material-symbols-rounded">waving_hand</span></span>
              <span>Добро пожаловать, <strong>{user.displayName || user.email}</strong>!</span>
            </div>
          )}
        </div>
      </section>

       

  
      {/* Статистика — рендерим только если данные загрузились */}
      {stats && (
      <section className={styles.statsSection} ref={statsRef}>
        {[
          { value: stats.companiesCount ?? 0, label: 'Компаний', icon: 'apartment' },
          { value: stats.opportunitiesCount ?? 0, label: 'Вакансий', icon: 'work' },
          { value: stats.internshipsCount ?? 0, label: 'Стажировок', icon: 'school' },
          { value: stats.applicantsCount ?? 0, label: 'Соискателей', icon: 'group' },
        ].map((stat, i) => (
          <div
            key={i}
            className={`${styles.statCard} ${statsVisible ? styles.statCardVisible : ''}`}
            style={{ transitionDelay: `${i * 0.1}s` }}
          >
            <span className={`material-symbols-rounded ${styles.statIcon}`}>{stat.icon}</span>
            <span className={styles.statValue}>
              {statsVisible ? <AnimatedNumber value={stat.value} /> : '0'}
            </span>
            <span className={styles.statLabel}>{stat.label}</span>
          </div>
        ))}
      </section>
      )}





      {/* Панель управления */}
      <section className={styles.controlsSection} id="controls-section">
        <div className={styles.controlsContainer}>

          {/* Переключатель карта/список */}
          <div className={styles.viewToggle}>
            <button
              className={`${styles.toggleBtn} ${viewMode === 'map' ? styles.toggleBtnActive : ''}`}
              onClick={() => setViewMode('map')}
            >
              <span className="material-symbols-rounded">map</span> Карта
            </button>
            <button
              className={`${styles.toggleBtn} ${viewMode === 'list' ? styles.toggleBtnActive : ''}`}
              onClick={() => setViewMode('list')}
            >
              <span className="material-symbols-rounded">Assignment</span> Список
            </button>
          </div>

          {/* Фильтры */}
          <div className={styles.filters}>
            <div className={styles.searchRow}>
              <span className="material-symbols-rounded" style={{ fontSize: '20px', color: 'var(--color-text-secondary)' }}>search</span>
              <input
                type="text"
                className={styles.searchInput}
                placeholder="Поиск по названию или описанию..."
                value={filters.search || ''}
                onChange={(e) => handleFilterChange('search', e.target.value)}
              />
            </div>

            <select
              className={styles.filterSelect}
              value={filters.type || ''}
              onChange={(e) => handleFilterChange('type', e.target.value)}
            >
              <option value="">Все типы</option>
              <option value={OpportunityType.VACANCY}>Вакансии</option>
              <option value={OpportunityType.INTERNSHIP}>Стажировки</option>
              <option value={OpportunityType.MENTORSHIP}>Менторство</option>
              <option value={OpportunityType.EVENT}>Мероприятия</option>
            </select>

            <select
              className={styles.filterSelect}
              value={filters.workFormat || ''}
              onChange={(e) => handleFilterChange('workFormat', e.target.value)}
            >
              <option value="">Любой формат</option>
              <option value={WorkFormat.OFFICE}>Офис</option>
              <option value={WorkFormat.HYBRID}>Гибрид</option>
              <option value={WorkFormat.REMOTE}>Удалённо</option>
            </select>

            <input
              type="text"
              className={styles.filterInput}
              placeholder="Город"
              value={filters.city || ''}
              onChange={(e) => handleFilterChange('city', e.target.value)}
            />

            <input
              type="number"
              className={styles.filterInput}
              placeholder="Зарплата от"
              value={filters.salaryMin || ''}
              onChange={(e) => handleFilterChange('salaryMin', e.target.value)}
              min={0}
            />

            {hasActiveFilters && (
              <button className={styles.filterClear} onClick={clearFilters}>
                 Сбросить
              </button>
            )}

            {/* Теги */}
            {!tagsLoading && allTags.length > 0 && (
              <div className={styles.tagFilters}>
                {allTags.map(tag => {
                  const isSelected = (filters.tagIds || []).includes(tag.id);
                  return (
                    <button
                      key={tag.id}
                      className={`${styles.tagChip} ${isSelected ? styles.tagChipActive : ''}`}
                      onClick={() => handleTagToggle(tag.id)}
                    >
                      {tag.name}
                    </button>
                  );
                })}
              </div>
            )}

          </div>

          {/* Счётчик */}
          {viewMode === 'map' && markerCount !== null && (
            <div className={styles.counter}>
              Найдено на карте: {markerCount}
            </div>
          )}
        </div>
      </section>

      {/* Контент либо Карты или списка */}
      <section className={styles.contentSection}>
        <div className={styles.contentContainer}>
          {viewMode === 'map' ? (
            /* Карта */
            <div className={styles.mapWrapper}>
              <MapView onMarkersLoaded={setMarkerCount} tagIds={filters.tagIds} />
            </div>
          ) : (
            /* Список */
            <div className={styles.listWrapper}>
              {listLoading ? (
                <div className={styles.listGrid}>
                  {[...Array(6)].map((_, i) => <SkeletonCard key={i} />)}
                </div>
              ) : opportunities.length === 0 ? (
                <div className={styles.listEmpty}>
                  <p>Ничего не найдено</p>
                  {hasActiveFilters && (
                    <button className={styles.filterClear} onClick={clearFilters}>
                      Сбросить фильтры
                    </button>
                  )}
                </div>
              ) : (
                <>
                  <div className={styles.listGrid}>
                    {opportunities.map(opp => (
                      <div
                        key={opp.id}
                        className={styles.listCard}
                        onClick={() => navigate(`/opportunities/${opp.id}`)}
                      >
                        {/* Шапка карточки */}
                        <div className={styles.listCardTop}>
                          <span className={styles.listCardType}>
                            {TYPE_LABELS[opp.type] || opp.type}
                          </span>
                          <span className={styles.listCardFormat} style={{ display: 'inline-flex', alignItems: 'center', gap: '3px' }}>
                            <span className="material-symbols-rounded" style={{ fontSize: '14px' }}>{FORMAT_ICONS[opp.workFormat] || 'work'}</span>
                            {FORMAT_LABELS[opp.workFormat] || opp.workFormat}
                          </span>
                        </div>

                        {/* Название */}
                        <h3 className={styles.listCardTitle}>{opp.title}</h3>

                        {/* Компания */}
                        <div className={styles.listCardCompany}>
                          <div className={styles.listCardLogo}>
                            {opp.logoUrl
                              ? <img src={opp.logoUrl} alt="" />
                              : opp.companyName.charAt(0)
                            }
                          </div>
                          <span>{opp.companyName}</span>
                        </div>

                        {/* Теги */}
                        {opp.tags && opp.tags.length > 0 && (
                          <div className={styles.listCardTags}>
                            {opp.tags.slice(0, 3).map((tag, i) => (
                              <span key={i} className={styles.listCardTag}>{tag}</span>
                            ))}
                          </div>
                        )}

                        {/* Город и зарплата */}
                        <div className={styles.listCardBottom}>
                          <span className={styles.listCardCity} style={{ display: 'inline-flex', alignItems: 'center', gap: '3px' }}>
                            <span className="material-symbols-rounded" style={{ fontSize: '14px' }}>location_on</span>
                            {opp.city}
                          </span>
                          <span className={styles.listCardSalary}>
                            {formatSalary(opp.salaryMin, opp.salaryMax)}
                          </span>
                        </div>

                        {/* Дата */}
                        {opp.publishedAt && (
                          <div className={styles.listCardDate}>
                            {formatDate(opp.publishedAt)}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>

                  {totalPages > 1 && (
                    <div className={styles.pagination}>
                      <button
                        className={styles.pageBtn}
                        disabled={filters.page === 0}
                        onClick={() => handlePageChange((filters.page || 0) - 1)}
                      >
                        <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>chevron_left</span> Назад
                      </button>
                      <span className={styles.pageInfo}>
                        Страница {(filters.page || 0) + 1} из {totalPages}
                      </span>
                      <button
                        className={styles.pageBtn}
                        disabled={(filters.page || 0) >= totalPages - 1}
                        onClick={() => handlePageChange((filters.page || 0) + 1)}
                      >
                        Вперёд <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>chevron_right</span>
                      </button>
                    </div>
                  )}
                </>
              )}
            </div>
          )}
        </div>
      </section>
    </main>
  );
}