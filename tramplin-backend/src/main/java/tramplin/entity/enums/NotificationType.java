package tramplin.entity.enums;

/**
 * Тип уведомления, отправляемого пользователю. Реальная SMTP-отправка
 * замокана (см. {@code NotificationService}); тип фиксируется в логе как
 * часть audit trail по NFR-3.4.
 */
public enum NotificationType {
    APPLICATION_RECEIVED,
    APPLICATION_STATUS_CHANGED,
    VERIFICATION_APPROVED,
    VERIFICATION_REJECTED
}
