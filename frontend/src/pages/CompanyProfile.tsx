import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getCompanyPublicProfile } from '../api/employer';
import type { CompanyProfileResponse } from '../types';
import styles from './OpportunityPage.module.css';

export default function CompanyProfile() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [company, setCompany] = useState<CompanyProfileResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    async function load() {
      try {
        const data = await getCompanyPublicProfile(id!);
        setCompany(data);
      } catch (err: any) {
        setError(err?.response?.status === 404 ? 'Компания не найдена' : 'Не удалось загрузить данные');
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id]);

  if (loading) {
    return (
      <div className={styles.container}>
        <div className={styles.skeleton}>Загрузка профиля компании...</div>
      </div>
    );
  }

  if (error || !company) {
    return (
      <div className={styles.container}>
        <div className={styles.errorState}>
          <h2>{error || 'Что-то пошло не так'}</h2>
          <button onClick={() => navigate('/')} className={styles.btnSecondary}>На главную</button>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <button onClick={() => navigate(-1)} className={styles.backBtn}>
        <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>chevron_left</span> Назад
      </button>

      <div className={styles.hero}>
        <div className={styles.heroLeft}>
          <div className={styles.companyRow}>
            {company.logoUrl && (
              <img src={company.logoUrl} alt={company.companyName} className={styles.companyLogo} />
            )}
            <div>
              <h1 style={{ fontSize: '1.75rem', fontWeight: 700, margin: 0 }}>{company.companyName}</h1>
              {company.industry && (
                <span className={styles.companyIndustry}>{company.industry}</span>
              )}
            </div>
          </div>
        </div>
        <div className={styles.heroRight}>
          <span style={{
            fontSize: '0.8rem', fontWeight: 600, padding: '0.2rem 0.6rem',
            border: '1.5px solid', borderRadius: '999px',
            color: company.verificationStatus === 'VERIFIED' ? '#059669' : '#6b7280',
            borderColor: company.verificationStatus === 'VERIFIED' ? '#059669' : '#6b7280',
          }}>
            {company.verificationStatus === 'VERIFIED' ? 'Верифицирована' : 'Не верифицирована'}
          </span>
        </div>
      </div>

      <div className={styles.meta}>
        {company.city && (
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>Город</span>
            <span className={styles.metaValue}>{company.city}</span>
          </div>
        )}
        {company.address && (
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>Адрес</span>
            <span className={styles.metaValue}>{company.address}</span>
          </div>
        )}
        {company.websiteUrl && (
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>Сайт</span>
            <a href={company.websiteUrl} target="_blank" rel="noopener noreferrer" className={styles.contactLink}>
              {company.websiteUrl}
            </a>
          </div>
        )}
      </div>

      {company.description && (
        <div className={styles.descriptionSection}>
          <h2 className={styles.sectionTitle}>О компании</h2>
          <div className={styles.description}>
            {company.description.split('\n').map((p, i) => <p key={i}>{p}</p>)}
          </div>
        </div>
      )}

      {(company.phone || company.email) && (
        <div className={styles.contactsSection}>
          <h2 className={styles.sectionTitle}>Контакты</h2>
          <div className={styles.contactsList}>
            {company.email && (
              <a href={`mailto:${company.email}`} className={styles.contactLink}>
                <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>contact_mail</span> {company.email}
              </a>
            )}
            {company.phone && (
              <a href={`tel:${company.phone}`} className={styles.contactLink}>
                <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>call</span> {company.phone}
              </a>
            )}
          </div>
        </div>
      )}

      {company.videoUrl && (
        <div style={{ padding: '1.5rem 0', borderBottom: '1px solid var(--color-border, #e5e7eb)' }}>
          <h2 className={styles.sectionTitle}>Видеопрезентация</h2>
          <a href={company.videoUrl} target="_blank" rel="noopener noreferrer" className={styles.contactLink}>
            <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>play_circle</span> Смотреть видео
          </a>
        </div>
      )}

      {company.officePhotos && company.officePhotos.length > 0 && (
        <div style={{ padding: '1.5rem 0' }}>
          <h2 className={styles.sectionTitle}>Фото офиса</h2>
          <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
            {company.officePhotos.map((url, i) => (
              <img key={i} src={url} alt={`Офис ${i + 1}`} style={{
                width: 200, height: 140, objectFit: 'cover', borderRadius: '12px',
                border: '1px solid var(--color-border, #e5e7eb)',
              }} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
