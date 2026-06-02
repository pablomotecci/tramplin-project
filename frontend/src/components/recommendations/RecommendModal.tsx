import { useState, useEffect, useCallback } from 'react';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { useToast } from '../ui/Toast';
import { createRecommendation } from '../../api/recommendations';
import { getOpportunities } from '../../api/opportunities';
import { getErrorMessage } from '../../api/client';
import type { OpportunityResponse } from '../../types';
import styles from './RecommendModal.module.css';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  recommendedUserId: string;
  recommendedName: string;
  onSuccess?: () => void;
}

export function RecommendModal({
  isOpen,
  onClose,
  recommendedUserId,
  recommendedName,
  onSuccess,
}: Props) {
  const { showToast } = useToast();
  const [opportunities, setOpportunities] = useState<OpportunityResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [selectedOppId, setSelectedOppId] = useState<string>('');
  const [message, setMessage] = useState('');

  // Загружаем активные вакансии при открытии модалки
  useEffect(() => {
    if (!isOpen) return;

    let mounted = true;
    setLoading(true);

    getOpportunities({ page: 0, size: 100 })
      .then((data) => {
        if (!mounted) return;
        const active = data.content.filter((opp) => opp.status === 'ACTIVE');
        setOpportunities(active);
      })
      .catch(() => {
        if (!mounted) return;
        showToast('Не удалось загрузить список вакансий', 'error');
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });

    return () => {
      mounted = false;
    };
  }, [isOpen, showToast]);

  // Сбрасываем форму при закрытии
  useEffect(() => {
    if (!isOpen) {
      setSelectedOppId('');
      setMessage('');
    }
  }, [isOpen]);

  const handleSubmit = useCallback(async () => {
    if (!selectedOppId) {
      showToast('Выберите вакансию', 'error');
      return;
    }

    setSubmitting(true);
    try {
      await createRecommendation({
        recommendedId: recommendedUserId,
        opportunityId: selectedOppId,
        message: message.trim() || undefined,
      });
      showToast('Рекомендация отправлена ✓', 'success');
      onSuccess?.();
      onClose();
    } catch (err) {
      showToast(getErrorMessage(err), 'error');
    } finally {
      setSubmitting(false);
    }
  }, [selectedOppId, message, recommendedUserId, showToast, onClose, onSuccess]);

  const firstName = recommendedName.split(' ')[0] || 'специалист';

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Рекомендовать ${recommendedName}`}
    >
      <p className={styles.subtitle}>
        Выберите вакансию, на которую хотите порекомендовать этого специалиста.
        Когда {firstName} откликнется на неё, работодатель увидит вашу рекомендацию
        в его карточке отклика.
      </p>

      <div className={styles.field}>
        <label className={styles.label}>Вакансия *</label>
        {loading ? (
          <div className={styles.loadingHint}>Загрузка списка вакансий…</div>
        ) : opportunities.length === 0 ? (
          <div className={styles.emptyHint}>На платформе нет активных вакансий</div>
        ) : (
          <select
            className={styles.select}
            value={selectedOppId}
            onChange={(e) => setSelectedOppId(e.target.value)}
            disabled={submitting}
          >
            <option value="">— выберите вакансию —</option>
            {opportunities.map((opp) => (
              <option key={opp.id} value={opp.id}>
                {opp.title} — {opp.companyName}
              </option>
            ))}
          </select>
        )}
      </div>

      <div className={styles.field}>
        <label className={styles.label}>Комментарий (необязательно)</label>
        <textarea
          className={styles.textarea}
          rows={4}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="Например: работали вместе на хакатоне, сильный fullstack-разработчик"
          maxLength={500}
          disabled={submitting}
        />
        <span className={styles.charCount}>{message.length} / 500</span>
      </div>

      <div className={styles.actions}>
        <Button variant="secondary" onClick={onClose} disabled={submitting}>
          Отмена
        </Button>
        <Button
          variant="primary"
          onClick={handleSubmit}
          isLoading={submitting}
          disabled={!selectedOppId || loading}
        >
          Рекомендовать
        </Button>
      </div>
    </Modal>
  );
}