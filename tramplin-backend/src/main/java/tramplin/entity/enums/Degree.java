package tramplin.entity.enums;

/**
 * Уровень образования в записи resume_education.
 * BACHELOR — бакалавриат
 * MASTER — магистратура
 * SPECIALIST — специалитет (российская специфика)
 * PHD — аспирантура / докторантура
 * COLLEGE — среднее специальное (колледж, техникум)
 * OTHER — иное (онлайн-курсы, профпереподготовка)
 */
public enum Degree {
    BACHELOR,
    MASTER,
    SPECIALIST,
    PHD,
    COLLEGE,
    OTHER
}