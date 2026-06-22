# Pitso Book Management System

> A production-ready RESTful API built with Java 17 and Spring Boot 3 for managing a dual-format book inventory — handling both digital EBooks and physical PrintBooks through a single, unified API.

---

## Why This Project Stands Out

This isn't a tutorial clone. Every architectural decision was made with **scalability, maintainability, and real-world production requirements** in mind:

- **Layered architecture** (Controller → Service → Repository) with clear separation of concerns
- **JPA JOINED inheritance** — clean schema design with no nullable columns, ready to add new book types without touching existing tables
- **Paginated from day one** — no endpoint ever returns an unbounded list
- **Consistent error contract** — every failure produces a structured JSON response, never a stack trace
- **Three test layers** — unit, service, and full integration tests covering 29 test cases
- **Docker-ready** — multi-stage Dockerfile and Docker Compose for zero-config local setup and cloud deployment

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.3 |
| Database | PostgreSQL (production), H2 (development) |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation |
| Schema Migrations | Flyway |
| API Documentation | SpringDoc OpenAPI / Swagger UI |
| Testing | JUnit 5, Mockito, Spring MockMvc |
| Containerisation | Docker, Docker Compose |
| Health Monitoring | Spring Boot Actuator |

---

## Architecture

```
┌─────────────────────────────────────────┐
│           REST Clients / Swagger UI     │
└────────────────────┬────────────────────┘
                     │ HTTP / JSON
┌────────────────────▼────────────────────┐
│           BookController                │
│   Input validation · Pagination ·      │
│   URI versioning (/api/v1/)            │
└────────────────────┬────────────────────┘
                     │
┌────────────────────▼────────────────────┐
│           BookService                   │
│   ISBN routing · Business rules ·      │
│   Type dispatch · Duplicate guards     │
└────────────────────┬────────────────────┘
                     │
┌────────────────────▼────────────────────┐
│         BookRepository (JPA)            │
│   Custom JPQL · Pagination · Search    │
└────────────────────┬────────────────────┘
                     │
┌────────────────────▼────────────────────┐
│   PostgreSQL (prod) · H2 (dev/test)    │
│   JOINED inheritance schema            │
│   Flyway version-controlled migrations │
└─────────────────────────────────────────┘
```

---

## Core Domain Logic

The system manages two book types under a single abstract `Book` entity using **JOINED table inheritance**:

| Book Type | ISBN Prefix | Extra Fields |
|---|---|---|
| `EBook` | Starts with `0` | `fileSizeKb` |
| `PrintBook` | Starts with `1` | `noOfPages`, `weightGrams` |

The ISBN prefix automatically determines which concrete type is created — no separate endpoints, no type parameter needed. The service layer handles the dispatch cleanly and explicitly.

### ISBN Validation Rules

The domain enforces these rules at the entity level, not just the controller:

| Rule | Requirement |
|---|---|
| Length | Exactly 10 characters |
| Prefix | Must be `0` or `1` |
| Characters 1–9 | Numeric digits only |
| Character 10 | Digit, `B`, or `b` |

Valid: `032156840b` · `119873456B` · `067001617B` · `1367823245`

---

## API Reference

Base path: `/api/v1/books`

| Method | Endpoint | Description | Status |
|---|---|---|---|
| `POST` | `/` | Create EBook or PrintBook | `201 Created` |
| `GET` | `/` | List all books (paginated + sortable) | `200 OK` |
| `GET` | `/{id}` | Get book by ID | `200 / 404` |
| `GET` | `/isbn/{isbnNo}` | Get book by ISBN | `200 / 404` |
| `GET` | `/search?q=` | Search by title or author | `200 OK` |
| `GET` | `/ebooks` | List all EBooks | `200 OK` |
| `GET` | `/printbooks` | List all PrintBooks | `200 OK` |
| `GET` | `/stats` | Inventory statistics | `200 OK` |
| `PATCH` | `/{id}` | Partial update | `200 / 404` |
| `DELETE` | `/{id}` | Delete book | `204 / 404` |

### Request Examples

**Create EBook**
```json
POST /api/v1/books
{
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbnNo": "032156840b",
  "fileSizeKb": 3500
}
```

**Create PrintBook**
```json
POST /api/v1/books
{
  "title": "Refactoring",
  "author": "Martin Fowler",
  "isbnNo": "119873456B",
  "noOfPages": 448,
  "weightGrams": 680.5
}
```

**Success Response**
```json
{
  "id": 1,
  "bookType": "EBOOK",
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbnNo": "032156840b",
  "sizeDetails": "EBook : Clean Code, 3500 KB",
  "fileSizeKb": 3500,
  "createdAt": "2024-06-23T10:00:00",
  "updatedAt": "2024-06-23T10:00:00"
}
```

**Error Response**
```json
{
  "timestamp": "2024-06-23T10:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "A book with ISBN '032156840b' already exists",
  "path": "/api/v1/books"
}
```

**Pagination & Sorting**
```
GET /api/v1/books?page=0&size=10&sortBy=author&direction=desc
```

| Parameter | Default | Options |
|---|---|---|
| `page` | `0` | Any integer |
| `size` | `20` | 1–100 |
| `sortBy` | `title` | `title`, `author`, `isbnNo` |
| `direction` | `asc` | `asc`, `desc` |

---

## Database Schema

```sql
-- Shared base table
books (id, book_type, title, author, isbn_no UNIQUE, created_at, updated_at)

-- EBook-specific (joined)
ebooks (id FK → books.id, file_size_kb)

-- PrintBook-specific (joined)
print_books (id FK → books.id, no_of_pages, weight_grams)
```

JOINED inheritance means each table only holds the columns relevant to that type. No nulls, no wasted storage, no ambiguous schema.

Indexes on: `isbn_no` (unique), `author`, `book_type`, `title`

---

## Running the Project

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker (optional)

### Dev Mode — H2 In-Memory Database

```bash
mvn spring-boot:run
```

| URL | What You Get |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Interactive API explorer |
| `http://localhost:8080/api/v1/books` | REST API |
| `http://localhost:8080/h2-console` | Database browser |
| `http://localhost:8080/actuator/health` | Health status |

Six sample books are auto-loaded on startup.

### Docker Compose — Full Stack (App + PostgreSQL)

```bash
docker-compose up --build
```

### Production JAR

```bash
mvn clean package -DskipTests

java -jar target/pitso-book-system-1.0.0.jar \
  --spring.profiles.active=prod \
  --DB_URL=jdbc:postgresql://your-host:5432/pitsodb \
  --DB_USERNAME=pitso \
  --DB_PASSWORD=yourpassword
```

---

## Testing

```bash
mvn test
```

**29 tests** across three layers:

| Test Class | Type | Coverage |
|---|---|---|
| `BookIsbnTest` | Unit | All ISBN validation rules and edge cases |
| `BookServiceTest` | Unit (Mockito) | Business logic, duplicate detection, type dispatch |
| `BookControllerIntegrationTest` | Integration (MockMvc) | Full HTTP request/response cycle |

---

## Project Stats

- **15** Java source files
- **1,364** lines of production and test code
- **29** automated tests
- **10** REST endpoints
- **3** test layers (unit, service, integration)
- **2** database profiles (H2 dev, PostgreSQL prod)
- **1** Dockerfile (multi-stage, non-root user, JVM-tuned)
