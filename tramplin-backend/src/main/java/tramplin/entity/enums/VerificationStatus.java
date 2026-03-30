package tramplin.entity.enums;

public enum VerificationStatus {
    UNVERIFIED, // Профиль создан, но документы не отправлены
    PENDING,    // Ожидает проверки куратором
    VERIFIED,   // Проверка пройдена (может публиковать вакансии)
    REJECTED    // Проверка не пройдена
}