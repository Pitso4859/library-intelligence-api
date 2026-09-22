# Pitso Library Intelligence API

A production-style **Java 17 + Spring Boot 3** backend that manages EBooks and PrintBooks through one REST API and demonstrates the engineering practices expected in junior-to-mid Java backend roles.

The application logic is Java. There is no JavaScript frontend: the project focuses on REST design, OOP, JPA/Hibernate, transactions, validation, testing, observability and PostgreSQL-ready persistence.

## Why I choose to build this project

This repository goes beyond basic CRUD. It demonstrates:

- **Object-oriented domain modelling** with `Book`, `EBook` and `PrintBook`
- **JPA JOINED inheritance** for clean relational schema design
- **Transactional bulk operations** with all-or-nothing semantics
- **Explainable recommendation engine** implemented in Java with deterministic scoring
- **Catalog analytics** using JPQL aggregation and projection interfaces
- **API request tracing** through `X-Request-ID`
- **Centralized exception handling** with stable JSON error contracts
- **Case-insensitive duplicate ISBN protection**
- **Bounded pagination and sort-field whitelisting**
- **Bean Validation** for request contracts
- **PostgreSQL + Flyway** for production schema management
- **H2** for fast local development and integration testing
- **Swagger / OpenAPI** for interactive API documentation
- **Actuator** health and metrics endpoints
- **39 automated tests** across domain, service and integration layers
- Docker support for deployable local environments

## New engineering features in v1.1

### 1. Explainable recommendation engine

```http
GET /api/v1/catalog/recommendations?q=java&preferredType=EBOOK&limit=5
```

Instead of returning an unexplained ranking, each recommendation includes its score and reasons:

```json
[
  {
    "id": 1,
    "title": "Effective Java",
    "author": "Joshua Bloch",
    "isbnNo": "012345671B",
    "bookType": "EBOOK",
    "score": 53,
    "reasons": [
      "Title matches 'java'",
      "Matches preferred format EBOOK",
      "Compact digital edition"
    ]
  }
]
```

The algorithm is intentionally deterministic and explainable so it can be unit tested, debugged and replaced later without changing the API contract.

### 2. Catalog intelligence / analytics

```http
GET /api/v1/catalog/insights
```

Returns operational catalog metrics such as:

- total books by type
- unique authors
- average EBook size
- average PrintBook page count and weight
- top authors
- newest books

The aggregation is performed through Spring Data JPA/JPQL and lightweight projection interfaces.

### 3. Transactional bulk creation

```http
POST /api/v1/books/bulk
Content-Type: application/json
```

```json
{
  "books": [
    {
      "title": "Effective Java",
      "author": "Joshua Bloch",
      "isbnNo": "012345670B",
      "fileSizeKb": 2400
    },
    {
      "title": "Java Concurrency in Practice",
      "author": "Brian Goetz",
      "isbnNo": "112345670B",
      "noOfPages": 424,
      "weightGrams": 620.0
    }
  ]
}
```

The service validates the complete payload before saving. If one item is invalid or duplicated, **nothing is persisted**.

### 4. Request tracing

Every response contains:

```http
X-Request-ID: 16844e0f-17ce-4f48-a79f-5af8ffec44d7
```

Clients may also send their own `X-Request-ID`. Error responses include the same request ID so production failures are easier to trace.

## Architecture

```text
HTTP Client / Swagger
        |
        v
RequestIdFilter
        |
        v
Controllers
  |                 \
  v                  v
BookService     CatalogIntelligenceService
  |                  |
  +--------+---------+
           v
      BookRepository
           |
           v
 Spring Data JPA / Hibernate
           |
     +-----+------+
     |            |
    H2        PostgreSQL
  dev/test       prod
```

### Layer responsibilities

| Layer | Responsibility |
|---|---|
| Controller | HTTP mapping, validation entry point, safe pagination |
| Service | Business rules, transactions, recommendation scoring |
| Repository | Persistence, JPQL search, analytics queries |
| Domain | Book inheritance and ISBN rules |
| Advice / Filter | Error contract and request traceability |

## Domain model

The API uses polymorphism rather than separate unrelated models.

| Type | ISBN prefix | Type-specific data |
|---|---:|---|
| `EBook` | `0` | `fileSizeKb` |
| `PrintBook` | `1` | `noOfPages`, `weightGrams` |

JPA uses `InheritanceType.JOINED`, producing a shared `books` table and subtype tables for `ebooks` and `print_books`.

## API reference

### Books

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/books` | Create one book |
| POST | `/api/v1/books/bulk` | Create up to 100 books atomically |
| GET | `/api/v1/books` | Paginated book list |
| GET | `/api/v1/books/{id}` | Get by ID |
| GET | `/api/v1/books/isbn/{isbn}` | Get by ISBN |
| GET | `/api/v1/books/search?q=` | Search title/author |
| GET | `/api/v1/books/ebooks` | EBooks |
| GET | `/api/v1/books/printbooks` | PrintBooks |
| GET | `/api/v1/books/stats` | Basic inventory totals |
| PATCH | `/api/v1/books/{id}` | Partial update |
| DELETE | `/api/v1/books/{id}` | Delete book |

### Catalog intelligence

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/catalog/recommendations` | Explainable ranked recommendations |
| GET | `/api/v1/catalog/insights` | Aggregated catalog analytics |

## Safe pagination

```http
GET /api/v1/books?page=0&size=20&sortBy=author&direction=asc
```

Rules:

- page must be `>= 0`
- size must be `1..100`
- direction must be `asc` or `desc`
- sort fields are whitelisted: `id`, `title`, `author`, `isbnNo`, `createdAt`, `updatedAt`

Invalid values return HTTP `400` rather than leaking persistence errors.

## Error contract

```json
{
  "timestamp": "2026-08-29T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Book not found with id: 99",
  "path": "/api/v1/books/99",
  "requestId": "16844e0f-17ce-4f48-a79f-5af8ffec44d7"
}
```

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| REST | Spring Web MVC |
| Persistence | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation |
| Production DB | PostgreSQL |
| Development / Test DB | H2 |
| Migrations | Flyway |
| API docs | SpringDoc OpenAPI / Swagger UI |
| Monitoring | Spring Boot Actuator |
| Testing | JUnit 5, Mockito, MockMvc |
| Build | Maven |
| Deployment | Docker / Docker Compose |

## Run locally

### Requirements

- JDK 17+
- Maven 3.8+

```bash
mvn clean test
mvn spring-boot:run
```

Useful URLs:

| URL | Purpose |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Interactive API documentation |
| `http://localhost:8080/api/v1/books` | Books API |
| `http://localhost:8080/api/v1/catalog/insights` | Analytics |
| `http://localhost:8080/h2-console` | Development DB console |
| `http://localhost:8080/actuator/health` | Health check |

## Run with Docker + PostgreSQL

```bash
docker compose up --build
```

For production, activate the `prod` profile and provide credentials through environment variables:

```bash
java -jar target/pitso-book-system-1.1.0.jar \
  --spring.profiles.active=prod \
  --DB_URL=jdbc:postgresql://localhost:5432/pitsodb \
  --DB_USERNAME=pitso \
  --DB_PASSWORD=change-me
```

Flyway owns production schema migration while Hibernate runs in `validate` mode.

## Testing strategy

Run:

```bash
mvn test
```

The repository contains **39 tests** covering:

- ISBN domain rules and edge cases
- EBook / PrintBook creation logic
- duplicate handling
- transactional bulk-create behaviour
- recommendation ranking and format filtering
- invalid recommendation parameters
- REST integration with H2
- request validation and error responses
- request ID propagation
- safe sorting and pagination

## Engineering decisions worth discussing in an interview

**Why JOINED inheritance?**  
It keeps common fields in one table while avoiding subtype-specific nullable columns. It also lets JPA query the base class polymorphically.

**Why deterministic recommendations instead of an AI dependency?**  
The goal is backend engineering. The scoring is explainable, testable and cheap. The API contract could later be backed by ML without changing clients.

**Why pre-validate a bulk request?**  
It avoids partial writes and makes the transaction contract clear: all books are created or none are.

**Why return request IDs?**  
Production systems need traceability. A user can report a request ID and developers can correlate it with logs or monitoring.

**Why whitelist sort fields?**  
It prevents invalid/unexpected entity-property access and gives the public API a deliberate contract.

## Suggested future improvements

- Spring Security with JWT and role-based permissions
- optimistic locking for concurrent updates
- Redis/Caffeine caching for hot read paths
- Testcontainers for PostgreSQL integration tests
- rate limiting and API quotas
- event-driven audit trail using domain events

## Author

**Pitso Nkotolane**  
Java / Software Developer  
GitHub: https://github.com/Pitso4859

---

If you are reviewing this repository for a software engineering role, start with `BookService`, `CatalogIntelligenceService`, `BookRepository`, `RequestIdFilter`, and the integration tests. They contain the main engineering decisions in the project.


## Web frontend

The project now includes a server-rendered frontend built with **Spring Boot + Thymeleaf**.
The UI uses the same Java services as the REST API, so book validation, CRUD behavior,
and catalog intelligence remain in one business layer.

After starting the application with `mvn spring-boot:run`, open:

- `http://localhost:8080/` — frontend landing page
- `http://localhost:8080/dashboard` — inventory dashboard
- `http://localhost:8080/books` — book inventory and CRUD
- `http://localhost:8080/catalog` — catalog insights and recommendations
- `http://localhost:8080/api` — API status JSON
- `http://localhost:8080/swagger-ui.html` — REST API documentation

The frontend is responsive and requires no separate Node.js build.
