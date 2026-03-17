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
- При повторе одинакового запроса ответ может быть возвращен из кэша (`"cached": true`).
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
- `cached` — был ли ответ взят из in-memory индекса;
- `queryType` — тип использованного запроса (`jpql` или `native`).

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

## Swagger UI
`http://localhost:8080/swagger-ui.html`

## SonarCloud
`https://sonarcloud.io/project/overview?id=ownage69_JavaProject`
