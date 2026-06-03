package tramplin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tramplin.entity.enums.NotificationType;

/**
 * Уведомления пользователей. Реальная отправка (SMTP) сознательно замокана для
 * конкурса — вместо неё каждое событие пишется структурным логом через SLF4J,
 * что закрывает NFR-3.4 (логирование ключевых операций) и служит audit trail.
 * <p>
 * Контракт: {@link #send} никогда не бросает наружу. Уведомление — это побочный
 * эффект бизнес-операции (отклик, верификация), и его сбой не должен откатывать
 * саму операцию. Для реальной отправки следующий шаг — вынос в after-commit/async.
 */
@Slf4j
@Service
public class NotificationService {

    public void send(String toEmail, NotificationType type, String subject) {
        try {
            log.info("[EMAIL] to={} type={} subject='{}'", toEmail, type, subject);
        } catch (Exception e) {
            log.warn("[EMAIL] не удалось сформировать уведомление для {}: {}", toEmail, e.getMessage());
        }
    }
}
