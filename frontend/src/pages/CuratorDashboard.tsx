// Страница личного кабинета Куратора


import { useState, useEffect, useCallback, type FormEvent } from 'react';
import { useAuth } from '../hooks/useAuth';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { getErrorMessage } from '../api/client';
import { getUsers, createCurator, getPendingVerifications, approveVerification, rejectVerification, hideOpportunity, unhideOpportunity, blockUser, unblockUser, getModerationLogs, getPendingTags, approveTag, rejectTag, createTagByCurator, resetUserPassword } from '../api/curator';
import { getOpportunities } from '../api/opportunities';
import { TagCategory } from '../types';
import type { UserManagementResponse, VerificationRequestResponse, ModerationLogResponse, Tag, OpportunityResponse, UserRole, AccountStatus } from '../types';
import styles from './Dashboard.module.css';
import { SkeletonProfile } from '../components/ui/Skeleton';




type TabKey = 'verification' | 'moderation' | 'tags' | 'users' | 'logs' | 'curators';

interface TabDef {
  key: TabKey;
  label: string;
  icon: string;
  adminOnly?: boolean;
}

const TABS: TabDef[] = [
  { key: 'verification', label: 'Верификация', icon: 'verified' },
  { key: 'moderation', label: 'Модерация', icon: 'shield' },
  { key: 'tags', label: 'Теги', icon: 'sell' },
  { key: 'users', label: 'Пользователи', icon: 'group' },
  { key: 'logs', label: 'Журнал', icon: 'history' },
  { key: 'curators', label: 'Кураторы', icon: 'admin_panel_settings', adminOnly: true },
];

const ROLE_LABELS: Record<string, string> = {
  APPLICANT: 'Соискатель',
  EMPLOYER: 'Работодатель',
  CURATOR: 'Куратор',
  ADMIN: 'Администратор',
};

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Активен',
  PENDING_VERIFICATION: 'Ожидает верификации',
  BLOCKED: 'Заблокирован',
};

const STATUS_COLORS: Record<string, string> = {
  ACTIVE: '#059669',
  PENDING_VERIFICATION: '#d97706',
  BLOCKED: '#dc2626',
};

const VERIF_STATUS_LABELS: Record<string, string> = {
  PENDING: 'На рассмотрении',
  INN_VERIFIED: 'ИНН подтверждён',
  EMAIL_VERIFIED: 'Email подтверждён',
  APPROVED: 'Одобрена',
  REJECTED: 'Отклонена',
};

const VERIF_STATUS_COLORS: Record<string, string> = {
  PENDING: '#d97706',
  INN_VERIFIED: '#2563eb',
  EMAIL_VERIFIED: '#7c3aed',
  APPROVED: '#059669',
  REJECTED: '#dc2626',
};

const ACTION_LABELS: Record<string, string> = {
  EDIT: 'Редактирование',
  HIDE: 'Скрытие',
  UNHIDE: 'Восстановление',
  BLOCK_USER: 'Блокировка',
  UNBLOCK_USER: 'Разблокировка',
  DELETE: 'Удаление',
};

const TARGET_LABELS: Record<string, string> = {
  OPPORTUNITY: 'Вакансия',
  USER: 'Пользователь',
  COMPANY: 'Компания',
};

const TAG_CATEGORY_LABELS: Record<string, string> = {
  LANGUAGE: 'Язык',
  FRAMEWORK: 'Фреймворк',
  LEVEL: 'Уровень',
  SPECIALIZATION: 'Специализация',
  EMPLOYMENT_TYPE: 'Тип занятости',
  TOOL: 'Инструмент',
  DATABASE: 'База данных',
};

function formatDate(iso: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function formatDateTime(iso: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleString('ru-RU', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}


export function CuratorDashboard() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const [activeTab, setActiveTab] = useState<TabKey>('verification');

  const visibleTabs = TABS.filter(t => !t.adminOnly || isAdmin);

  return (
    <main className={styles.dashboard}>
      <div className={styles.container}>
        <header className={styles.pageHeader}>
          <h1 className={styles.pageTitle}>Панель управления</h1>
          <span className={`${styles.roleBadge} ${styles.roleBadgeCurator}`}>
            {isAdmin ? 'Администратор' : 'Куратор'}
          </span>
        </header>

        <nav style={{
          display: 'flex', gap: '0.25rem', marginBottom: '1.5rem',
          borderBottom: '1px solid var(--color-border)', paddingBottom: '0',
          overflowX: 'auto',
        }}>
          {visibleTabs.map(tab => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              style={{
                display: 'flex', alignItems: 'center', gap: '0.4rem',
                padding: '0.65rem 1rem',
                fontSize: '0.9rem', fontWeight: activeTab === tab.key ? 600 : 400,
                color: activeTab === tab.key ? 'var(--color-accent)' : 'var(--color-text-secondary)',
                background: 'none', border: 'none', cursor: 'pointer',
                borderBottom: activeTab === tab.key ? '2px solid var(--color-accent)' : '2px solid transparent',
                transition: 'all 0.15s ease',
                whiteSpace: 'nowrap',
              }}
            >
              <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>{tab.icon}</span>
              {tab.label}
            </button>
          ))}
        </nav>

        {activeTab === 'verification' && <VerificationTab />}
        {activeTab === 'moderation' && <ModerationTab />}
        {activeTab === 'tags' && <TagsTab />}
        {activeTab === 'users' && <UsersTab />}
        {activeTab === 'logs' && <LogsTab />}
        {activeTab === 'curators' && isAdmin && <CuratorsTab />}
      </div>
    </main>
  );
}


// Верификация компаний
function VerificationTab() {
  const [requests, setRequests] = useState<VerificationRequestResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const [rejectingId, setRejectingId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState('');

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getPendingVerifications(0, 50);
      setRequests(data.content);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  const handleApprove = async (id: string) => {
    if (!confirm('Одобрить верификацию этой компании?')) return;
    try {
      setActionLoading(id);
      await approveVerification(id);
      setRequests(prev => prev.filter(r => r.id !== id));
    } catch (e) {
      alert(getErrorMessage(e));
    } finally {
      setActionLoading(null);
    }
  };

  const handleReject = async () => {
    if (!rejectingId || !rejectReason.trim()) return;
    try {
      setActionLoading(rejectingId);
      await rejectVerification(rejectingId, { rejectionReason: rejectReason.trim() });
      setRequests(prev => prev.filter(r => r.id !== rejectingId));
      setRejectingId(null);
      setRejectReason('');
    } catch (e) {
      alert(getErrorMessage(e));
    } finally {
      setActionLoading(null);
    }
  };

  if (loading) return <SkeletonProfile />;
  if (error) return <div className={styles.errorBanner}>{error}</div>;

  return (
    <div>
      <p style={{ color: 'var(--color-text-secondary)', marginBottom: '1rem', fontSize: '0.9rem' }}>
        Заявки на верификацию от работодателей. Компании прошли автоматическую проверку ИНН и ожидают вашего решения!
      </p>

      {requests.length === 0 ? (
        <div className={styles.emptyState}>
          <span className="material-symbols-rounded" style={{ fontSize: '48px', opacity: 0.4 }}>task_alt</span>
          <p>Нет заявок на рассмотрение</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {requests.map(req => (
            <div key={req.id} className={styles.card} style={{ padding: '1.25rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.05rem', fontWeight: 600, color: 'var(--color-text)' }}>
                    {req.companyName || 'Без названия'}
                  </h3>
                  <span style={{
                    display: 'inline-block', marginTop: '0.3rem',
                    fontSize: '0.75rem', fontWeight: 600, padding: '0.15rem 0.5rem',
                    borderRadius: '999px', background: VERIF_STATUS_COLORS[req.status] + '18',
                    color: VERIF_STATUS_COLORS[req.status],
                  }}>
                    {VERIF_STATUS_LABELS[req.status]}
                  </span>
                </div>
                <span style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)' }}>
                  {formatDate(req.createdAt)}
                </span>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem', marginBottom: '1rem' }}>
                <div>
                  <div className={styles.detailLabel}>ИНН</div>
                  <div style={{ fontSize: '0.9rem', color: 'var(--color-text)', marginTop: '0.15rem' }}>{req.inn}</div>
                </div>
                <div>
                  <div className={styles.detailLabel}>Домен</div>
                  <div style={{ fontSize: '0.9rem', color: 'var(--color-text)', marginTop: '0.15rem' }}>{req.companyDomain}</div>
                </div>
                <div>
                  <div className={styles.detailLabel}>Корп. почта</div>
                  <div style={{ fontSize: '0.9rem', color: 'var(--color-text)', marginTop: '0.15rem' }}>{req.corporateEmail}</div>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <Button variant="primary" size="sm" isLoading={actionLoading === req.id} onClick={() => handleApprove(req.id)}>
                  Одобрить
                </Button>
                <Button variant="secondary" size="sm" onClick={() => { setRejectingId(req.id); setRejectReason(''); }}>
                  Отклонить
                </Button>
              </div>

              {rejectingId === req.id && (
                <div style={{
                  marginTop: '0.75rem', padding: '1rem',
                  background: 'var(--color-error-bg)', borderRadius: '10px',
                }}>
                  <div style={{ marginBottom: '0.5rem' }}>
                    <label style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--color-text)' }}>
                      Причина отклонения *
                    </label>
                    <textarea
                      className={styles.textarea}
                      rows={2}
                      value={rejectReason}
                      onChange={e => setRejectReason(e.target.value)}
                      placeholder="Укажите причину отклонения заявки..."
                      style={{ marginTop: '0.3rem' }}
                    />
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <Button
                      variant="primary" size="sm"
                      disabled={!rejectReason.trim()}
                      isLoading={actionLoading === req.id}
                      onClick={handleReject}
                      style={{ background: 'var(--color-error)' }}
                    >
                      Подтвердить отклонение
                    </Button>
                    <Button variant="ghost" size="sm" onClick={() => setRejectingId(null)}>Отмена</Button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}



function ModerationTab() {
  const [opportunities, setOpportunities] = useState<OpportunityResponse[]>([]);
  const [users, setUsers] = useState<UserManagementResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const [reasonFor, setReasonFor] = useState<string | null>(null);
  const [reason, setReason] = useState('');

  const [tempPassword, setTempPassword] = useState<{ userId: string; password: string } | null>(null);
  const [resetLoading, setResetLoading] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [oppData, usersData] = await Promise.all([
        getOpportunities({ page: 0, size: 50 }),
        getUsers(0, 50),
      ]);
      setOpportunities(oppData.content);
      setUsers(usersData.content);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  const handleHide = async (id: string) => {
    try {
      setActionLoading(id);
      await hideOpportunity(id, { reason: reason || 'Нарушение правил платформы' });
      setOpportunities(prev => prev.map(o =>
        o.id === id ? { ...o, status: 'ON_MODERATION' as any } : o
      ));
      setReasonFor(null);
      setReason('');
    } catch (e) {
      alert(getErrorMessage(e));
    } finally {
      setActionLoading(null);
    }
  };

  const handleUnhide = async (id: string) => {
    try {
      setActionLoading(id);
      await unhideOpportunity(id);
      setOpportunities(prev => prev.map(o =>
        o.id === id ? { ...o, status: 'ACTIVE' as any } : o
      ));
    } catch (e) {
      alert(getErrorMessage(e));
    } finally {
      setActionLoading(null);
    }
  };

  const handleBlock = async (id: string) => {
    if (!reason.trim()) { alert('Укажите причину блокировки'); return; }
    try {
      setActionLoading(id);
      await blockUser(id, { reason: reason.trim() });
      setUsers(prev => prev.map(u =>
        u.id === id ? { ...u, status: 'BLOCKED' as AccountStatus } : u
      ));
      setReasonFor(null);
      setReason('');
    } catch (e) {
      alert(getErrorMessage(e));
    } finally {
      setActionLoading(null);
    }
  };

  const handleUnblock = async (id: string) => {
    try {
      setActionLoading(id);
      await unblockUser(id);
      setUsers(prev => prev.map(u =>
        u.id === id ? { ...u, status: 'ACTIVE' as AccountStatus } : u
      ));
    } catch (e) {
      alert(getErrorMessage(e));
    } finally {
      setActionLoading(null);
    }
  };

  async function handleResetPassword(userId: string) {
    if (!confirm('Сбросить пароль этого пользователя? Будет сгенерирован временный пароль.')) return;
    setResetLoading(userId);
    try {
      const result = await resetUserPassword(userId);
      setTempPassword({ userId: result.userId, password: result.temporaryPassword });
    } catch (err) {
      alert(getErrorMessage(err));
    } finally {
      setResetLoading(null);
    }
  }

  if (loading) return <SkeletonProfile />;
  if (error) return <div className={styles.errorBanner}>{error}</div>;

  const activeOpps = opportunities.filter(o => o.status === 'ACTIVE' || o.status === 'ON_MODERATION');

  return (
    <div>
      {/* Карточки возможностей */}
      <section className={styles.card} style={{ marginBottom: '1.25rem' }}>
        <div className={styles.cardHeader}>
          <h2 className={styles.cardTitle}>
            <span className="material-symbols-rounded">work</span> Карточки возможностей
          </h2>
          <span style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)' }}>{activeOpps.length} шт.</span>
        </div>

        {activeOpps.length === 0 ? (
          <p className={styles.placeholder}>Нет карточек для модерации</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {activeOpps.map(opp => (
              <div key={opp.id} className={styles.oppCard} style={{ flexWrap: 'wrap' }}>
                <div className={styles.oppMain}>
                  <div className={styles.oppTop}>
                    <span className={styles.oppStatus} style={{
                      color: opp.status === 'ACTIVE' ? '#059669' : '#d97706'
                    }}>
                      {opp.status === 'ACTIVE' ? 'Активна' : 'Скрыта'}
                    </span>
                    <span className={styles.oppType}>{opp.companyName}</span>
                  </div>
                  <p className={styles.oppTitle}>{opp.title}</p>
                  <div className={styles.oppMeta}>
                    <span>{opp.city}</span>
                    {opp.tags && opp.tags.length > 0 && <span>{opp.tags.slice(0, 3).join(', ')}</span>}
                  </div>
                </div>

                <div className={styles.oppActions}>
                  {opp.status === 'ACTIVE' ? (
                    <button className={styles.oppBtnDelete} title="Скрыть"
                      disabled={actionLoading === opp.id}
                      onClick={() => { setReasonFor(`opp-${opp.id}`); setReason(''); }}>
                      <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>visibility_off</span>
                    </button>
                  ) : (
                    <button className={styles.oppBtnEdit} title="Восстановить"
                      disabled={actionLoading === opp.id}
                      onClick={() => handleUnhide(opp.id)}>
                      <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>visibility</span>
                    </button>
                  )}
                </div>

                {reasonFor === `opp-${opp.id}` && (
                  <div style={{ width: '100%', marginTop: '0.5rem', padding: '0.75rem', background: '#fef3cd', borderRadius: '8px' }}>
                    <input type="text" placeholder="Причина скрытия (необязательно)" value={reason}
                      onChange={e => setReason(e.target.value)}
                      style={{ width: '100%', padding: '0.4rem 0.6rem', marginBottom: '0.4rem',
                        border: '1px solid var(--color-border)', borderRadius: '6px', fontSize: '0.85rem' }} />
                    <div style={{ display: 'flex', gap: '0.4rem' }}>
                      <Button size="sm" onClick={() => handleHide(opp.id)} isLoading={actionLoading === opp.id}>Скрыть</Button>
                      <Button variant="ghost" size="sm" onClick={() => setReasonFor(null)}>Отмена</Button>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Пользователи */}
      <section className={styles.card}>
        <div className={styles.cardHeader}>
          <h2 className={styles.cardTitle}>
            <span className="material-symbols-rounded">person</span> Управление пользователями
          </h2>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
          {users.filter(u => u.role !== 'ADMIN').map(usr => (
            <div key={usr.id} className={styles.appRow} style={{ flexWrap: 'wrap' }}>
              <div className={styles.appInfo}>
                <span className={styles.appTitle} style={{ cursor: 'default' }}>{usr.displayName || usr.email}</span>
                <span className={styles.appCompany}>{usr.email} · {ROLE_LABELS[usr.role]}</span>
              </div>
              <div className={styles.appMeta}>
                <span className={styles.appStatus} style={{
                  color: STATUS_COLORS[usr.status],
                  borderColor: STATUS_COLORS[usr.status] + '40',
                  background: STATUS_COLORS[usr.status] + '10',
                }}>{STATUS_LABELS[usr.status]}</span>
                {usr.status !== 'BLOCKED' ? (
                  <button className={styles.oppBtnDelete} title="Заблокировать"
                    onClick={() => { setReasonFor(`user-${usr.id}`); setReason(''); }}>
                    <span className="material-symbols-rounded" style={{ fontSize: '16px' }}>block</span>
                  </button>
                ) : (
                  <button className={styles.oppBtnEdit} title="Разблокировать"
                    onClick={() => handleUnblock(usr.id)} disabled={actionLoading === usr.id}>
                    <span className="material-symbols-rounded" style={{ fontSize: '16px' }}>lock_open</span>
                  </button>
                )}
                <button className={styles.oppBtnEdit} title="Сбросить пароль"
                  onClick={() => handleResetPassword(usr.id)}
                  disabled={resetLoading === usr.id}>
                  <span className="material-symbols-rounded" style={{ fontSize: '16px' }}>
                    {resetLoading === usr.id ? 'hourglass_top' : 'lock_reset'}
                  </span>
                </button>
              </div>

              {reasonFor === `user-${usr.id}` && (
                <div style={{ width: '100%', marginTop: '0.5rem', padding: '0.75rem', background: 'var(--color-error-bg)', borderRadius: '8px' }}>
                  <input type="text" placeholder="Причина блокировки *" value={reason}
                    onChange={e => setReason(e.target.value)}
                    style={{ width: '100%', padding: '0.4rem 0.6rem', marginBottom: '0.4rem',
                      border: '1px solid var(--color-border)', borderRadius: '6px', fontSize: '0.85rem' }} />
                  <div style={{ display: 'flex', gap: '0.4rem' }}>
                    <Button size="sm" onClick={() => handleBlock(usr.id)} isLoading={actionLoading === usr.id}
                      style={{ background: 'var(--color-error)' }}>Заблокировать</Button>
                    <Button variant="ghost" size="sm" onClick={() => setReasonFor(null)}>Отмена</Button>
                  </div>
                </div>
              )}
              {tempPassword && tempPassword.userId === usr.id && (
                <div style={{
                  width: '100%', marginTop: '0.5rem', padding: '0.75rem',
                  background: 'rgba(5, 150, 105, 0.1)', borderRadius: '8px',
                  border: '1px solid rgba(5, 150, 105, 0.3)',
                }}>
                  <p style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--color-success)', margin: '0 0 0.3rem' }}>
                    Временный пароль сгенерирован:
                  </p>
                  <code style={{
                    display: 'block', padding: '0.5rem 0.75rem',
                    background: 'var(--color-bg)', borderRadius: '6px',
                    fontSize: '1.1rem', fontWeight: 700, letterSpacing: '0.05em',
                    color: 'var(--color-text)', userSelect: 'all',
                  }}>
                    {tempPassword.password}
                  </code>
                  <p style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', margin: '0.3rem 0 0' }}>
                    Передайте пароль пользователю. Он сможет сменить его в личном кабинете.
                  </p>
                  <Button variant="ghost" size="sm" style={{ marginTop: '0.5rem' }}
                    onClick={() => setTempPassword(null)}>
                    Закрыть
                  </Button>
                </div>
              )}
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}


// Теги
function TagsTab() {
  const [pendingTags, setPendingTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const [showForm, setShowForm] = useState(false);
  const [newTagName, setNewTagName] = useState('');
  const [newTagCategory, setNewTagCategory] = useState<TagCategory>(TagCategory.LANGUAGE);
  const [formLoading, setFormLoading] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getPendingTags(0, 50);
      setPendingTags(data.content);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  const handleApprove = async (id: string) => {
    try {
      setActionLoading(id);
      await approveTag(id);
      setPendingTags(prev => prev.filter(t => t.id !== id));
    } catch (e) {
      alert(getErrorMessage(e));
    } finally {
      setActionLoading(null);
    }
  };

  const handleReject = async (id: string) => {
    if (!confirm('Удалить этот тег?')) return;
    try {
      setActionLoading(id);
      await rejectTag(id);
      setPendingTags(prev => prev.filter(t => t.id !== id));
    } catch (e) {
      alert(getErrorMessage(e));
    } finally {
      setActionLoading(null);
    }
  };

  const handleCreateTag = async (e: FormEvent) => {
    e.preventDefault();
    if (!newTagName.trim()) return;
    try {
      setFormLoading(true);
      setFormError(null);
      await createTagByCurator({ name: newTagName.trim(), category: newTagCategory });
      setNewTagName('');
      setShowForm(false);
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setFormLoading(false);
    }
  };

  if (loading) return <SkeletonProfile />;
  if (error) return <div className={styles.errorBanner}>{error}</div>;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem', margin: 0 }}>
          Предложенные работодателями теги, ожидающие одобрения.
        </p>
        <Button variant="secondary" size="sm" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Скрыть форму' : '+ Создать тег'}
        </Button>
      </div>

      {showForm && (
        <form onSubmit={handleCreateTag} className={styles.card} style={{ marginBottom: '1rem', padding: '1.25rem' }}>
          <h3 style={{ fontSize: '1rem', fontWeight: 600, margin: '0 0 0.75rem 0' }}>Новый тег (сразу одобрен)</h3>
          {formError && <div className={styles.errorBanner}>{formError}</div>}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr auto', gap: '0.75rem', alignItems: 'end' }}>
            <Input label="Название" value={newTagName} onChange={e => setNewTagName(e.target.value)} placeholder="React, Python, Docker..." />
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
              <label style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--color-text)' }}>Категория</label>
              <select value={newTagCategory} onChange={e => setNewTagCategory(e.target.value as TagCategory)}
                className={styles.statusSelect} style={{ padding: '0.6rem 0.75rem', borderRadius: '10px' }}>
                {Object.entries(TAG_CATEGORY_LABELS).map(([key, label]) => (
                  <option key={key} value={key}>{label}</option>
                ))}
              </select>
            </div>
            <Button variant="primary" size="md" type="submit" isLoading={formLoading}>Создать</Button>
          </div>
        </form>
      )}

      {pendingTags.length === 0 ? (
        <div className={styles.emptyState}>
          <span className="material-symbols-rounded" style={{ fontSize: '48px', opacity: 0.4 }}>label_off</span>
          <p>Нет тегов, ожидающих одобрения</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
          {pendingTags.map(tag => (
            <div key={tag.id} className={styles.appRow}>
              <div className={styles.appInfo}>
                <span className={styles.appTitle} style={{ cursor: 'default' }}>{tag.name}</span>
                <span className={styles.appCompany}>
                  {TAG_CATEGORY_LABELS[tag.category] || tag.category}
                  {tag.parentName && ` → родитель: ${tag.parentName}`}
                </span>
              </div>
              <div style={{ display: 'flex', gap: '0.4rem' }}>
                <button className={styles.oppBtnEdit} title="Одобрить" disabled={actionLoading === tag.id}
                  onClick={() => handleApprove(tag.id)}>
                  <span className="material-symbols-rounded" style={{ fontSize: '18px', color: '#059669' }}>check</span>
                </button>
                <button className={styles.oppBtnDelete} title="Отклонить" disabled={actionLoading === tag.id}
                  onClick={() => handleReject(tag.id)}>
                  <span className="material-symbols-rounded" style={{ fontSize: '18px' }}>close</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}



// Пользователи
function UsersTab() {
  const [users, setUsers] = useState<UserManagementResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);

  const [filterRole, setFilterRole] = useState<UserRole | ''>('');
  const [filterStatus, setFilterStatus] = useState<AccountStatus | ''>('');

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getUsers(page, 20, filterRole || undefined, filterStatus || undefined);
      setUsers(data.content);
      setTotalElements(data.totalElements);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [page, filterRole, filterStatus]);

  useEffect(() => { loadData(); }, [loadData]);
  useEffect(() => { setPage(0); }, [filterRole, filterStatus]);

  if (error) return <div className={styles.errorBanner}>{error}</div>;

  return (
    <div>
      <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
        <select value={filterRole} onChange={e => setFilterRole(e.target.value as UserRole | '')} className={styles.filterSelect}>
          <option value="">Все роли</option>
          <option value="APPLICANT">Соискатели</option>
          <option value="EMPLOYER">Работодатели</option>
          <option value="CURATOR">Кураторы</option>
          <option value="ADMIN">Администраторы</option>
        </select>

        <select value={filterStatus} onChange={e => setFilterStatus(e.target.value as AccountStatus | '')} className={styles.filterSelect}>
          <option value="">Все статусы</option>
          <option value="ACTIVE">Активные</option>
          <option value="PENDING_VERIFICATION">Ожидают верификации</option>
          <option value="BLOCKED">Заблокированные</option>
        </select>

        <span style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', marginLeft: 'auto' }}>
          Всего: {totalElements}
        </span>
      </div>

      {loading ? (
        <SkeletonProfile />
      ) : users.length === 0 ? (
        <div className={styles.emptyState}>
          <span className="material-symbols-rounded" style={{ fontSize: '48px', opacity: 0.4 }}>person_off</span>
          <p>Пользователи не найдены</p>
        </div>
      ) : (
        <>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {users.map(usr => (
              <div key={usr.id} className={styles.appRow}>
                <div className={styles.appInfo}>
                  <span className={styles.appTitle} style={{ cursor: 'default' }}>{usr.displayName || '(без имени)'}</span>
                  <span className={styles.appCompany}>{usr.email} · {ROLE_LABELS[usr.role]} · Рег: {formatDate(usr.createdAt)}</span>
                </div>
                <span className={styles.appStatus} style={{
                  color: STATUS_COLORS[usr.status],
                  borderColor: STATUS_COLORS[usr.status] + '40',
                  background: STATUS_COLORS[usr.status] + '10',
                }}>{STATUS_LABELS[usr.status]}</span>
              </div>
            ))}
          </div>

          <div style={{ display: 'flex', justifyContent: 'center', gap: '0.5rem', marginTop: '1rem' }}>
            <Button variant="ghost" size="sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Назад</Button>
            <span style={{ display: 'flex', alignItems: 'center', fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
              Стр. {page + 1}
            </span>
            <Button variant="ghost" size="sm" disabled={users.length < 20} onClick={() => setPage(p => p + 1)}>Вперёд →</Button>
          </div>
        </>
      )}
    </div>
  );
}



// Журнал действий 
function LogsTab() {
  const [logs, setLogs] = useState<ModerationLogResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getModerationLogs(page, 30);
      setLogs(data.content);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => { loadData(); }, [loadData]);

  if (loading) return <SkeletonProfile />;
  if (error) return <div className={styles.errorBanner}>{error}</div>;

  return (
    <div>
      <p style={{ color: 'var(--color-text-secondary)', marginBottom: '1rem', fontSize: '0.9rem' }}>
        История всех действий модерации на платформе.
      </p>

      {logs.length === 0 ? (
        <div className={styles.emptyState}>
          <span className="material-symbols-rounded" style={{ fontSize: '48px', opacity: 0.4 }}>receipt_long</span>
          <p>Журнал пуст</p>
        </div>
      ) : (
        <>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {logs.map(log => (
              <div key={log.id} className={styles.appRow}>
                <div className={styles.appInfo}>
                  <span className={styles.appTitle} style={{ cursor: 'default' }}>
                    <span style={{
                      display: 'inline-block', padding: '0.1rem 0.45rem',
                      borderRadius: '4px', fontSize: '0.75rem', fontWeight: 700,
                      background: log.action.includes('BLOCK') || log.action === 'HIDE' || log.action === 'DELETE'
                        ? '#dc262618' : '#05966918',
                      color: log.action.includes('BLOCK') || log.action === 'HIDE' || log.action === 'DELETE'
                        ? '#dc2626' : '#059669',
                      marginRight: '0.4rem',
                    }}>
                      {ACTION_LABELS[log.action] || log.action}
                    </span>
                    {TARGET_LABELS[log.targetType] || log.targetType}
                  </span>
                  <span className={styles.appCompany}>
                    Куратор: {log.curatorName || 'Система'}
                    {log.reason && ` · Причина: ${log.reason}`}
                  </span>
                </div>
                <span style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', whiteSpace: 'nowrap' }}>
                  {formatDateTime(log.createdAt)}
                </span>
              </div>
            ))}
          </div>

          <div style={{ display: 'flex', justifyContent: 'center', gap: '0.5rem', marginTop: '1rem' }}>
            <Button variant="ghost" size="sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Назад</Button>
            <span style={{ display: 'flex', alignItems: 'center', fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
              Стр. {page + 1}
            </span>
            <Button variant="ghost" size="sm" disabled={logs.length < 30} onClick={() => setPage(p => p + 1)}>Вперёд →</Button>
          </div>
        </>
      )}
    </div>
  );
}


// Создание кураторов только админом
function CuratorsTab() {
  const [curators, setCurators] = useState<UserManagementResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [email, setEmail] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [password, setPassword] = useState('');
  const [formLoading, setFormLoading] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [formSuccess, setFormSuccess] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getUsers(0, 50, 'CURATOR' as UserRole);
      setCurators(data.content);
    } catch (e) {
      setError(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setFormSuccess(null);

    if (!email.trim() || !displayName.trim() || !password.trim()) {
      setFormError('Заполните все поля');
      return;
    }
    if (password.length < 8) {
      setFormError('Пароль должен быть не менее 8 символов');
      return;
    }

    try {
      setFormLoading(true);
      const newCurator = await createCurator({
        email: email.trim(),
        displayName: displayName.trim(),
        password: password.trim(),
      });
      setCurators(prev => [newCurator, ...prev]);
      setEmail('');
      setDisplayName('');
      setPassword('');
      setFormSuccess(`Куратор «${newCurator.displayName}» успешно создан!`);
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setFormLoading(false);
    }
  };

  if (loading) return <SkeletonProfile />;
  if (error) return <div className={styles.errorBanner}>{error}</div>;

  return (
    <div>
      <section className={styles.card} style={{ marginBottom: '1.25rem', padding: '1.5rem' }}>
        <h2 className={styles.cardTitle} style={{ marginBottom: '1rem' }}>
          <span className="material-symbols-rounded">person_add</span> Создать куратора
        </h2>

        {formError && <div className={styles.errorBanner}>{formError}</div>}
        {formSuccess && <div className={styles.successBanner}>{formSuccess}</div>}

        <form onSubmit={handleCreate} className={styles.editForm}>
          <div className={styles.formRow}>
            <Input label="Email" type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="curator@tramplin.ru" />
            <Input label="Отображаемое имя" value={displayName} onChange={e => setDisplayName(e.target.value)} placeholder="Иванов Иван" />
            <Input label="Пароль" type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="Мин. 8 символов" />
          </div>
          <div className={styles.formActions}>
            <Button variant="primary" size="md" type="submit" isLoading={formLoading}>Создать куратора</Button>
          </div>
        </form>
      </section>

      <section className={styles.card}>
        <div className={styles.cardHeader}>
          <h2 className={styles.cardTitle}>
            <span className="material-symbols-rounded">group</span> Кураторы платформы
          </h2>
          <span style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)' }}>{curators.length} чел.</span>
        </div>

        {curators.length === 0 ? (
          <p className={styles.placeholder}>Кураторов пока нет. Создайте первого выше.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {curators.map(c => (
              <div key={c.id} className={styles.appRow}>
                <div className={styles.appInfo}>
                  <span className={styles.appTitle} style={{ cursor: 'default' }}>{c.displayName}</span>
                  <span className={styles.appCompany}>{c.email} · Создан: {formatDate(c.createdAt)}</span>
                </div>
                <span className={styles.appStatus} style={{
                  color: STATUS_COLORS[c.status],
                  borderColor: STATUS_COLORS[c.status] + '40',
                  background: STATUS_COLORS[c.status] + '10',
                }}>{STATUS_LABELS[c.status]}</span>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
