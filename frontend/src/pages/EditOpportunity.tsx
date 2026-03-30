import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getOpportunityById, updateOpportunity } from '../api/opportunities';
import type { OpportunityRequest } from '../types';
import { OpportunityType, WorkFormat } from '../types';
import styles from './CreateOpportunity.module.css';
import { getTags } from '../api/tags';
import type { Tag } from '../types';

export default function EditOpportunity() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [form, setForm] = useState<OpportunityRequest>({
    title: '', description: '', type: OpportunityType.INTERNSHIP,
    workFormat: WorkFormat.OFFICE, city: '', address: null,
    salaryMin: null, salaryMax: null, expiresAt: null, eventDate: null,
    contactEmail: null, contactPhone: null, contactUrl: null,
  });

  const [loading, setLoading] = useState(false);
  const [pageLoading, setPageLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [allTags, setAllTags] = useState<Tag[]>([]);
  const [selectedTagIds, setSelectedTagIds] = useState<string[]>([]);

  useEffect(() => {
    async function load() {
      try {
        const [opp, tags] = await Promise.all([
          getOpportunityById(id!),
          getTags(),
        ]);
        setForm({
          title: opp.title, description: opp.description, type: opp.type,
          workFormat: opp.workFormat, city: opp.city, address: opp.address,
          salaryMin: opp.salaryMin, salaryMax: opp.salaryMax,
          expiresAt: opp.expiresAt?.split('T')[0] || null,
          eventDate: opp.eventDate?.split('T')[0] || null,
          contactEmail: opp.contactEmail, contactPhone: opp.contactPhone,
          contactUrl: opp.contactUrl,
        });
        setAllTags(tags);
        // Match tags by name to get IDs
        const tagNameSet = new Set(opp.tags || []);
        setSelectedTagIds(tags.filter(t => tagNameSet.has(t.name)).map(t => t.id));
      } catch {
        setError('Не удалось загрузить вакансию');
      } finally {
        setPageLoading(false);
      }
    }
    if (id) load();
  }, [id]);

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value || null }));
  }

  function handleNumberChange(e: React.ChangeEvent<HTMLInputElement>) {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value ? Number(value) : null }));
  }

  function handleTagToggle(tagId: string) {
    setSelectedTagIds(prev =>
      prev.includes(tagId) ? prev.filter(i => i !== tagId) : [...prev, tagId]
    );
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!form.title.trim()) { setError('Укажите название'); return; }
    if (!form.description.trim()) { setError('Заполните описание'); return; }
    if (!form.city.trim()) { setError('Укажите город'); return; }

    setLoading(true);
    try {
      const payload = {
        ...form,
        expiresAt: form.expiresAt ? form.expiresAt + 'T00:00:00' : null,
        eventDate: form.eventDate ? form.eventDate + 'T00:00:00' : null,
        tagIds: selectedTagIds.length > 0 ? selectedTagIds : undefined,
      };
      await updateOpportunity(id!, payload);
      navigate('/company');
    } catch (err: any) {
      setError(err?.response?.data?.error?.message || 'Не удалось сохранить');
    } finally {
      setLoading(false);
    }
  }

  if (pageLoading) return <div className={styles.container}>Загрузка...</div>;

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <button onClick={() => navigate('/company')} className={styles.backBtn}>← Назад</button>
        <h1>Редактирование</h1>
      </div>

      {error && <div className={styles.errorBanner}>{error}</div>}

      <form onSubmit={handleSubmit} className={styles.form}>
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Основная информация</h2>
          <label className={styles.field}>
            <span className={styles.label}>Название *</span>
            <input type="text" name="title" value={form.title} onChange={handleChange}
              className={styles.input} maxLength={200} />
          </label>
          <label className={styles.field}>
            <span className={styles.label}>Описание *</span>
            <textarea name="description" value={form.description} onChange={handleChange}
              className={styles.textarea} rows={6} />
          </label>
          <div className={styles.row}>
            <label className={styles.field}>
              <span className={styles.label}>Тип</span>
              <select name="type" value={form.type} onChange={handleChange} className={styles.select}>
                <option value={OpportunityType.INTERNSHIP}>Стажировка</option>
                <option value={OpportunityType.VACANCY}>Вакансия</option>
                <option value={OpportunityType.MENTORSHIP}>Менторская программа</option>
                <option value={OpportunityType.EVENT}>Мероприятие</option>
              </select>
            </label>
            <label className={styles.field}>
              <span className={styles.label}>Формат работы</span>
              <select name="workFormat" value={form.workFormat} onChange={handleChange} className={styles.select}>
                <option value={WorkFormat.OFFICE}>Офис</option>
                <option value={WorkFormat.HYBRID}>Гибрид</option>
                <option value={WorkFormat.REMOTE}>Удалённо</option>
              </select>
            </label>
          </div>
        </section>

        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Локация</h2>
          <div className={styles.row}>
            <label className={styles.field}>
              <span className={styles.label}>Город *</span>
              <input type="text" name="city" value={form.city} onChange={handleChange} className={styles.input} />
            </label>
            {form.workFormat !== WorkFormat.REMOTE && (
              <label className={styles.field}>
                <span className={styles.label}>Адрес офиса</span>
                <input type="text" name="address" value={form.address || ''} onChange={handleChange} className={styles.input} />
              </label>
            )}
          </div>
        </section>

        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Зарплата</h2>
          <div className={styles.row}>
            <label className={styles.field}>
              <span className={styles.label}>От</span>
              <input type="number" name="salaryMin" value={form.salaryMin ?? ''} onChange={handleNumberChange} className={styles.input} min={0} />
            </label>
            <label className={styles.field}>
              <span className={styles.label}>До</span>
              <input type="number" name="salaryMax" value={form.salaryMax ?? ''} onChange={handleNumberChange} className={styles.input} min={0} />
            </label>
          </div>
        </section>

        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Даты</h2>
          <div className={styles.row}>
            <label className={styles.field}>
              <span className={styles.label}>Срок действия</span>
              <input type="date" name="expiresAt" value={form.expiresAt || ''} onChange={handleChange} className={styles.input} />
            </label>
            {form.type === OpportunityType.EVENT && (
              <label className={styles.field}>
                <span className={styles.label}>Дата проведения</span>
                <input type="date" name="eventDate" value={form.eventDate || ''} onChange={handleChange} className={styles.input} />
              </label>
            )}
          </div>
        </section>

        {allTags.length > 0 && (
          <section className={styles.section}>
            <h2 className={styles.sectionTitle}>Теги</h2>
            <div className={styles.tagsGrid}>
              {allTags.map(tag => (
                <button key={tag.id} type="button"
                  className={`${styles.tagChip} ${selectedTagIds.includes(tag.id) ? styles.tagChipActive : ''}`}
                  onClick={() => handleTagToggle(tag.id)}>
                  {tag.name}
                </button>
              ))}
            </div>
          </section>
        )}

        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Контакты</h2>
          <div className={styles.row}>
            <label className={styles.field}>
              <span className={styles.label}>Email</span>
              <input type="email" name="contactEmail" value={form.contactEmail || ''} onChange={handleChange} className={styles.input} />
            </label>
            <label className={styles.field}>
              <span className={styles.label}>Телефон</span>
              <input type="tel" name="contactPhone" value={form.contactPhone || ''} onChange={handleChange} className={styles.input} />
            </label>
            <label className={styles.field}>
              <span className={styles.label}>Ссылка</span>
              <input type="url" name="contactUrl" value={form.contactUrl || ''} onChange={handleChange} className={styles.input} />
            </label>
          </div>
        </section>

        <div className={styles.actions}>
          <button type="button" onClick={() => navigate('/company')} className={styles.btnSecondary}>Отмена</button>
          <button type="submit" disabled={loading} className={styles.btnPrimary}>
            {loading ? 'Сохранение...' : 'Сохранить'}
          </button>
        </div>
      </form>
    </div>
  );
}
