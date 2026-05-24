import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Modal } from '../ui/Modal';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';
import { useToast } from '../ui/Toast';
import { createEducation, updateEducation } from '../../api/resume';
import type { ResumeEducation } from '../../types';

const yearRegex = /^\d{4}$/;
const yearInRange = (v: string) => { const n = Number(v); return n >= 1950 && n <= 2100; };

const DEGREES = [
  ['BACHELOR', 'Бакалавриат'], ['MASTER', 'Магистратура'], ['SPECIALIST', 'Специалитет'],
  ['PHD', 'Аспирантура'], ['COLLEGE', 'Колледж'], ['OTHER', 'Другое'],
] as const;

const schema = z.object({
  institution: z.string().trim().min(1, 'Укажите учебное заведение').max(255, 'До 255 символов'),
  faculty:     z.string().max(255, 'До 255 символов'),
  degree:      z.enum(['BACHELOR', 'MASTER', 'SPECIALIST', 'PHD', 'COLLEGE', 'OTHER']),
  startYear:   z.string().regex(yearRegex, 'Год из 4 цифр').refine(yearInRange, '1950–2100'),
  endYear:     z.string().regex(yearRegex, 'Год из 4 цифр').refine(yearInRange, '1950–2100').or(z.literal('')),
  description: z.string().max(2000, 'До 2000 символов'),
}).refine((d) => !d.endYear || Number(d.endYear) >= Number(d.startYear), {
  message: 'Год окончания не раньше начала', path: ['endYear'],
});

type FormValues = z.infer<typeof schema>;

interface Props { isOpen: boolean; onClose: () => void; onSaved: () => void; record: ResumeEducation | null; }

export function EducationModal({ isOpen, onClose, onSaved, record }: Props) {
  const { showToast } = useToast();
  const { register, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  useEffect(() => {
    if (isOpen) reset({
      institution: record?.institution ?? '', faculty: record?.faculty ?? '',
      degree: record?.degree ?? 'BACHELOR',
      startYear: record ? String(record.startYear) : '',
      endYear: record?.endYear != null ? String(record.endYear) : '',
      description: record?.description ?? '',
    });
  }, [isOpen, record, reset]);

  const onSubmit = async (v: FormValues) => {
    const payload = {
      institution: v.institution.trim(),
      faculty: v.faculty || null,
      degree: v.degree,
      startYear: Number(v.startYear),
      endYear: v.endYear ? Number(v.endYear) : null,
      description: v.description || null,
    };
    try {
      if (record) { await updateEducation(record.id, payload); showToast('Образование обновлено', 'success'); }
      else { await createEducation(payload); showToast('Образование добавлено', 'success'); }
      onSaved(); onClose();
    } catch (e: any) {
      showToast(e?.response?.data?.error?.message || 'Не удалось сохранить', 'error');
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={record ? 'Редактировать образование' : 'Добавить образование'}>
      <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        <Input label="Учебное заведение" {...register('institution')} error={errors.institution?.message} />
        <Input label="Факультет (необязательно)" {...register('faculty')} error={errors.faculty?.message} />
        <div>
          <label style={{ display: 'block', marginBottom: 4, fontSize: '0.85rem' }}>Уровень</label>
          <select {...register('degree')}
            style={{ width: '100%', padding: '0.5rem', borderRadius: 8, border: '1px solid var(--color-border)', background: 'transparent', color: 'inherit', font: 'inherit' }}>
            {DEGREES.map(([val, label]) => <option key={val} value={val}>{label}</option>)}
          </select>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
            <div style={{ flex: 1, minWidth: 0 }}>
                <Input label="Год начала" type="number" placeholder="2022" {...register('startYear')} error={errors.startYear?.message} />
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
                <Input label="Год окончания" type="number" placeholder="2026" {...register('endYear')} error={errors.endYear?.message} />
            </div>
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: 4, fontSize: '0.85rem' }}>Описание</label>
          <textarea {...register('description')} rows={3}
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