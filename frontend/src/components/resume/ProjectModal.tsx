import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Modal } from '../ui/Modal';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';
import { useToast } from '../ui/Toast';
import { createProject, updateProject } from '../../api/resume';
import type { ResumeProject } from '../../types';

const monthRegex = /^\d{4}-(0[1-9]|1[0-2])$/;
const urlOk = (v: string) => v === '' || /^https?:\/\//.test(v);

const schema = z.object({
  title:         z.string().trim().min(1, 'Укажите название').max(255, 'До 255 символов'),
  role:          z.string().max(255, 'До 255 символов'),
  startDate:     z.string().regex(monthRegex, 'Укажите месяц начала'),
  endDate:       z.string().regex(monthRegex, 'Неверный формат').or(z.literal('')),
  description:   z.string().max(2000, 'До 2000 символов'),
  projectUrl:    z.string().max(500).refine(urlOk, 'Ссылка должна начинаться с http:// или https://'),
  repositoryUrl: z.string().max(500).refine(urlOk, 'Ссылка должна начинаться с http:// или https://'),
}).refine((d) => !d.endDate || d.endDate >= d.startDate, {
  message: 'Окончание не может быть раньше начала', path: ['endDate'],
});

type FormValues = z.infer<typeof schema>;

interface Props { isOpen: boolean; onClose: () => void; onSaved: () => void; record: ResumeProject | null; }

export function ProjectModal({ isOpen, onClose, onSaved, record }: Props) {
  const { showToast } = useToast();
  const { register, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  useEffect(() => {
    if (isOpen) reset({
      title: record?.title ?? '', role: record?.role ?? '',
      startDate: record?.startDate ?? '', endDate: record?.endDate ?? '',
      description: record?.description ?? '', projectUrl: record?.projectUrl ?? '',
      repositoryUrl: record?.repositoryUrl ?? '',
    });
  }, [isOpen, record, reset]);

  const onSubmit = async (v: FormValues) => {
    const payload = {
      title: v.title.trim(),
      role: v.role || null,
      startDate: v.startDate,
      endDate: v.endDate || null,
      description: v.description || null,
      projectUrl: v.projectUrl || null,
      repositoryUrl: v.repositoryUrl || null,
    };
    try {
      if (record) { await updateProject(record.id, payload); showToast('Проект обновлён', 'success'); }
      else { await createProject(payload); showToast('Проект добавлен', 'success'); }
      onSaved(); onClose();
    } catch (e: any) {
      showToast(e?.response?.data?.error?.message || 'Не удалось сохранить', 'error');
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={record ? 'Редактировать проект' : 'Добавить проект'}>
      <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        <Input label="Название" {...register('title')} error={errors.title?.message} />
        <Input label="Роль (необязательно)" {...register('role')} error={errors.role?.message} />
        <div style={{ display: 'flex', gap: '0.75rem' }}>
            <div style={{ flex: 1, minWidth: 0 }}>
                <Input label="Начало" type="month" {...register('startDate')} error={errors.startDate?.message} />
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
                <Input label="Окончание" type="month" {...register('endDate')} error={errors.endDate?.message} />
            </div>
        </div>
        <Input label="Ссылка на демо (необязательно)" placeholder="https://..." {...register('projectUrl')} error={errors.projectUrl?.message} />
        <Input label="Репозиторий (необязательно)" placeholder="https://github.com/..." {...register('repositoryUrl')} error={errors.repositoryUrl?.message} />
        <div>
          <label style={{ display: 'block', marginBottom: 4, fontSize: '0.85rem' }}>Описание</label>
          <textarea {...register('description')} rows={4}
            style={{ width: '100%', padding: '0.5rem', borderRadius: 8, border: '1px solid var(--color-border)', background: 'transparent', color: 'inherit', font: 'inherit', resize: 'vertical' }} />
          {errors.description && <span style={{ color: '#dc2626', fontSize: '0.8rem' }}>{errors.description.message}</span>}
        </div>
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '0.5rem' }}>
          <Button type="button" variant="secondary" size="sm" onClick={onClose}>Отмена</Button>
          <Button type="submit" variant="primary" size="sm" isLoading={isSubmitting}>Сохранить</Button>
        </div>
      </form>
    </Modal>
  );
}