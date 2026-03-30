// Страница личного кабинета соискателя

import { useState, useEffect, useCallback, type FormEvent } from 'react';
import { useAuth } from '../hooks/useAuth';
import { getApplicantProfile, updateApplicantProfile } from '../api/applicant';
import { getErrorMessage, changePassword } from '../api/client';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import type { ApplicantProfileResponse, UpdateApplicantRequest } from '../types';
import styles from './Dashboard.module.css';
import { useFavorites } from '../hooks/useFavorites';
import { getOpportunityById } from '../api/opportunities';
import type { OpportunityResponse } from '../types';
import { useNavigate } from 'react-router-dom';
import { getMyApplications } from '../api/applications';
import type { ApplicationResponse } from '../types';
import type { ContactResponse, ContactRequestResponse } from '../types';
import { getMyContacts, getIncomingContactRequests, respondToContactRequest, removeContact } from '../api/contacts';
import { getPrivacySettings, updatePrivacySettings, type PrivacySettings, type Visibility } from '../api/privacy';
import { SkeletonProfile } from '../components/ui/Skeleton';
import { getRecommendationsForMe, type RecommendationResponse } from '../api/recommendations';
import { getRecommendationsForApplicant, type OpportunityScore } from '../api/scoring';
import { FileUpload } from '../components/ui/FileUpload';
import { getTags } from '../api/tags';
import { getApplicantTags, updateApplicantTags } from '../api/applicant';
import type { Tag } from '../types';



export function ApplicantDashboard() {
  const { user } = useAuth();

  // Состояния
  const [profile, setProfile] = useState<ApplicantProfileResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);


  const navigate = useNavigate();
  const { favorites, removeFavorite } = useFavorites();
  const [favOpportunities, setFavOpportunities] = useState<OpportunityResponse[]>([]);
  const [favsLoading, setFavsLoading] = useState(false);
  const [applications, setApplications] = useState<ApplicationResponse[]>([]);
  const [appsLoading, setAppsLoading] = useState(false);


  // Контакты и нетворкинг
  const [contacts, setContacts] = useState<ContactResponse[]>([]);
  const [incomingRequests, setIncomingRequests] = useState<ContactRequestResponse[]>([]);
  const [contactsLoading, setContactsLoading] = useState(false);
  const [contactsTab, setContactsTab] = useState<'contacts' | 'requests' | 'recommendations' | 'privacy'>('contacts');
  const [contactActionLoading, setContactActionLoading] = useState<string | null>(null);

  // Приватность
  const [privacy, setPrivacy] = useState<PrivacySettings | null>(null);
  const [privacyLoading, setPrivacyLoading] = useState(false);
  const [privacySaving, setPrivacySaving] = useState(false);

  // Рекомендации
  const [recommendations, setRecommendations] = useState<RecommendationResponse[]>([]);
  const [recsLoading, setRecsLoading] = useState(false);

  const [allTags, setAllTags] = useState<Tag[]>([]);
  const [selectedTagIds, setSelectedTagIds] = useState<string[]>([]);

  const [scoredOpps, setScoredOpps] = useState<OpportunityScore[]>([]);
  const [scoringLoading, setScoringLoading] = useState(false);

  // Смена пароля
  const [pwOld, setPwOld] = useState('');
  const [pwNew, setPwNew] = useState('');
  const [pwConfirm, setPwConfirm] = useState('');
  const [pwLoading, setPwLoading] = useState(false);
  const [pwError, setPwError] = useState<string | null>(null);
  const [pwSuccess, setPwSuccess] = useState(false);



  // Загрузка данных избранных вакансий
  useEffect(() => {
    async function loadFavorites() {
      const oppIds = favorites
        .filter(f => f.type === 'OPPORTUNITY')
        .map(f => f.id);
      if (oppIds.length === 0) {
        setFavOpportunities([]);
        return;
      }
      setFavsLoading(true);
      try {
        const results = await Promise.all(
          oppIds.map(id => getOpportunityById(id).catch(() => null))
        );
        setFavOpportunities(results.filter(Boolean) as OpportunityResponse[]);
      } catch (err) {
        console.error('Ошибка загрузки избранного:', err);
      } finally {
        setFavsLoading(false);
      }
    }
    loadFavorites();
  }, [favorites]);


  useEffect(() => {
    async function loadApplications() {
      setAppsLoading(true);
      try {
        const data = await getMyApplications(0, 50);
        setApplications(data.content || []);
      } catch (err) {
        console.error('Ошибка загрузки откликов:', err);
      } finally {
        setAppsLoading(false);
      }
    }
    loadApplications();
  }, []);



  // Загрузка контактов
  const loadContacts = useCallback(async () => {
    setContactsLoading(true);
    try {
      const [contactsData, requestsData] = await Promise.all([
        getMyContacts(),
        getIncomingContactRequests(),
      ]);
      setContacts(contactsData);
      setIncomingRequests(requestsData);
    } catch (err) {
      console.error('Ошибка загрузки контактов:', err);
    } finally {
      setContactsLoading(false);
    }
  }, []);

  useEffect(() => { loadContacts(); }, [loadContacts]);

  // Загрузка настроек приватности
  useEffect(() => {
    async function loadPrivacy() {
      setPrivacyLoading(true);
      try {
        const data = await getPrivacySettings();
        setPrivacy(data);
      } catch (err) {
        console.error('Ошибка загрузки приватности:', err);
      } finally {
        setPrivacyLoading(false);
      }
    }
    loadPrivacy();
  }, []);

  // Загрузка рекомендаций
  useEffect(() => {
    async function loadRecs() {
      setRecsLoading(true);
      try {
        const data = await getRecommendationsForMe();
        setRecommendations(data);
      } catch (err) {
        console.error('Ошибка загрузки рекомендаций:', err);
      } finally {
        setRecsLoading(false);
      }
    }
    loadRecs();
  }, []);

  // Загрузка скоринга — подобранные вакансии
  useEffect(() => {
    async function loadScoring() {
      setScoringLoading(true);
      try {
        const data = await getRecommendationsForApplicant();
        setScoredOpps(data);
      } catch (err) {
        console.error('Ошибка загрузки скоринга:', err);
      } finally {
        setScoringLoading(false);
      }
    }
    loadScoring();
  }, []);

  async function handleRespondContact(requestId: string, status: 'ACCEPTED' | 'REJECTED') {
    try {
      setContactActionLoading(requestId);
      await respondToContactRequest(requestId, status);
      setIncomingRequests(prev => prev.filter(r => r.id !== requestId));
      if (status === 'ACCEPTED') loadContacts();
    } catch (err) {
      alert(getErrorMessage(err));
    } finally {
      setContactActionLoading(null);
    }
  }

  async function handleRemoveContact(contactRequestId: string) {
    if (!confirm('Удалить этот контакт?')) return;
    try {
      setContactActionLoading(contactRequestId);
      await removeContact(contactRequestId);
      setContacts(prev => prev.filter(c => c.contactRequestId !== contactRequestId));
    } catch (err) {
      alert(getErrorMessage(err));
    } finally {
      setContactActionLoading(null);
    }
  }

  async function handlePrivacyChange(field: keyof PrivacySettings, value: Visibility) {
    if (!privacy) return;
    setPrivacy({ ...privacy, [field]: value });
    try {
      setPrivacySaving(true);
      await updatePrivacySettings({ [field]: value });
    } catch (err) {
      alert(getErrorMessage(err));
    } finally {
      setPrivacySaving(false);
    }
  }

  async function handleChangePassword(e: FormEvent) {
    e.preventDefault();
    setPwError(null);
    setPwSuccess(false);
    if (pwNew.length < 6) { setPwError('Минимум 6 символов'); return; }
    if (pwNew !== pwConfirm) { setPwError('Пароли не совпадают'); return; }
    setPwLoading(true);
    try {
      await changePassword(pwOld, pwNew);
      setPwSuccess(true);
      setPwOld(''); setPwNew(''); setPwConfirm('');
      setTimeout(() => setPwSuccess(false), 3000);
    } catch (err) {
      setPwError(getErrorMessage(err));
    } finally {
      setPwLoading(false);
    }
  }

  // Данные формы редактирования
  const [form, setForm] = useState<UpdateApplicantRequest>({
    firstName: '',
    lastName: '',
    middleName: '',
    university: '',
    course: undefined,
    graduationYear: undefined,
    bio: '',
    phone: '',
    portfolioUrl: '',
    githubUrl: '',
    skillsSummary: '',
  });

  // Загрузка профиля при монтировании
  useEffect(() => {
    loadProfile();
  }, []);

  async function loadProfile() {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getApplicantProfile();
      setProfile(data);
      // Заполняем форму текущими данными
      setForm({
        firstName: data.firstName || '',
        lastName: data.lastName || '',
        middleName: data.middleName || '',
        university: data.university || '',
        course: data.course ?? undefined,
        graduationYear: data.graduationYear ?? undefined,
        bio: data.bio || '',
        phone: data.phone || '',
        portfolioUrl: data.portfolioUrl || '',
        githubUrl: data.githubUrl || '',
        skillsSummary: data.skillsSummary || '',
      });
      getTags().then(setAllTags).catch(console.error);
      getApplicantTags()
        .then(tags => setSelectedTagIds(tags.map((t: any) => t.id)))
        .catch(console.error);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }

  // Обработка полей формы
  function handleChange(field: keyof UpdateApplicantRequest, value: string) {
    setForm((prev) => ({
      ...prev,
      [field]: field === 'course' || field === 'graduationYear'
        ? (value === '' ? undefined : Number(value))
        : value,
    }));
  }

  // Сохранение
  async function handleSave(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);

    // Клиентская валидация
    if (!form.firstName.trim() || !form.lastName.trim()) {
      setError('Имя и фамилия обязательны');
      return;
    }
    if (form.course !== undefined && (form.course < 1 || form.course > 6)) {
      setError('Курс должен быть от 1 до 6');
      return;
    }

    setIsSaving(true);
    try {
      const updated = await updateApplicantProfile(form);
      if (selectedTagIds.length > 0) {
        await updateApplicantTags(selectedTagIds);
      }
      setProfile(updated);
      setIsEditing(false);
      setSuccessMsg('Профиль сохранён');
      // скрыть сообщение черзе 3 секунды
      setTimeout(() => setSuccessMsg(null), 3000);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsSaving(false);
    }
  }

  // Отмена редактирования
  function handleCancel() {
    if (profile) {
      setForm({
        firstName: profile.firstName || '',
        lastName: profile.lastName || '',
        middleName: profile.middleName || '',
        university: profile.university || '',
        course: profile.course ?? undefined,
        graduationYear: profile.graduationYear ?? undefined,
        bio: profile.bio || '',
        phone: profile.phone || '',
        portfolioUrl: profile.portfolioUrl || '',
        githubUrl: profile.githubUrl || '',
        skillsSummary: profile.skillsSummary || '',
      });
    }
    setIsEditing(false);
    setError(null);
  }

  // Индикатор загрузки
  if (isLoading) {
    return (
      <main className={styles.dashboard}>
        <div className={styles.container}>
          <div className={styles.container}><SkeletonProfile /></div>
        </div>
      </main>
    );
  }

  // Полное имя для отображения
  const fullName = profile
    ? [profile.lastName, profile.firstName, profile.middleName].filter(Boolean).join(' ')
    : user?.displayName || '';


  const statusLabels: Record<string, string> = {
    PENDING: 'Ожидание',
    REVIEWED: 'Просмотрен',
    ACCEPTED: 'Принят',
    REJECTED: 'Отклонён',
    RESERVED: 'В резерве',
  };

  const statusColors: Record<string, string> = {
    PENDING: '#d97706',
    REVIEWED: '#2563eb',
    ACCEPTED: '#059669',
    REJECTED: '#dc2626',
    RESERVED: '#6b7280',
  };



  return (
    <main className={styles.dashboard}>
      <div className={styles.container}>
        <header className={styles.pageHeader}>
          <h1 className={styles.pageTitle}>Личный кабинет</h1>
          <span className={styles.roleBadge}>Соискатель</span>
        </header>

        {/* Сообщения */}
        {successMsg && <div className={styles.successBanner}>{successMsg}</div>}
        {error && <div className={styles.errorBanner}>{error}</div>}

        {/* Карточка профиля */}
        <section className={styles.card}>
          <div className={styles.cardHeader}>
            <h2 className={styles.cardTitle}>Мой профиль</h2>
            {!isEditing && (
              <Button variant="secondary" size="sm" onClick={() => setIsEditing(true)}>
                Редактировать
              </Button>
            )}
          </div>

          {isEditing ? (
            /* Режим редактирования */
            <form onSubmit={handleSave} className={styles.editForm}>
              <FileUpload
                label="Аватар"
                currentUrl={profile?.avatarUrl}
                onUploaded={(url) => {
                  setProfile(prev => prev ? { ...prev, avatarUrl: url } : prev);
                }}
              />
              <div className={styles.formRow}>
                <Input
                  label="Фамилия *"
                  value={form.lastName}
                  onChange={(e) => handleChange('lastName', e.target.value)}
                  placeholder="Киреев"
                />
                <Input
                  label="Имя *"
                  value={form.firstName}
                  onChange={(e) => handleChange('firstName', e.target.value)}
                  placeholder="Влад"
                />
                <Input
                  label="Отчество"
                  value={form.middleName || ''}
                  onChange={(e) => handleChange('middleName', e.target.value)}
                  placeholder="Алексеевич"
                />
              </div>

              <div className={styles.formRow}>
                <Input
                  label="ВУЗ"
                  value={form.university || ''}
                  onChange={(e) => handleChange('university', e.target.value)}
                  placeholder="МГТУ им. Н.Э Баумана"
                />
                <Input
                  label="Курс (1-6)"
                  type="number"
                  value={form.course?.toString() || ''}
                  onChange={(e) => handleChange('course', e.target.value)}
                  placeholder="1"
                />
                <Input
                  label="Год выпуска"
                  type="number"
                  value={form.graduationYear?.toString() || ''}
                  onChange={(e) => handleChange('graduationYear', e.target.value)}
                  placeholder="2026"
                />
              </div>

              <Input
                label="Телефон"
                type="tel"
                value={form.phone || ''}
                onChange={(e) => handleChange('phone', e.target.value)}
                placeholder="+7 (901) 243-35-39"
              />

              <div className={styles.textareaGroup}>
                <label className={styles.textareaLabel}>О себе</label>
                <textarea
                  className={styles.textarea}
                  value={form.bio || ''}
                  onChange={(e) => handleChange('bio', e.target.value)}
                  placeholder="Расскажите о себе, о своих навыках и целях..."
                  rows={4}
                />
              </div>

              {/* Портфолио */}
              <div className={styles.textareaGroup}>
                  <label className={styles.textareaLabel}>Навыки (теги)</label>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
                    {allTags.map(tag => {
                      const isSelected = selectedTagIds.includes(tag.id);
                      return (
                        <button
                          key={tag.id}
                          type="button"
                          onClick={() => {
                            setSelectedTagIds(prev =>
                              isSelected
                                ? prev.filter(id => id !== tag.id)
                                : [...prev, tag.id]
                            );
                          }}
                          style={{
                            padding: '0.25rem 0.6rem',
                            borderRadius: '999px',
                            border: isSelected ? '2px solid var(--color-accent)' : '1px solid var(--color-border)',
                            background: isSelected ? 'var(--color-accent-light)' : 'transparent',
                            color: isSelected ? 'var(--color-accent)' : 'var(--color-text-secondary)',
                            cursor: 'pointer',
                            fontSize: '0.8rem',
                            fontWeight: isSelected ? 700 : 400,
                          }}
                        >
                          {tag.name}
                        </button>
                      );
                    })}
                  </div>
                </div>

              <div className={styles.formRow}>
                <Input
                  label="Портфолио (URL)"
                  value={form.portfolioUrl || ''}
                  onChange={(e) => handleChange('portfolioUrl', e.target.value)}
                  placeholder="https://myportfolio.com"
                />
                <Input
                  label="GitHub"
                  value={form.githubUrl || ''}
                  onChange={(e) => handleChange('githubUrl', e.target.value)}
                  placeholder="https://github.com/username"
                />
              </div>

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
                <div className={styles.avatar}>
                  {profile?.avatarUrl
                    ? <img src={profile.avatarUrl} alt="" style={{
                        width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover',
                      }} />
                    : (profile?.firstName || user?.displayName || '?').charAt(0).toUpperCase()
                  }
                </div>
                <div className={styles.profileInfo}>
                  <p className={styles.profileName}>
                    {fullName || 'Имя не указано'}
                  </p>
                  <p className={styles.profileEmail}>{user?.email}</p>
                </div>
              </div>

              <div className={styles.detailsGrid}>
                {profile?.university && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>ВУЗ</span>
                    <span className={styles.detailValue}>{profile.university}</span>
                  </div>
                )}
                {profile?.course && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>Курс</span>
                    <span className={styles.detailValue}>{profile.course}</span>
                  </div>
                )}
                {profile?.graduationYear && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>Год выпуска</span>
                    <span className={styles.detailValue}>{profile.graduationYear}</span>
                  </div>
                )}
                {profile?.phone && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>Телефон</span>
                    <span className={styles.detailValue}>{profile.phone}</span>
                  </div>
                )}
                {profile?.bio && (
                  <div className={`${styles.detailItem} ${styles.detailFull}`}>
                    <span className={styles.detailLabel}>О себе</span>
                    <span className={styles.detailValue}>{profile.bio}</span>
                  </div>
                )}
                {profile?.skillsSummary && (
                  <div className={`${styles.detailItem} ${styles.detailFull}`}>
                    <span className={styles.detailLabel}>Навыки</span>
                    <span className={styles.detailValue}>{profile.skillsSummary}</span>
                  </div>
                )}
                {profile?.portfolioUrl && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>Портфолио</span>
                    <a href={profile.portfolioUrl} target="_blank" rel="noopener noreferrer" className={styles.detailLink || styles.detailValue}>
                      {profile.portfolioUrl}
                    </a>
                  </div>
                )}
                {profile?.githubUrl && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>GitHub</span>
                    <a href={profile.githubUrl} target="_blank" rel="noopener noreferrer" className={styles.detailLink || styles.detailValue}>
                      {profile.githubUrl}
                    </a>
                  </div>
                )}
                {profile?.tags && profile.tags.length > 0 && (
                  <div className={`${styles.detailItem} ${styles.detailFull}`}>
                    <span className={styles.detailLabel}>Теги / Навыки</span>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.3rem' }}>
                      {profile.tags.map((tag, i) => (
                        <span key={i} style={{
                          padding: '0.15rem 0.5rem', borderRadius: '999px',
                          background: 'var(--color-accent-light)', color: 'var(--color-accent)',
                          fontSize: '0.8rem', fontWeight: 600,
                        }}>{tag}</span>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </section>

        {/* Рекомендованные вакансии (скоринг) */}
        {!scoringLoading && scoredOpps.length > 0 && (
          <section className={styles.card} style={{ marginBottom: '1.25rem' }}>
            <div className={styles.cardHeader}>
              <h2 className={styles.cardTitle}>
                <span className="material-symbols-rounded">auto_awesome</span> Подобранные для вас
              </h2>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              {scoredOpps.slice(0, 5).map(opp => (
                <div key={opp.opportunityId} className={styles.appRow}
                  style={{ cursor: 'pointer' }}
                  onClick={() => navigate(`/opportunities/${opp.opportunityId}`)}>
                  <div className={styles.appInfo}>
                    <span className={styles.appTitle}>{opp.title}</span>
                    <span className={styles.appCompany}>{opp.companyName}</span>
                  </div>
                  <span style={{
                    fontSize: '0.75rem', fontWeight: 700, padding: '0.15rem 0.5rem',
                    borderRadius: '999px', background: 'var(--color-accent-light)',
                    color: 'var(--color-accent)', flexShrink: 0,
                  }}>
                    {Math.round(opp.score)}% совпадение
                  </span>
                </div>
              ))}
            </div>
          </section>
        )}

        <div className={styles.grid}>
          <section className={styles.card}>
            <div className={styles.cardHeader}>
              <h2 className={styles.cardTitle}><span className="material-symbols-rounded">patient_list</span> Мои отклики</h2>
            </div>
            {appsLoading ? (
              <p className={styles.placeholder}>Загрузка...</p>
            ) : applications.length === 0 ? (
              <p className={styles.placeholder}>У вас пока нет откликов</p>
            ) : (
              <div className={styles.appsTable}>
                {applications.map(app => (
                  <div key={app.id} className={styles.appRow}>
                    <div className={styles.appInfo}>
                      <span
                        className={styles.appTitle}
                        onClick={() => navigate(`/opportunities/${app.opportunityId}`)}
                      >
                        {app.opportunityTitle}
                      </span>
                      <span className={styles.appCompany}>{app.companyName}</span>
                    </div>
                    <div className={styles.appMeta}>
                      <span
                        className={styles.appStatus}
                        style={{
                          color: statusColors[app.status] || '#6b7280',
                          borderColor: statusColors[app.status] || '#6b7280',
                        }}
                      >
                        {statusLabels[app.status] || app.status}
                      </span>
                      <span className={styles.appDate}>
                        {new Date(app.createdAt).toLocaleDateString('ru-RU')}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>

          <section className={styles.card}>
            <div className={styles.cardHeader}>
              <h2 className={styles.cardTitle}><span className="material-symbols-rounded">bookmarks</span> Избранное</h2>
            </div>
            {favsLoading ? (
              <p className={styles.placeholder}>Загрузка...</p>
            ) : favOpportunities.length === 0 ? (
              <p className={styles.placeholder}>Нет сохранённых вакансий</p>
            ) : (
              <div className={styles.favList}>
                {favOpportunities.map(opp => (
                  <div key={opp.id} className={styles.favItem}>
                    <div
                      className={styles.favInfo}
                      onClick={() => navigate(`/opportunities/${opp.id}`)}
                    >
                      <span className={styles.favTitle}>{opp.title}</span>
                      <span className={styles.favCompany}>{opp.companyName} · {opp.city}</span>
                    </div>
                    <button
                      className={styles.favRemove}
                      onClick={() => removeFavorite(opp.id)}
                      title="Убрать из избранного"
                    >
                      <span className="material-symbols-rounded" style={{ fontSize: '16px' }}>close</span>
                    </button>
                  </div>
                ))}
              </div>
            )}
          </section>

          <section className={styles.card}>
            <div className={styles.cardHeader}>
              <h2 className={styles.cardTitle}><span className="material-symbols-rounded">group</span> Нетворкинг</h2>
            </div>

            {/* Подвкладки */}
            <div style={{
              display: 'flex', gap: '0.25rem', marginBottom: '1rem',
              borderBottom: '1px solid var(--color-border)',
            }}>
              {([
                { key: 'contacts' as const, label: 'Контакты', count: contacts.length },
                { key: 'requests' as const, label: 'Запросы', count: incomingRequests.length },
                { key: 'recommendations' as const, label: 'Рекомендации', count: recommendations.length },
                { key: 'privacy' as const, label: 'Приватность', count: 0 },
              ]).map(tab => (
                <button
                  key={tab.key}
                  onClick={() => setContactsTab(tab.key)}
                  style={{
                    padding: '0.5rem 0.75rem', fontSize: '0.85rem',
                    fontWeight: contactsTab === tab.key ? 600 : 400,
                    color: contactsTab === tab.key ? 'var(--color-accent)' : 'var(--color-text-secondary)',
                    background: 'none', border: 'none', cursor: 'pointer',
                    borderBottom: contactsTab === tab.key ? '2px solid var(--color-accent)' : '2px solid transparent',
                  }}
                >
                  {tab.label}
                  {tab.count > 0 && (
                    <span style={{
                      marginLeft: '0.3rem', fontSize: '0.7rem', fontWeight: 700,
                      background: tab.key === 'requests' ? '#dc262618' : 'var(--color-bg-subtle)',
                      color: tab.key === 'requests' ? '#dc2626' : 'var(--color-text-secondary)',
                      padding: '0.1rem 0.4rem', borderRadius: '999px',
                    }}>{tab.count}</span>
                  )}
                </button>
              ))}
            </div>

            {/* Мои контакты */}
            {contactsTab === 'contacts' && (
              contactsLoading ? (
                <p className={styles.placeholder}>Загрузка...</p>
              ) : contacts.length === 0 ? (
                <div className={styles.emptyState}>
                  <span className="material-symbols-rounded" style={{ fontSize: '36px', opacity: 0.4 }}>person_add</span>
                  <p>У вас пока нет контактов</p>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {contacts.map(c => (
                    <div key={c.contactRequestId} className={styles.appRow}>
                      <div className={styles.appInfo}>
                        <span className={styles.appTitle} style={{ cursor: 'default' }}>{c.displayName}</span>
                        <span className={styles.appCompany}>{c.email}</span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <span style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)' }}>
                          {new Date(c.connectedAt).toLocaleDateString('ru-RU')}
                        </span>
                        <button className={styles.favRemove}
                          onClick={() => handleRemoveContact(c.contactRequestId)}
                          disabled={contactActionLoading === c.contactRequestId}
                          title="Удалить контакт">
                          <span className="material-symbols-rounded" style={{ fontSize: '16px' }}>close</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )
            )}

            {/* Входящие запросы */}
            {contactsTab === 'requests' && (
              incomingRequests.length === 0 ? (
                <div className={styles.emptyState}>
                  <span className="material-symbols-rounded" style={{ fontSize: '36px', opacity: 0.4 }}>mail</span>
                  <p>Нет входящих запросов</p>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {incomingRequests.map(req => (
                    <div key={req.id} className={styles.appRow}>
                      <div className={styles.appInfo}>
                        <span className={styles.appTitle} style={{ cursor: 'default' }}>{req.senderDisplayName}</span>
                        <span className={styles.appCompany}>{req.senderEmail}</span>
                      </div>
                      <div style={{ display: 'flex', gap: '0.4rem' }}>
                        <Button size="sm" variant="primary"
                          isLoading={contactActionLoading === req.id}
                          onClick={() => handleRespondContact(req.id, 'ACCEPTED')}>
                          Принять
                        </Button>
                        <Button size="sm" variant="ghost"
                          disabled={contactActionLoading === req.id}
                          onClick={() => handleRespondContact(req.id, 'REJECTED')}>
                          Отклонить
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              )
            )}

            {/* Рекомендации */}
            {contactsTab === 'recommendations' && (
              recsLoading ? (
                <p className={styles.placeholder}>Загрузка...</p>
              ) : recommendations.length === 0 ? (
                <div className={styles.emptyState}>
                  <span className="material-symbols-rounded" style={{ fontSize: '36px', opacity: 0.4 }}>recommend</span>
                  <p>Вас пока никто не рекомендовал</p>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {recommendations.map(rec => (
                    <div key={rec.id} className={styles.appRow}>
                      <div className={styles.appInfo}>
                        <span className={styles.appTitle} style={{ cursor: 'default' }}>
                          {rec.opportunityTitle}
                        </span>
                        <span className={styles.appCompany}>
                          {rec.companyName} — рекомендовал(а) {rec.recommenderName}
                        </span>
                        {rec.message && (
                          <span style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', marginTop: '0.25rem' }}>
                            «{rec.message}»
                          </span>
                        )}
                      </div>
                      <span style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', flexShrink: 0 }}>
                        {new Date(rec.createdAt).toLocaleDateString('ru-RU')}
                      </span>
                    </div>
                  ))}
                </div>
              )
            )}

            {/* Приватность */}
            {contactsTab === 'privacy' && (
              privacyLoading ? (
                <p className={styles.placeholder}>Загрузка...</p>
              ) : !privacy ? (
                <p className={styles.placeholder}>Не удалось загрузить настройки</p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <p style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', margin: 0 }}>
                    Настройте, кто может видеть разделы вашего профиля.
                    {privacySaving && <span style={{ marginLeft: '0.5rem', color: 'var(--color-accent)' }}>Сохранение...</span>}
                  </p>
                  {([
                    { key: 'profileVisibility' as const, label: 'Профиль', desc: 'ФИО, вуз, фото' },
                    { key: 'resumeVisibility' as const, label: 'Резюме', desc: 'Навыки, опыт, портфолио' },
                    { key: 'applicationsVisibility' as const, label: 'Отклики', desc: 'На какие вакансии откликнулись' },
                    { key: 'contactsVisibility' as const, label: 'Контакты', desc: 'Список контактов' },
                  ]).map(item => (
                    <div key={item.key} style={{
                      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                      padding: '0.75rem', background: 'var(--color-bg)', borderRadius: '10px',
                    }}>
                      <div>
                        <div style={{ fontSize: '0.9rem', fontWeight: 600 }}>{item.label}</div>
                        <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)' }}>{item.desc}</div>
                      </div>
                      <select
                        value={privacy[item.key]}
                        onChange={e => handlePrivacyChange(item.key, e.target.value as Visibility)}
                        className={styles.filterSelect}
                        style={{ minWidth: '160px' }}
                      >
                        <option value="ALL">Все авторизованные</option>
                        <option value="CONTACTS_ONLY">Только контакты</option>
                        <option value="EMPLOYERS_ONLY">Только работодатели</option>
                        <option value="NOBODY">Никто</option>
                      </select>
                    </div>
                  ))}
                </div>
              )
            )}
          </section>
        </div>

        {/* Безопасность — смена пароля */}
        <section className={styles.card} style={{ marginTop: '1.25rem' }}>
          <div className={styles.cardHeader}>
            <h2 className={styles.cardTitle}>
              <span className="material-symbols-rounded">lock</span> Безопасность
            </h2>
          </div>
          {pwSuccess && <div className={styles.successBanner}>Пароль успешно изменён</div>}
          {pwError && <div className={styles.errorBanner}>{pwError}</div>}
          <form onSubmit={handleChangePassword} className={styles.editForm}>
            <div className={styles.formRow}>
              <Input label="Текущий пароль" type="password" value={pwOld}
                onChange={e => setPwOld(e.target.value)} placeholder="••••••••" />
              <Input label="Новый пароль" type="password" value={pwNew}
                onChange={e => setPwNew(e.target.value)} placeholder="Минимум 8 символов" />
              <Input label="Подтвердите пароль" type="password" value={pwConfirm}
                onChange={e => setPwConfirm(e.target.value)} placeholder="Повторите новый пароль" />
            </div>
            <div className={styles.formActions}>
              <Button type="submit" size="sm" isLoading={pwLoading}>Сменить пароль</Button>
            </div>
          </form>
        </section>
      </div>
    </main>
  );
}