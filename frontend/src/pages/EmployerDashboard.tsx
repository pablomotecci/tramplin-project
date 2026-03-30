// Страница личного кабинета Работодателя


import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { getCompanyProfile, updateCompanyProfile } from '../api/employer';
import { getMyOpportunities, deleteOpportunity, changeOpportunityStatus } from '../api/opportunities';
import { getErrorMessage } from '../api/client';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import type { CompanyProfileResponse, UpdateCompanyRequest, OpportunityResponse } from '../types';
import styles from './Dashboard.module.css';
import { getIncomingApplications, updateApplicationStatus } from '../api/applications';
import type { ApplicationResponse, VerificationRequestResponse } from '../types';
import { ApplicationStatus } from '../types';
import { submitVerificationRequest, getMyVerificationRequest } from '../api/verification';
import { SkeletonProfile } from '../components/ui/Skeleton';
import { getRecommendationsByOpportunity, type RecommendationResponse } from '../api/recommendations';
import { getCandidatesForOpportunity, type ApplicantScore } from '../api/scoring';
import { FileUpload } from '../components/ui/FileUpload';



const verificationLabels: Record<string, string> = {
  UNVERIFIED: 'Не подтверждена',
  PENDING: 'На проверке',
  VERIFIED: 'Верифицирована',
  REJECTED: 'Отклонена',
};

const verificationColors: Record<string, string> = {
  UNVERIFIED: '#6b7280',
  PENDING: '#d97706',
  VERIFIED: '#059669',
  REJECTED: '#dc2626',
};

const typeLabels: Record<string, string> = {
  VACANCY: 'Вакансия',
  INTERNSHIP: 'Стажировка',
  MENTORSHIP: 'Менторство',
  EVENT: 'Мероприятие',
};

const statusLabels: Record<string, string> = {
  DRAFT: 'Черновик',
  ACTIVE: 'Активна',
  CLOSED: 'Закрыта',
  ON_MODERATION: 'На модерации',
};

const statusColors: Record<string, string> = {
  DRAFT: '#6b7280',
  ACTIVE: '#059669',
  CLOSED: '#dc2626',
  ON_MODERATION: '#d97706',
};

const workFormatLabels: Record<string, string> = {
  OFFICE: 'Офис',
  HYBRID: 'Гибрид',
  REMOTE: 'Удалённо',
};

// Вспомогательная функция — текст баннера верификации
function getVerificationBanner(status: string): { icon: string; title: string; text: string } | null {
  switch (status) {
    case 'REJECTED':
      return {
        icon: 'cancel',
        title: 'Верификация отклонена',
        text: 'Проверьте данные компании и повторите попытку. Вы не можете создавать вакансии.',
      };
    case 'PENDING':
      return {
        icon: 'hourglass_top',
        title: 'Компания на проверке',
        text: 'Ваша компания проходит верификацию. До завершения проверки вы не сможете создавать вакансии.',
      };
    case 'UNVERIFIED':
      return {
        icon: 'verified_user',
        title: 'Требуется верификация',
        text: 'Заполните профиль компании и ИНН для прохождения верификации.',
      };
    default:
      return null;
  }
}

export function EmployerDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [profile, setProfile] = useState<CompanyProfileResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Вакансии работодателя
  const [opportunities, setOpportunities] = useState<OpportunityResponse[]>([]);
  const [oppsLoading, setOppsLoading] = useState(true);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [statusChangingId, setStatusChangingId] = useState<string | null>(null);

  // Отклики
  const [apps, setApps] = useState<ApplicationResponse[]>([]);
  const [appsLoading, setAppsLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState<ApplicationStatus | undefined>(undefined);
  const [updatingAppId, setUpdatingAppId] = useState<string | null>(null);

  // Рекомендации по вакансиям
  const [recsMap, setRecsMap] = useState<Record<string, RecommendationResponse[]>>({});
  // Скоринг — лучшие кандидаты по вакансиям
  const [candidatesMap, setCandidatesMap] = useState<Record<string, ApplicantScore[]>>({});

  //Верификация
  const [verifRequest, setVerifRequest] = useState<VerificationRequestResponse | null>(null);
  const [showVerifForm, setShowVerifForm] = useState(false);
  const [verifInn, setVerifInn] = useState('');
  const [verifDomain, setVerifDomain] = useState('');
  const [verifEmail, setVerifEmail] = useState('');
  const [verifLoading, setVerifLoading] = useState(false);
  const [verifError, setVerifError] = useState<string | null>(null);

  const [form, setForm] = useState<UpdateCompanyRequest>({
    companyName: '',
    description: '',
    industry: '',
    inn: '',
    websiteUrl: '',
    city: '',
    address: '',
    phone: '',
    email: '',
    officePhotos: [],
    videoUrl: '',
  });

  useEffect(() => {
    loadProfile();
  }, []);

  useEffect(() => {
    loadOpportunities();
  }, []);

  async function loadProfile() {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getCompanyProfile();
      setProfile(data);
      setForm({
        companyName: data.companyName || '',
        description: data.description || '',
        industry: data.industry || '',
        inn: data.inn || '',
        websiteUrl: data.websiteUrl || '',
        city: data.city || '',
        address: data.address || '',
        phone: data.phone || '',
        email: data.email || '',
        officePhotos: data.officePhotos || [],
        videoUrl: data.videoUrl || '',
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }

  async function loadOpportunities() {
    setOppsLoading(true);
    try {
      const data = await getMyOpportunities();
      setOpportunities(data);
    } catch (err) {
      console.error('Не удалось загрузить вакансии:', err);
    } finally {
      setOppsLoading(false);
    }
  }

  // Загрузка рекомендаций и скоринга по вакансиям
  useEffect(() => {
    if (opportunities.length === 0) return;
    async function loadRecsAndCandidates() {
      const rMap: Record<string, RecommendationResponse[]> = {};
      const cMap: Record<string, ApplicantScore[]> = {};
      await Promise.all(
        opportunities.map(async (opp) => {
          try {
            const [recs, candidates] = await Promise.all([
              getRecommendationsByOpportunity(opp.id).catch(() => [] as RecommendationResponse[]),
              getCandidatesForOpportunity(opp.id).catch(() => [] as ApplicantScore[]),
            ]);
            if (recs.length > 0) rMap[opp.id] = recs;
            if (candidates.length > 0) cMap[opp.id] = candidates;
          } catch { /* skip */ }
        })
      );
      setRecsMap(rMap);
      setCandidatesMap(cMap);
    }
    loadRecsAndCandidates();
  }, [opportunities]);

  // Загрузка текущей заявки на верификацию
  useEffect(() => {
    async function loadVerification() {
      try {
        const data = await getMyVerificationRequest();
        setVerifRequest(data);
      } catch {
        // Нет заявки — нормально
      }
    }
    loadVerification();
  }, []);

  async function handleSubmitVerification(e: FormEvent) {
    e.preventDefault();
    setVerifError(null);

    if (!/^\d{10}(\d{2})?$/.test(verifInn)) {
      setVerifError('ИНН должен содержать 10 или 12 цифр');
      return;
    }
    if (!verifDomain.trim()) {
      setVerifError('Укажите домен компании');
      return;
    }
    if (!verifEmail.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(verifEmail)) {
      setVerifError('Укажите корректный корпоративный email');
      return;
    }

    try {
      setVerifLoading(true);
      const result = await submitVerificationRequest({
        inn: verifInn.trim(),
        companyDomain: verifDomain.trim(),
        corporateEmail: verifEmail.trim(),
      });
      setVerifRequest(result);
      setShowVerifForm(false);
      setSuccessMsg('Заявка на верификацию отправлена! Ожидайте проверки куратором.');
    } catch (err) {
      setVerifError(getErrorMessage(err));
    } finally {
      setVerifLoading(false);
    }
  }

  async function handleOppStatusChange(id: string, status: 'ACTIVE' | 'CLOSED' | 'DRAFT') {
    setStatusChangingId(id);
    try {
      const updated = await changeOpportunityStatus(id, status);
      setOpportunities(prev => prev.map(o => o.id === id ? updated : o));
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setStatusChangingId(null);
    }
  }

  async function handleDeleteOpportunity(id: string) {
    if (!confirm('Удалить эту вакансию? Это действие необратимо.')) return;
    setDeletingId(id);
    try {
      await deleteOpportunity(id);
      setOpportunities(prev => prev.filter(o => o.id !== id));
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setDeletingId(null);
    }
  }

  function formatSalary(min: number | null, max: number | null): string {
    if (min && max) return `${min.toLocaleString('ru-RU')} – ${max.toLocaleString('ru-RU')} ₽`;
    if (min) return `от ${min.toLocaleString('ru-RU')} ₽`;
    if (max) return `до ${max.toLocaleString('ru-RU')} ₽`;
    return 'По договорённости';
  }

  function handleChange(field: keyof UpdateCompanyRequest, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);

    if (!form.companyName.trim()) {
      setError('Название компании обязательно');
      return;
    }
    if (form.inn && !/^\d{10}(\d{2})?$/.test(form.inn)) {
      setError('ИНН должен содержать 10 или 12 цифр');
      return;
    }
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      setError('Некорректный формат email');
      return;
    }

    setIsSaving(true);
    try {
      const updated = await updateCompanyProfile(form);
      setProfile(updated);
      setIsEditing(false);
      setSuccessMsg('Профиль компании сохранён');
      setTimeout(() => setSuccessMsg(null), 3000);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSaving(false);
    }
  }

  function handleCancel() {
    if (profile) {
      setForm({
        companyName: profile.companyName || '',
        description: profile.description || '',
        industry: profile.industry || '',
        inn: profile.inn || '',
        websiteUrl: profile.websiteUrl || '',
        city: profile.city || '',
        address: profile.address || '',
        phone: profile.phone || '',
        email: profile.email || '',
        officePhotos: profile.officePhotos || [],
        videoUrl: profile.videoUrl || '',
      });
    }
    setIsEditing(false);
    setError(null);
  }


  useEffect(() => {
    loadApplications();
  }, [statusFilter]);

  async function loadApplications() {
    setAppsLoading(true);
    try {
      const data = await getIncomingApplications(0, 50, statusFilter);
      setApps(data.content || []);
    } catch (err) {
      console.error('Ошибка загрузки откликов:', err);
    } finally {
      setAppsLoading(false);
    }
  }

  async function handleStatusChange(appId: string, newStatus: ApplicationStatus) {
    setUpdatingAppId(appId);
    try {
      const updated = await updateApplicationStatus(appId, { status: newStatus });
      setApps(prev => prev.map(a => a.id === appId ? updated : a));
    } catch (err) {
      console.error('Ошибка смены статуса:', err);
    } finally {
      setUpdatingAppId(null);
    }
  }


  if (isLoading) {
    return (
      <main className={styles.dashboard}>
        <div className={styles.container}>
          <div className={styles.container}><SkeletonProfile /></div>
        </div>
      </main>
    );
  }

  

  const vStatus = profile?.verificationStatus || 'UNVERIFIED';
  const isNotVerified = vStatus !== 'VERIFIED';
  const banner = getVerificationBanner(vStatus);

  return (
    <main className={styles.dashboard}>
      <div className={styles.container}>
        <header className={styles.pageHeader}>
          <h1 className={styles.pageTitle}>Кабинет работодателя</h1>
          <span className={`${styles.roleBadge} ${styles.roleBadgeEmployer}`}>
            Работодатель
          </span>
        </header>

        {successMsg && <div className={styles.successBanner}>{successMsg}</div>}
        {error && <div className={styles.errorBanner}>{error}</div>}

        {banner && (
          <div className={styles.warningBanner}>
            <span className="material-symbols-rounded">{banner.icon}</span>
            <div>
              <strong>{banner.title}</strong>
              <p>{banner.text}</p>
            </div>
          </div>
        )}

        {/* Форма верификации */}
        {isNotVerified && (
          <section className={styles.card} style={{ marginBottom: '1.25rem' }}>
            <div className={styles.cardHeader}>
              <h2 className={styles.cardTitle}>
                <span className="material-symbols-rounded">verified_user</span> Верификация компании
              </h2>
              {!showVerifForm && !verifRequest && (
                <Button variant="primary" size="sm" onClick={() => setShowVerifForm(true)}>
                  Подать заявку
                </Button>
              )}
            </div>

            {/* Статус текущей заявки */}
            {verifRequest && (
              <div style={{
                padding: '1rem', borderRadius: '10px',
                background: verifRequest.status === 'REJECTED' ? 'var(--color-error-bg)' : 'var(--color-bg-subtle)',
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <span style={{ fontSize: '0.9rem', fontWeight: 600 }}>
                    Заявка #{verifRequest.id.slice(0, 8)}
                  </span>
                  <span style={{
                    fontSize: '0.75rem', fontWeight: 600, padding: '0.15rem 0.5rem',
                    borderRadius: '999px',
                    background: verifRequest.status === 'APPROVED' ? '#05966918' :
                                verifRequest.status === 'REJECTED' ? '#dc262618' : '#d9770618',
                    color: verifRequest.status === 'APPROVED' ? '#059669' :
                           verifRequest.status === 'REJECTED' ? '#dc2626' : '#d97706',
                  }}>
                    {verifRequest.status === 'PENDING' ? 'На рассмотрении' :
                     verifRequest.status === 'INN_VERIFIED' ? 'ИНН подтверждён' :
                     verifRequest.status === 'EMAIL_VERIFIED' ? 'Email подтверждён' :
                     verifRequest.status === 'APPROVED' ? 'Одобрена' : 'Отклонена'}
                  </span>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.5rem', fontSize: '0.85rem', color: 'var(--color-text)' }}>
                  <div><span style={{ color: 'var(--color-text-secondary)' }}>ИНН:</span> {verifRequest.inn}</div>
                  <div><span style={{ color: 'var(--color-text-secondary)' }}>Домен:</span> {verifRequest.companyDomain}</div>
                  <div><span style={{ color: 'var(--color-text-secondary)' }}>Email:</span> {verifRequest.corporateEmail}</div>
                </div>
                {verifRequest.rejectionReason && (
                  <div style={{
                    marginTop: '0.5rem', padding: '0.5rem 0.75rem',
                    background: '#fff', borderRadius: '6px', borderLeft: '3px solid var(--color-error)',
                    fontSize: '0.85rem', color: 'var(--color-error)',
                  }}>
                    Причина: {verifRequest.rejectionReason}
                  </div>
                )}
                {verifRequest.status === 'REJECTED' && (
                  <Button variant="secondary" size="sm" style={{ marginTop: '0.75rem' }}
                    onClick={() => { setShowVerifForm(true); setVerifRequest(null); }}>
                    Подать повторно
                  </Button>
                )}
              </div>
            )}

            {/* Форма подачи */}
            {showVerifForm && !verifRequest && (
              <form onSubmit={handleSubmitVerification} className={styles.editForm}>
                {verifError && <div className={styles.errorBanner}>{verifError}</div>}
                <p style={{ fontSize: '0.9rem', color: 'var(--color-text-secondary)', margin: '0 0 0.5rem' }}>
                  Укажите ИНН, корпоративный домен и рабочий email. ИНН проверяется автоматически, затем куратор рассмотрит заявку.
                </p>
                <div className={styles.formRow}>
                  <Input label="ИНН" value={verifInn} onChange={e => setVerifInn(e.target.value)}
                    placeholder="10 или 12 цифр" maxLength={12} />
                  <Input label="Домен компании" value={verifDomain} onChange={e => setVerifDomain(e.target.value)}
                    placeholder="company.ru" />
                  <Input label="Корпоративная почта" type="email" value={verifEmail}
                    onChange={e => setVerifEmail(e.target.value)} placeholder="hr@company.ru" />
                </div>
                <div className={styles.formActions}>
                  <Button variant="primary" size="md" type="submit" isLoading={verifLoading}>
                    Отправить на проверку
                  </Button>
                  <Button variant="ghost" size="md" type="button" onClick={() => setShowVerifForm(false)}>
                    Отмена
                  </Button>
                </div>
              </form>
            )}

            {!showVerifForm && !verifRequest && (
              <p className={styles.placeholder}>
                Для создания вакансий необходимо пройти верификацию. Подайте заявку с ИНН и корпоративным email.
              </p>
            )}
          </section>
        )}

        {/* Профиль компании */}
        <section className={styles.card}>
          <div className={styles.cardHeader}>
            <h2 className={styles.cardTitle}>Профиль компании</h2>
            <div className={styles.cardActions}>
              <span
                className={styles.verificationBadge}
                style={{
                  color: verificationColors[vStatus],
                  borderColor: verificationColors[vStatus],
                }}
              >
                {verificationLabels[vStatus] || vStatus}
              </span>
              {!isEditing && (
                <Button variant="secondary" size="sm" onClick={() => setIsEditing(true)}>
                  Редактировать
                </Button>
              )}
            </div>
          </div>

          {isEditing ? (
            <form onSubmit={handleSave} className={styles.editForm}>
              <FileUpload
                label="Логотип компании"
                currentUrl={profile?.logoUrl}
                onUploaded={(url) => {
                  setProfile(prev => prev ? { ...prev, logoUrl: url } : prev);
                }}
              />
              <div className={styles.formRow}>
                <Input
                  label="Название компании "
                  value={form.companyName}
                  onChange={(e) => handleChange('companyName', e.target.value)}
                  placeholder="Сбербанк"
                />
                <Input
                  label="ИНН"
                  value={form.inn || ''}
                  onChange={(e) => handleChange('inn', e.target.value)}
                  placeholder="123456789012"
                />
                <Input
                  label="Сфера деятельности"
                  value={form.industry || ''}
                  onChange={(e) => handleChange('industry', e.target.value)}
                  placeholder="Разработка ПО"
                />
              </div>

              <div className={styles.formRow}>
                <Input
                  label="Город"
                  value={form.city || ''}
                  onChange={(e) => handleChange('city', e.target.value)}
                  placeholder="Москва"
                />
                <Input
                  label="Адрес"
                  value={form.address || ''}
                  onChange={(e) => handleChange('address', e.target.value)}
                  placeholder="ул. Измайловская, д. 1"
                />
                <Input
                  label="Сайт"
                  value={form.websiteUrl || ''}
                  onChange={(e) => handleChange('websiteUrl', e.target.value)}
                  placeholder="https://sber.ru"
                />
              </div>

              <div className={styles.formRow2}>
                <Input
                  label="Телефон"
                  type="tel"
                  value={form.phone || ''}
                  onChange={(e) => handleChange('phone', e.target.value)}
                  placeholder="+7 (495) 123-45-67"
                />
                <Input
                  label="Email компании"
                  type="email"
                  value={form.email || ''}
                  onChange={(e) => handleChange('email', e.target.value)}
                  placeholder="sber@mail.ru"
                />
              </div>

              <div className={styles.textareaGroup}>
                <label className={styles.textareaLabel}>Описание компании</label>
                <textarea
                  className={styles.textarea}
                  value={form.description || ''}
                  onChange={(e) => handleChange('description', e.target.value)}
                  placeholder="Расскажите о компании, проектах..."
                  rows={5}
                />
              </div>

              {/* Медиа */}
              <Input
                label="Видеопрезентация (URL)"
                value={form.videoUrl || ''}
                onChange={(e) => handleChange('videoUrl', e.target.value)}
                placeholder="https://youtube.com/... или https://rutube.ru/video/..."
              />

              <div className={styles.formActions}>
                <Button type="submit" isLoading={isSaving}>
                  Сохранить
                </Button>
                <Button type="button" variant="ghost" onClick={handleCancel}>
                  Отмена
                </Button>
              </div>
            </form>
          ) : (
            <div className={styles.profileView}>
              <div className={styles.profileSummary}>
                <div className={`${styles.avatar} ${styles.avatarCompany}`}>
                  {(profile?.companyName || user?.displayName || '?').charAt(0).toUpperCase()}
                </div>
                <div className={styles.profileInfo}>
                  <p className={styles.profileName}>
                    {profile?.companyName || 'Название не указано'}
                  </p>
                  <p className={styles.profileEmail}>{user?.email}</p>
                </div>
              </div>

              <div className={styles.detailsGrid}>
                {profile?.industry && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>Сфера</span>
                    <span className={styles.detailValue}>{profile.industry}</span>
                  </div>
                )}
                {profile?.inn && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>ИНН</span>
                    <span className={styles.detailValue}>{profile.inn}</span>
                  </div>
                )}
                {profile?.city && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>Город</span>
                    <span className={styles.detailValue}>{profile.city}</span>
                  </div>
                )}
                {profile?.address && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>Адрес</span>
                    <span className={styles.detailValue}>{profile.address}</span>
                  </div>
                )}
                {profile?.phone && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>Телефон</span>
                    <span className={styles.detailValue}>{profile.phone}</span>
                  </div>
                )}
                {profile?.email && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>Email</span>
                    <span className={styles.detailValue}>{profile.email}</span>
                  </div>
                )}
                {profile?.websiteUrl && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>Сайт</span>
                    <a
                      href={profile.websiteUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className={styles.detailLink}
                    >
                      {profile.websiteUrl}
                    </a>
                  </div>
                )}
                {profile?.description && (
                  <div className={`${styles.detailItem} ${styles.detailFull}`}>
                    <span className={styles.detailLabel}>Описание</span>
                    <span className={styles.detailValue}>{profile.description}</span>
                  </div>
                )}
              </div>
            </div>
          )}
        </section>

        {/* Мои возможности */}
        <section className={`${styles.card} ${styles.cardFull}`}>
          <div className={styles.cardHeader}>
            <h2 className={styles.cardTitle}><span className="material-symbols-rounded">list_alt</span> Мои возможности</h2>
            {!isNotVerified && (
              <Button size="sm" onClick={() => navigate('/company/opportunities/new')}>
                + Создать
              </Button>
            )}
          </div>

          {isNotVerified ? (
            <p className={styles.placeholder}>
              Создание вакансий станет доступно после верификации компании.
            </p>
          ) : oppsLoading ? (
            <p className={styles.placeholder}>Загрузка вакансий...</p>
          ) : opportunities.length === 0 ? (
            <div className={styles.emptyState}>
              <p>У вас пока нет вакансий.</p>
              <Button size="sm" onClick={() => navigate('/company/opportunities/new')}>
                Создать первую
              </Button>
            </div>
          ) : (
            <div className={styles.opportunitiesList}>
              {opportunities.map(opp => (
                <div key={opp.id} className={styles.oppCard}>
                  <div className={styles.oppMain}>
                    <div className={styles.oppTop}>
                      <span
                        className={styles.oppStatus}
                        style={{ color: statusColors[opp.status] || '#6b7280' }}
                      >
                        {statusLabels[opp.status] || opp.status}
                      </span>
                      <span className={styles.oppType}>
                        {typeLabels[opp.type] || opp.type}
                      </span>
                    </div>
                    <h3
                      className={styles.oppTitle}
                      onClick={() => navigate(`/opportunities/${opp.id}`)}
                    >
                      {opp.title}
                    </h3>
                    <div className={styles.oppMeta}>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                        <span className="material-symbols-rounded" style={{ fontSize: '16px' }}>
                          {opp.workFormat === 'OFFICE' ? 'apartment' : opp.workFormat === 'HYBRID' ? 'sync_alt' : 'home'}
                        </span>
                        {workFormatLabels[opp.workFormat] || opp.workFormat}
                      </span>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                        <span className="material-symbols-rounded" style={{ fontSize: '16px' }}>location_on</span>
                        {opp.city}
                      </span>
                      <span>{formatSalary(opp.salaryMin, opp.salaryMax)}</span>
                    </div>
                  </div>
                  <div className={styles.oppActions}>
                    <select
                      className={styles.statusSelect}
                      value={opp.status}
                      onChange={(e) => handleOppStatusChange(opp.id, e.target.value as 'ACTIVE' | 'CLOSED' | 'DRAFT')}
                      disabled={statusChangingId === opp.id}
                      title="Сменить статус"
                    >
                      <option value="DRAFT">Черновик</option>
                      <option value="ACTIVE">Активна</option>
                      <option value="CLOSED">Закрыта</option>
                    </select>
                    <button
                      className={styles.oppBtnEdit}
                      onClick={() => navigate(`/company/opportunities/edit/${opp.id}`)}
                      title="Редактировать"
                    >
                      <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>edit</span>
                    </button>
                    <button
                      className={styles.oppBtnEdit}
                      onClick={() => navigate(`/opportunities/${opp.id}`)}
                      title="Просмотр"
                    >
                      <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>visibility</span>
                    </button>
                    <button
                      className={styles.oppBtnDelete}
                      onClick={() => handleDeleteOpportunity(opp.id)}
                      disabled={deletingId === opp.id}
                      title="Удалить"
                    >
                      {deletingId === opp.id
                        ? <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>hourglass_top</span>
                        : <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>delete</span>
                      }
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Лучшие кандидаты (скоринг) */}
        {Object.keys(candidatesMap).length > 0 && (
          <section className={`${styles.card} ${styles.cardFull}`}>
            <div className={styles.cardHeader}>
              <h2 className={styles.cardTitle}>
                <span className="material-symbols-rounded">auto_awesome</span> Лучшие кандидаты
              </h2>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {opportunities.filter(o => candidatesMap[o.id]).map(opp => (
                <div key={opp.id}>
                  <div style={{ fontSize: '0.9rem', fontWeight: 600, marginBottom: '0.4rem', color: 'var(--color-text)' }}>
                    {opp.title}
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
                    {candidatesMap[opp.id].slice(0, 3).map(c => (
                      <div key={c.userId} className={styles.appRow}>
                        <div className={styles.appInfo}>
                          <span className={styles.appTitle} style={{ cursor: 'default' }}>{c.displayName}</span>
                        </div>
                        <span style={{
                          fontSize: '0.75rem', fontWeight: 700, padding: '0.15rem 0.5rem',
                          borderRadius: '999px', background: 'rgba(52,211,153,0.15)', color: '#059669',
                          flexShrink: 0,
                        }}>
                          {Math.round(c.score)}% совпадение
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}

        <section className={`${styles.card} ${styles.cardFull}`}>
          <div className={styles.cardHeader}>
            <h2 className={styles.cardTitle}><span className="material-symbols-rounded">group</span> Отклики</h2>
            <select
              className={styles.filterSelect}
              value={statusFilter || ''}
              onChange={(e) => setStatusFilter(e.target.value ? e.target.value as ApplicationStatus : undefined)}
            >
              <option value="">Все статусы</option>
              <option value="PENDING">Ожидание</option>
              <option value="REVIEWED">Просмотрен</option>
              <option value="ACCEPTED">Принят</option>
              <option value="REJECTED">Отклонён</option>
              <option value="RESERVED">В резерве</option>
            </select>
          </div>

          {appsLoading ? (
            <p className={styles.placeholder}>Загрузка откликов...</p>
          ) : apps.length === 0 ? (
            <p className={styles.placeholder}>
              {statusFilter ? 'Нет откликов с таким статусом' : 'Пока нет откликов на ваши вакансии'}
            </p>
          ) : (
            <div className={styles.appsTable}>
              {apps.map(app => (
                <div key={app.id} className={styles.appRowEmployer}>
                  <div className={styles.appInfo}>
                    <span className={styles.appTitle}
                      onClick={() => navigate(`/applicant/${app.applicantId}`)}>
                      {app.applicantFirstName} {app.applicantLastName}
                    </span>
                    <span className={styles.appCompany}>{app.applicantEmail}</span>
                    <span
                      className={styles.appOppLink}
                      onClick={() => navigate(`/opportunities/${app.opportunityId}`)}
                    >
                      {app.opportunityTitle}
                    </span>
                    {recsMap[app.opportunityId]?.some(r => r.recommendedName.includes(app.applicantFirstName)) && (
                      <span style={{
                        fontSize: '0.7rem', fontWeight: 600, padding: '0.1rem 0.4rem',
                        borderRadius: '999px', background: 'rgba(52,211,153,0.15)', color: '#059669',
                        display: 'inline-flex', alignItems: 'center', gap: '3px', width: 'fit-content',
                      }}>
                        <span className="material-symbols-rounded" style={{ fontSize: '12px' }}>recommend</span>
                        Рекомендован
                      </span>
                    )}
                  </div>
                  {app.coverLetter && (
                    <p className={styles.appCoverLetter}>{app.coverLetter}</p>
                  )}
                  <div className={styles.appActions}>
                    <span className={styles.appDate}>
                      {new Date(app.createdAt).toLocaleDateString('ru-RU')}
                    </span>
                    <select
                      className={styles.statusSelect}
                      value={app.status}
                      onChange={(e) => handleStatusChange(app.id, e.target.value as ApplicationStatus)}
                      disabled={updatingAppId === app.id}
                    >
                      <option value="PENDING">Ожидание</option>
                      <option value="REVIEWED">Просмотрен</option>
                      <option value="ACCEPTED">Принят</option>
                      <option value="REJECTED">Отклонён</option>
                      <option value="RESERVED">В резерве</option>
                    </select>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}

