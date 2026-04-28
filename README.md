# Библиотечная система

Веб-приложение для управления библиотекой: книги, авторы, категории, издательства, читатели и выдачи книг.

Проект состоит из Spring Boot backend, React frontend и PostgreSQL базы данных.

## Стек

- Backend: Java 17, Spring Boot, Spring Data JPA, PostgreSQL
- Frontend: React, TypeScript, Vite, Axios
- Инфраструктура: Docker Compose, Render
- Тесты: JUnit 5, Mockito, JaCoCo

## Возможности

- CRUD для книг, авторов, категорий, издательств, читателей и выдач.
- Поиск и фильтрация книг.
- Учет доступных и выданных экземпляров.
- Загрузка обложек книг. Обложка сохраняется вместе с книгой и видна с разных устройств.
- Swagger UI для просмотра API.
- Отдельный frontend-интерфейс для работы с каталогом.

## Быстрый запуск через Docker

1. Создайте `.env` из примера:

```bash
cp .env.example .env
```

2. Запустите приложение:

```bash
docker compose up --build
```

3. Откройте сервисы:

- Frontend: `http://localhost:3000`
- Backend healthcheck: `http://localhost:8080/actuator/health/readiness`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Локальный запуск без Docker

Backend требует PostgreSQL базу `library`.

```bash
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

По умолчанию Vite проксирует `/api` на `http://localhost:8080`.

## Основные переменные окружения

Backend:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_JPA_HIBERNATE_DDL_AUTO`
- `CORS_ALLOWED_ORIGINS`

Frontend:

- `VITE_API_BASE_URL`
- `VITE_BOOK_COVERS_BASE_URL`
- `VITE_AUTH_MODE`
- `VITE_BACKEND_TARGET`

Примеры находятся в `.env.example` и `frontend/.env.example`.

## Полезные API endpoints

- `GET /api/books` - список книг
- `GET /api/books/{id}` - книга по ID
- `POST /api/books` - создать книгу
- `PUT /api/books/{id}` - обновить книгу
- `DELETE /api/books/{id}` - удалить книгу
- `GET /api/books/search?author=...` - поиск по автору
- `GET /api/books/filter/jpql` - фильтрация книг через JPQL
- `GET /api/books/filter/native` - фильтрация книг через native SQL
- `POST /api/loans/bulk/with-transaction` - массовая выдача книг в транзакции

Полный список доступен в Swagger UI.

## Проверка проекта

Backend tests:

```bash
mvn clean test
```

Frontend build:

```bash
cd frontend
npm run build
```

## Структура

```text
src/main/java/com/library   backend source code
src/test/java/com/library   backend tests
frontend/                   React frontend
booksimages/                статические обложки для демо-каталога
scripts/                    вспомогательные скрипты
docker-compose.yml          локальный запуск всего стека
render.yaml                 конфигурация деплоя на Render
```
