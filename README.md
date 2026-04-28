# Библиотечная система

## Описание проекта
Данный проект представляет собой RESTful веб-приложение на `Spring Boot` для управления библиотекой.
В качестве хранилища данных используется `PostgreSQL`, доступ к данным реализован через `JPA (Hibernate/Spring Data)`.
Приложение построено по многослойной архитектуре `Controller -> Service -> Repository` и использует `DTO + mapper`.

## Выполняемые функции
Приложение предоставляет CRUD API для сущностей:
- `Book`
- `Author`
- `Category`
- `Publisher`
- `Reader`
- `Loan`

Основные примеры эндпоинтов:
- `GET /api/books` — получить все книги.
- `GET /api/books/{id}` — получить книгу по ID (`@PathVariable`).
- `GET /api/books/search?author=...` — поиск книг по автору (`@RequestParam`).
- `POST /api/books` — создать книгу.
- `POST /api/loans/bulk/without-transaction` — массово создать выдачи без общей транзакции.
- `POST /api/loans/bulk/with-transaction` — массово создать выдачи в одной транзакции.
- `PUT /api/books/{id}` — обновить книгу.
- `DELETE /api/books/{id}` — удалить книгу.

## Модель данных
Сущности:
1. `Book` (книга)
2. `Author` (автор)
3. `Category` (категория)
4. `Publisher` (издательство)
5. `Reader` (читатель)
6. `Loan` (выдача книги)

Связи:
- `Publisher (OneToMany) -> Book`
- `Book (ManyToMany) <-> Author` через `book_authors`
- `Book (ManyToMany) <-> Category` через `book_categories`
- `Reader (OneToMany) -> Loan`
- `Book (OneToMany) -> Loan`

## Технические детали
1. Подключение реляционной БД:
- Используется `PostgreSQL`.
- Параметры подключения задаются в `application.yml` (с поддержкой env vars).

2. `CascadeType` и `FetchType`:
- Для связей в основном используется `FetchType.LAZY`.
- Для `ManyToMany` в `Book` используется `CascadeType.PERSIST, CascadeType.MERGE`.
- Для `Reader -> Loan` используется `CascadeType.ALL` и `orphanRemoval=true`.

3. Демонстрация проблемы N+1 и решения:
- Проблема: `GET /api/books/n-plus-one` (обычный `findAll()`).
- Решение: `GET /api/books/with-entity-graph` (репозиторий с `@EntityGraph`).

4. Демонстрация транзакций:
- Без транзакции: `POST /api/scenarios/without-transaction`  
  при ошибке на несуществующем `authorId` часть данных сохраняется.
- С транзакцией: `POST /api/scenarios/with-transaction`  
  при той же ошибке изменения откатываются полностью.
- Для bulk-операции выдачи книг:
  - `POST /api/loans/bulk/without-transaction`
  - `POST /api/loans/bulk/with-transaction`
  - при ошибке на втором элементе списка видно различие в состоянии таблицы `loans`.

5. Data caching:
- `GET /api/books/filter/jpql` — сложный фильтр по вложенным сущностям через `@Query` (`JPQL`).
- `GET /api/books/filter/native` — аналогичный фильтр через `native query`.
- Для обоих запросов добавлена пагинация через `page` и `size`.
- Ранее запрошенные результаты сохраняются во внутренний in-memory индекс на основе `HashMap`.
- Ключ индекса составной и включает:
  - тип запроса (`jpql` или `native`)
  - `authorLastName`
  - `categoryName`
  - `publisherCountry`
  - `page`
  - `size`
- Для ключа реализованы корректные `equals()` и `hashCode()`.
- Работа индекса и его инвалидация демонстрируются через логи приложения.
- При изменении книг, авторов, категорий или издательств индекс очищается, чтобы не возвращать устаревшие данные.

## Лабораторная 3
Реализованы требования по теме `Data caching`:
- сложный `GET` с фильтрацией по вложенным сущностям через `JPQL`;
- аналогичный запрос через `native SQL`;
- пагинация через `Pageable`;
- in-memory индекс на `HashMap<K, V>`;
- инвалидация индекса при изменении данных.

Основные эндпоинты:
- `GET /api/books/filter/jpql?authorLastName=Булгаков&categoryName=Классика&publisherCountry=Россия&page=0&size=5`
- `GET /api/books/filter/native?authorLastName=Булгаков&categoryName=Классика&publisherCountry=Россия&page=0&size=5`
- `GET /api/books/filter/jpql?page=0&size=2`
- `GET /api/books/filter/jpql?page=1&size=2`

Поля ответа:
- `content` — список книг на текущей странице;
- `page` — номер страницы (начиная с `0`);
- `size` — размер страницы;
- `totalElements` — общее количество найденных книг;
- `totalPages` — общее количество страниц;
- `queryType` — тип использованного запроса (`jpql` или `native`).

## Лабораторная 4
Реализованы требования по теме `Bulk-операции, Stream API, Optional и транзакции`:
- бизнес-операция массовой выдачи книг через `POST` со списком объектов;
- два режима выполнения bulk-операции:
  - `POST /api/loans/bulk/without-transaction`
  - `POST /api/loans/bulk/with-transaction`
- использование `Stream API` и `Optional` в `LoanService`;
- бизнес-правило: одна книга не может иметь более одной активной выдачи;
- unit-тесты сервисного слоя на `Mockito`.

Для пошаговой демонстрации лабораторной работы см. `LAB4_DEMO.md`.

## Unit-тесты сервисов
`Mockito`-тестами покрыты все сервисы проекта:
- `AuthorService`
- `BookService`
- `CategoryService`
- `LoanService`
- `PublisherService`
- `ReaderService`
- `ScenarioService`

Запуск:
```bash
mvn test
```

## Покрытие кода и Sonar
Для покрытия подключён `JaCoCo`:
- XML-отчёт для Sonar: `target/site/jacoco/jacoco.xml`
- HTML-отчёт для локального просмотра: `target/site/jacoco/index.html`

Сгенерировать отчёт локально:
```bash
mvn clean verify
```

В `SonarCloud` передаётся coverage по `JaCoCo XML report`. Для метрики покрытия исключён boilerplate-слой:
- `controller`
- `dto`
- `entity`
- `repository`
- `mapper`
- `cache`
- `exception`
- `aop`

За счёт этого процент покрытия в Sonar отражает именно покрытие бизнес-логики сервисного слоя.

## CI Pipeline
В проекте настроен GitHub Actions workflow:
- сборка проекта;
- запуск unit-тестов;
- генерация `JaCoCo`-отчёта;
- отправка анализа в `SonarCloud`.

Файл workflow:
- `.github/workflows/ci-cd.yml`

Для работы анализа в `SonarCloud` нужно добавить GitHub Secret:
- `SONAR_TOKEN`

## Запуск проекта
1. Создать БД:
```sql
CREATE DATABASE library;
```
2. Проверить `src/main/resources/application.yml`.
3. Запустить приложение:
```bash
mvn spring-boot:run
```

## Docker
В проект добавлены Docker-конфигурации для локального запуска backend, frontend и PostgreSQL.

1. Создать локальный env-файл при необходимости:
```bash
cp .env.example .env
```
2. Запустить весь стек:
```bash
docker compose up --build
```
3. Проверить сервисы:
- backend: `http://localhost:8080/actuator/health/readiness`
- frontend: `http://localhost:3000`

Основные переменные окружения:
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `CORS_ALLOWED_ORIGINS`
- `VITE_API_BASE_URL`

## Swagger UI
`http://localhost:8080/swagger-ui.html`

## Frontend SPA
В репозитории добавлен отдельный frontend на `React + TypeScript + Vite`:

- каталог: `frontend/`
- документация по структуре и запуску: `frontend/README.md`

Быстрый запуск:
```bash
cd frontend
npm install
npm run dev
```

По умолчанию Vite proxy направляет `/api` на `http://localhost:8080`, поэтому фронтенд можно использовать вместе с текущим Spring Boot backend без изменения базового URL в коде.

## SonarCloud
`https://sonarcloud.io/project/overview?id=ownage69_JavaProject`

## CI/CD и Render
Workflow `.github/workflows/ci-cd.yml` выполняет backend build/test, frontend build,
Docker build, deploy на Render через deploy hooks и healthcheck после deploy.

Для деплоя в GitHub Secrets нужно добавить:
- `RENDER_BACKEND_DEPLOY_HOOK_URL` или старое имя `RENDER_DEPLOY_HOOK_URL`
- `RENDER_FRONTEND_DEPLOY_HOOK_URL` или старое имя `FRONTEND_RENDER_DEPLOY_HOOK_URL`
- `BACKEND_APP_BASE_URL` или старое имя `APP_BASE_URL`
- `FRONTEND_APP_BASE_URL`

Render Blueprint находится в `render.yaml`: backend разворачивается как Docker Web
Service, frontend как Static Site, база данных как Render Postgres.
