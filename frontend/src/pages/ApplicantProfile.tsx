import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getApplicantById } from '../api/applicant';
import type { ApplicantProfileResponse } from '../types';
import styles from './OpportunityPage.module.css';

export default function ApplicantProfile() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<ApplicantProfileResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    async function load() {
      try {
        const data = await getApplicantById(id!);
        setProfile(data);
      } catch (err: any) {
        setError(err?.response?.status === 404 ? 'Профиль не найден' : 'Не удалось загрузить данные');
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id]);

  if (loading) {
    return (
      <div className={styles.container}>
        <div className={styles.skeleton}>Загрузка профиля...</div>
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className={styles.container}>
        <div className={styles.errorState}>
          <h2>{error || 'Что-то пошло не так'}</h2>
          <button onClick={() => navigate(-1)} className={styles.btnSecondary}>Назад</button>
        </div>
      </div>
    );
  }

  const fullName = [profile.lastName, profile.firstName, profile.middleName].filter(Boolean).join(' ');

  return (
    <div className={styles.container}>
      <button onClick={() => navigate(-1)} className={styles.backBtn}>
        <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>chevron_left</span> Назад
      </button>

      <div className={styles.hero}>
        <div className={styles.heroLeft}>
          <div className={styles.companyRow}>
            {profile.avatarUrl ? (
              <img src={profile.avatarUrl} alt="" className={styles.companyLogo} style={{ borderRadius: '50%' }} />
            ) : (
              <div style={{
                width: 48, height: 48, borderRadius: '50%', background: 'var(--color-accent, #E8622C)',
                color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: '1.25rem', fontWeight: 700, flexShrink: 0,
              }}>
                {(profile.firstName || '?').charAt(0).toUpperCase()}
              </div>
            )}
            <div>
              <h1 style={{ fontSize: '1.5rem', fontWeight: 700, margin: 0 }}>
                {fullName || 'Имя не указано'}
              </h1>
            </div>
          </div>
        </div>
      </div>

      <div className={styles.meta}>
        {profile.university && (
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>ВУЗ</span>
            <span className={styles.metaValue}>{profile.university}</span>
          </div>
        )}
        {profile.course && (
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>Курс</span>
            <span className={styles.metaValue}>{profile.course}</span>
          </div>
        )}
        {profile.graduationYear && (
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>Год выпуска</span>
            <span className={styles.metaValue}>{profile.graduationYear}</span>
          </div>
        )}
        {profile.phone && (
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>Телефон</span>
            <span className={styles.metaValue}>{profile.phone}</span>
          </div>
        )}
      </div>

      {profile.bio && (
        <div className={styles.descriptionSection}>
          <h2 className={styles.sectionTitle}>О себе</h2>
          <div className={styles.description}>
            {profile.bio.split('\n').map((p, i) => <p key={i}>{p}</p>)}
          </div>
        </div>
      )}

      {profile.skillsSummary && (
        <div style={{ padding: '1.25rem 0', borderBottom: '1px solid var(--color-border, #e5e7eb)' }}>
          <h2 className={styles.sectionTitle}>Навыки</h2>
          <p style={{ fontSize: '0.95rem', lineHeight: 1.7, margin: 0 }}>{profile.skillsSummary}</p>
        </div>
      )}

      {profile.tags && profile.tags.length > 0 && (
        <div className={styles.tagsSection}>
          {profile.tags.map((tag, i) => (
            <span key={i} className={styles.tag}>{tag}</span>
          ))}
        </div>
      )}

      {(profile.portfolioUrl || profile.githubUrl) && (
        <div className={styles.contactsSection}>
          <h2 className={styles.sectionTitle}>Ссылки</h2>
          <div className={styles.contactsList}>
            {profile.portfolioUrl && (
              <a href={profile.portfolioUrl} target="_blank" rel="noopener noreferrer" className={styles.contactLink}>
                <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>link</span> Портфолио
              </a>
            )}
            {profile.githubUrl && (
              <a href={profile.githubUrl} target="_blank" rel="noopener noreferrer" className={styles.contactLink}>
                <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>code</span> GitHub
              </a>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
