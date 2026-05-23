import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Modal } from '../ui/Modal';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';
import { useToast } from '../ui/Toast';
import { createExperience, updateExperience } from '../../api/resume';
import type { ResumeExperience } from '../../types';

const monthRegex = /^\d{4}-(0[1-9]|1[0-2])$/;

const schema = z.object({
  organization: z.string().trim().min(1, 'Укажите организацию').max(255, 'До 255 символов'),
  position:     z.string().trim().min(1, 'Укажите должность').max(255, 'До 255 символов'),
  startDate:    z.string().regex(monthRegex, 'Укажите месяц начала'),
  endDate:      z.string().regex(monthRegex, 'Неверный формат').or(z.literal('')),
  description:  z.string().max(2000, 'До 2000 символов'),
}).refine(
  (d) => !d.endDate || d.endDate >= d.startDate,
  { message: 'Окончание не может быть раньше начала', path: ['endDate'] },
);

type FormValues = z.infer<typeof schema>;

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSaved: () => void;
  record: ResumeExperience | null;
}

export function ExperienceModal({ isOpen, onClose, onSaved, record }: Props) {
  const { showToast } = useToast();
  const { register, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm<FormValues>({
    resolver: zodResolver(schema),
  });

  // сброс формы при каждом открытии (создание - пусто, редактирование - данные записи)
  useEffect(() => {
    if (isOpen) {
      reset({
        organization: record?.organization ?? '',
        position:     record?.position ?? '',
        startDate:    record?.startDate ?? '',
        endDate:      record?.endDate ?? '',
        description:  record?.description ?? '',
      });
    }
  }, [isOpen, record, reset]);

  const onSubmit = async (values: FormValues) => {
    const payload = {
      organization: values.organization.trim(),
      position: values.position.trim(),
      startDate: values.startDate,
      endDate: values.endDate || null,
      description: values.description || null,
    };
    try {
      if (record) {
        await updateExperience(record.id, payload);
        showToast('Опыт обновлён', 'success');
      } else {
        await createExperience(payload);
        showToast('Опыт добавлен', 'success');
      }
      onSaved();   // перезагрузить список в ResumePage
      onClose();
    } catch (e: any) {
      showToast(e?.response?.data?.error?.message || 'Не удалось сохранить', 'error');
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={record ? 'Редактировать опыт' : 'Добавить опыт'}>
      <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        <Input label="Организация" {...register('organization')} error={errors.organization?.message} />
        <Input label="Должность" {...register('position')} error={errors.position?.message} />
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Input label="Начало" type="month" {...register('startDate')} error={errors.startDate?.message} />
          <Input label="Окончание" type="month" {...register('endDate')} error={errors.endDate?.message} />
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: 4, fontSize: '0.85rem' }}>Описание</label>
          <textarea {...register('description')} rows={4}
            placeholder="Чем занимались, технологии, достижения"
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