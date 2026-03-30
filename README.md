# Трамплин — карьерная платформа

**IT-Планета 2026 | Команда «Дети Дедлайна»**

Интерактивная карьерная экосистема для студентов, работодателей и карьерных центров вузов.

## Быстрый запуск
```bash
# 1. Клонировать репозиторий
git clone https://github.com/pablomotecci/tramplin_backend.git
cd tramplin_backend

# 2. Настроить переменные окружения
cp .env.example .env
# Отредактировать .env — указать API-ключ Yandex Geocoder

# 3. Запустить
docker-compose up -d --build

# 4. Проверить
# Swagger UI: http://localhost:8080/api/v1/swagger-ui.html
```

## Тестовые аккаунты

| Роль | Email | Пароль |
|------|-------|--------|
| Администратор | admin@tramplin.ru | Admin123! |
| Работодатель (Яндекс) | hr@yandex.ru | Test1234! |
| Работодатель (VK) | hr@vk.com | Test1234! |
| Соискатель | ivan.petrov@mail.ru | Test1234! |
| Соискатель | maria.sidorova@mail.ru | Test1234! |

## Стек технологий

- **Backend:** Java 21 + Spring Boot 4.0.2 + Spring Security + JWT
- **БД:** PostgreSQL 16 + PostGIS 3.4
- **Миграции:** Liquibase (16 миграций)
- **API-документация:** SpringDoc OpenAPI (Swagger UI)
- **Контейнеризация:** Docker + Docker Compose
- **Карты:** Yandex Maps API + Yandex Geocoder

## Архитектура

Controller → Service → Repository → Entity (PostgreSQL + PostGIS)

72 REST-эндпоинта, 15+ таблиц, 4 роли (APPLICANT, EMPLOYER, CURATOR, ADMIN).

## Авторские решения

1. **Трёхэтапная верификация компаний** — ИНН → корпоративный email → ручная проверка
2. **Система тегов с иерархией и синонимами** — ReactJS = React, Frontend включает React/Vue/Angular
3. **ScoringService** — взвешенное совпадение навыков с учётом категорий тегов
4. **Венгерский алгоритм (LAPJV)** — оптимальное распределение соискателей по вакансиям
5. **Гранулярная приватность** — 4 уровня видимости для каждого раздела профиля