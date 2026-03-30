import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getOpportunityById } from '../api/opportunities';
import { useAuth } from '../hooks/useAuth';
import type { OpportunityResponse } from '../types';
import { OpportunityType, WorkFormat } from '../types';
import styles from './OpportunityPage.module.css';
import { useFavorites } from '../hooks/useFavorites';
import { createApplication } from '../api/applications';
/*
  Публичная страница просмотра карточки возможности.
  Доступна всем — авторизованным и гостям.
*/
export default function OpportunityPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [opportunity, setOpportunity] = useState<OpportunityResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { isFavorite, toggleFavorite } = useFavorites();

  // окно отклика
  const [showApplyModal, setShowApplyModal] = useState(false);
  const [coverLetter, setCoverLetter] = useState('');
  const [applying, setApplying] = useState(false);
  const [applySuccess, setApplySuccess] = useState(false);
  const [applyError, setApplyError] = useState<string | null>(null);

  async function handleApply() {
    if (!id) return;
    setApplying(true);
    setApplyError(null);
    try {
      await createApplication({ opportunityId: id, coverLetter: coverLetter || undefined });
      setApplySuccess(true);
      setShowApplyModal(false);
      setCoverLetter('');
    } catch (err: any) {
      const msg = err?.response?.data?.error?.message || 'Не удалось отправить отклик';
      setApplyError(msg);
    } finally {
      setApplying(false);
    }
  }

  useEffect(() => {
    async function load() {
      if (!id) return;
      try {
        const data = await getOpportunityById(id);
        setOpportunity(data);
      } catch (err: any) {
        if (err?.response?.status === 404) {
          setError('Карточка не найдена');
        } else {
          setError('Не удалось загрузить данные');
        }
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id]);

  if (loading) {
    return (
      <div className={styles.container}>
        <div className={styles.skeleton}>Загрузка карточки...</div>
      </div>
    );
  }

  if (error || !opportunity) {
    return (
      <div className={styles.container}>
        <div className={styles.errorState}>
          <h2>{error || 'Что-то пошло не так'}</h2>
          <button onClick={() => navigate('/')} className={styles.btnSecondary}>
            На главную
          </button>
        </div>
      </div>
    );
  }

  // Вспомогательные функции для отображения
  function formatType(type: OpportunityType): string {
    const map: Record<OpportunityType, string> = {
      [OpportunityType.VACANCY]: 'Вакансия',
      [OpportunityType.INTERNSHIP]: 'Стажировка',
      [OpportunityType.MENTORSHIP]: 'Менторская программа',
      [OpportunityType.EVENT]: 'Мероприятие',
    };
    return map[type];
  }

  /* Человекопонятный формат работы */
  function WorkFormatBadge({ format }: { format: WorkFormat }) {
    const map: Record<WorkFormat, { icon: string; label: string }> = {
      [WorkFormat.OFFICE]: { icon: 'apartment', label: 'Офис' },
      [WorkFormat.HYBRID]: { icon: 'sync_alt', label: 'Гибрид' },
      [WorkFormat.REMOTE]: { icon: 'home', label: 'Удалённо' },
    } ;
    const { icon, label } = map[format];
    return (
      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
        <span className="material-symbols-rounded">{icon}</span>
        {label}
      </span>
    ) ;
  }

  /* Форматирование зарплаты */
  function formatSalary(): string {
    const { salaryMin, salaryMax } = opportunity!;

    if (salaryMin && salaryMax) {
      return `${salaryMin.toLocaleString('ru-RU')} – ${salaryMax.toLocaleString('ru-RU')} ₽`;
    }
    if (salaryMin) return `от ${salaryMin.toLocaleString('ru-RU')} ₽`;
    if (salaryMax) return `до ${salaryMax.toLocaleString('ru-RU')} ₽`;
    return 'По договорённости';
  }

  /* Форматирование даты */
  function formatDate(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('ru-RU', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }

  // Рендер
  return (
    <div className={styles.container}>

      {/* Кнопка "Назад" */}
      <button onClick={() => navigate(-1)} className={styles.backBtn}>
        <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>chevron_left</span> Назад
      </button>

      <div className={styles.hero}>
        <div className={styles.heroLeft}>
          {/* Бейдж типа */}
          <span className={styles.typeBadge}>{formatType(opportunity.type)}</span>

          <h1 className={styles.title}>{opportunity.title}</h1>

          {/* Компания */}
          <div className={styles.companyRow}>
            {opportunity.logoUrl && (
              <img
                src={opportunity.logoUrl}
                alt={opportunity.companyName}
                className={styles.companyLogo}
              />
            )}
            <div>
              <span
                className={styles.companyName}
                style={{ cursor: 'pointer' }}
                onClick={() => navigate(`/company/${opportunity.employerId}`)}
              >
                {opportunity.companyName}
              </span>
            </div>
          </div>
        </div>

        <div className={styles.heroRight}>
          <button
            className={`${styles.favBtn} ${isFavorite(opportunity.id) ? styles.favBtnActive : ''}`}
            onClick={() => toggleFavorite(opportunity.id, 'OPPORTUNITY')}
            title={isFavorite(opportunity.id) ? 'Убрать из избранного' : 'В избранное'}
          >
            <span className="material-symbols-rounded" style={{ fontSize: '24px', fontVariationSettings: isFavorite(opportunity.id) ? "'FILL' 1" : "'FILL' 0" }}>star</span>
          </button>
          <span className={styles.salary}>{formatSalary()}</span>
        </div>
      </div>

      <div className={styles.meta}>
        <div className={styles.metaItem}>
          <span className={styles.metaLabel}>Формат</span>
          <WorkFormatBadge format={opportunity.workFormat} />
        </div>

        <div className={styles.metaItem}>
          <span className={styles.metaLabel}>Город</span>
          <span className={styles.metaValue}>{opportunity.city}</span>
        </div>

        {opportunity.address && (
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>Адрес</span>
            <span className={styles.metaValue}>{opportunity.address}</span>
          </div>
        )}

        <div className={styles.metaItem}>
          <span className={styles.metaLabel}>Опубликовано</span>
          <span className={styles.metaValue}>{formatDate(opportunity.publishedAt)}</span>
        </div>

        {opportunity.expiresAt && (
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>Действует до</span>
            <span className={styles.metaValue}>{formatDate(opportunity.expiresAt)}</span>
          </div>
        )}

        {opportunity.type === OpportunityType.EVENT && opportunity.eventDate && (
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>Дата проведения</span>
            <span className={styles.metaValue}>{formatDate(opportunity.eventDate)}</span>
          </div>
        )}
      </div>

      {/* теги */}
        {opportunity.tags && opportunity.tags.length > 0 && (
          <div className={styles.tagsSection}>
            {opportunity.tags.map((tag, i) => (
              <span key={i} className={styles.tag}>{tag}</span>
            ))}
          </div>
        )}
        

      <div className={styles.descriptionSection}>
        <h2 className={styles.sectionTitle}>Описание</h2>
        <div className={styles.description}>
          {opportunity.description.split('\n').map((paragraph, i) => (
            <p key={i}>{paragraph}</p>
          ))}
        </div>
      </div>

      {(opportunity.contactEmail || opportunity.contactPhone || opportunity.contactUrl) && (
        <div className={styles.contactsSection}>
          <h2 className={styles.sectionTitle}>Контакты</h2>
          <div className={styles.contactsList}>
            {opportunity.contactEmail && (
              <a href={`mailto:${opportunity.contactEmail}`} className={styles.contactLink}>
                <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>contact_mail</span> {opportunity.contactEmail}
              </a>
            )}
            {opportunity.contactPhone && (
              <a href={`tel:${opportunity.contactPhone}`} className={styles.contactLink}>
                <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>call</span> {opportunity.contactPhone}
              </a>
            )}
            {opportunity.contactUrl && (
              <a
                href={opportunity.contactUrl}
                target="_blank"
                rel="noopener noreferrer"
                className={styles.contactLink}
              >
                <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>contacts</span> {opportunity.contactUrl}
              </a>
            )}
          </div>
        </div>
      )}

      {/* Кнопка отклика (только для авторизованного соискателя) */}
      <div className={styles.actions}>
        {user && user.role === 'APPLICANT' ? (
          applySuccess ? (
            <div className={styles.successBanner}>Отклик отправлен!</div>
          ) : (
            <button className={styles.btnPrimary} onClick={() => setShowApplyModal(true)}>
              Откликнуться
            </button>
          )
        ) : !user ? (
          <p className={styles.loginHint}>
            Чтобы откликнуться, <button onClick={() => navigate('/')} className={styles.linkBtn}>войдите</button> как соискатель.
          </p>
        ) : null}
      </div>
      
      {/* Модалка отклика */}
      {showApplyModal && (
        <div className={styles.modalOverlay} onClick={() => setShowApplyModal(false)}>
          <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
            <h2 className={styles.modalTitle}>Откликнуться на вакансию</h2>
            <p className={styles.modalSubtitle}>{opportunity.title} — {opportunity.companyName}</p>

            {applyError && <div className={styles.errorBanner}>{applyError}</div>}

            <label className={styles.modalLabel}>
              Сопроводительное письмо (по желанию)
            </label>
            <textarea
              className={styles.modalTextarea}
              value={coverLetter}
              onChange={(e) => setCoverLetter(e.target.value)}
              placeholder="Расскажите, почему вы подходите на эту позицию, что ожидаете, какие цели преследуете..."
              rows={5}
              maxLength={5000}
            />

            <div className={styles.modalActions}>
              <button
                className={styles.btnSecondary}
                onClick={() => setShowApplyModal(false)}
              >
                Отмена
              </button>
              <button
                className={styles.btnPrimary}
                onClick={handleApply}
                disabled={applying}
              >
                {applying ? 'Отправка...' : 'Отправить отклик'}
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}