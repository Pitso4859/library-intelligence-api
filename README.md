# 📚 Pitso Book Management System

A production-ready Java/Spring Boot REST API for managing book inventories — rebuilt from the original NetBeans/Swing desktop app into a scalable web service.

---

## 🏗 System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    CLIENT LAYER                         │
│        (Swagger UI / Mobile App / Web Frontend)         │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTPS / REST (JSON)
┌─────────────────────▼───────────────────────────────────┐
│                 CONTROLLER LAYER                        │
│              BookController (/api/v1/books)             │
│        Request validation · Pagination · Versioning     │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│                  SERVICE LAYER                          │
│                   BookService                           │
│       Business logic · ISBN routing · Type dispatch     │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│                 REPOSITORY LAYER                        │
│               BookRepository (JPA)                      │
│         Pagination · Search · Type-specific queries     │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│                  DATA LAYER                             │
│   PostgreSQL (prod) · H2 in-memory (dev/test)           │
│   JOINED inheritance: books + ebooks + print_books      │
└─────────────────────────────────────────────────────────┘
```

---

## 📁 File Structure

```
pitso-book-system/
├── src/
│   ├── main/
│   │   ├── java/com/primo/
│   │   │   ├── PitsoApplication.java          # Entry point
│   │   │   ├── model/
│   │   │   │   ├── Book.java                  # Abstract base entity
│   │   │   │   ├── EBook.java                 # EBook entity
│   │   │   │   ├── PrintBook.java             # PrintBook entity
│   │   │   │   └── BookDtos.java              # Request/Response DTOs
│   │   │   ├── repository/
│   │   │   │   └── BookRepository.java        # JPA repository
│   │   │   ├── service/
│   │   │   │   └── BookService.java           # Business logic
│   │   │   ├── controller/
│   │   │   │   └── BookController.java        # REST endpoints
│   │   │   ├── exception/
│   │   │   │   ├── BookExceptions.java        # Domain exceptions
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   └── config/
│   │   │       ├── OpenApiConfig.java         # Swagger config
│   │   │       └── DataSeeder.java            # Dev seed data
│   │   └── resources/
│   │       ├── application.properties         # Dev (H2)
│   │       ├── application-prod.properties    # Prod (PostgreSQL)
│   │       ├── application-test.properties    # Test (H2)
│   │       └── db/migration/
│   │           └── V1__Initial_Schema.sql     # Flyway migration
│   └── test/java/com/primo/
│       ├── model/BookIsbnTest.java            # ISBN unit tests
│       ├── service/BookServiceTest.java       # Service unit tests
│       └── controller/BookControllerIntegrationTest.java
├── Dockerfile                                 # Multi-stage build
├── docker-compose.yml                         # Full stack local dev
└── pom.xml
```

---

## 🗄 Database Schema

```sql
-- JOINED inheritance strategy: shared base + type-specific tables

books                        ebooks              print_books
─────────────────────        ──────────────      ───────────────────
id           BIGINT PK  ──►  id          FK  ──► id           FK
book_type    VARCHAR(10)      file_size_kb INT    no_of_pages  INT
title        VARCHAR(255)                         weight_grams FLOAT
author       VARCHAR(255)
isbn_no      VARCHAR(10) UNIQUE
created_at   TIMESTAMP
updated_at   TIMESTAMP
```

---

## 🔌 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/books` | Create EBook or PrintBook |
| `GET` | `/api/v1/books` | List all books (paginated) |
| `GET` | `/api/v1/books/{id}` | Get by ID |
| `GET` | `/api/v1/books/isbn/{isbn}` | Get by ISBN |
| `GET` | `/api/v1/books/search?q=` | Search by title/author |
| `GET` | `/api/v1/books/ebooks` | List all EBooks |
| `GET` | `/api/v1/books/printbooks` | List all PrintBooks with weight |
| `GET` | `/api/v1/books/stats` | Inventory statistics |
| `PATCH` | `/api/v1/books/{id}` | Partial update |
| `DELETE` | `/api/v1/books/{id}` | Delete a book |

### Create EBook (ISBN starts with '0')
```json
POST /api/v1/books
{
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbnNo": "032156840b",
  "fileSizeKb": 3500
}
```

### Create PrintBook (ISBN starts with '1')
```json
POST /api/v1/books
{
  "title": "Refactoring",
  "author": "Martin Fowler",
  "isbnNo": "1198734561B",
  "noOfPages": 448,
  "weightGrams": 680.5
}
```

---

## 📐 ISBN Business Rules (Preserved from original)

| Rule | Detail |
|------|--------|
| Length | Exactly 10 characters |
| Prefix | Must start with `0` (EBook) or `1` (PrintBook) |
| First 9 chars | Must all be numeric digits |
| Last char | Must be a digit, `B`, or `b` |

---

## 🚀 Running the Application

### Option 1: Dev mode (H2 in-memory)
```bash
# Requires Java 17+ and Maven
mvn spring-boot:run

# API: http://localhost:8080/api/v1/books
# Swagger: http://localhost:8080/swagger-ui.html
# H2 Console: http://localhost:8080/h2-console
```

### Option 2: Docker Compose (PostgreSQL + App)
```bash
docker-compose up --build

# App: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
```

### Option 3: Production JAR
```bash
mvn clean package -DskipTests
java -jar target/pitso-book-system-1.0.0.jar --spring.profiles.active=prod \
     --DB_URL=jdbc:postgresql://yourhost:5432/pitsodb \
     --DB_USERNAME=primo \
     --DB_PASSWORD=yourpassword
```

---

## 🧪 Running Tests
```bash
mvn test
```

---

## ♻️ What Changed from the Original

| Original (NetBeans Swing) | Rebuilt (Spring Boot REST API) |
|--------------------------|-------------------------------|
| Desktop GUI (JFrame) | RESTful HTTP API |
| In-memory ArrayList | PostgreSQL + JPA/Hibernate |
| No persistence | JOINED inheritance schema |
| Single user | Multi-user, concurrent |
| No error API | Consistent JSON error responses |
| No search | Full-text search with pagination |
| No tests beyond JUnit | Unit + Integration test suite |
| Cannot scale | Docker-ready, stateless, scalable |

---

## 📈 Scaling to Millions of Users

- **Horizontal scaling**: Stateless Spring Boot containers behind a load balancer
- **DB connection pooling**: HikariCP (20 connections/pod default)
- **Read replicas**: Point `@Transactional(readOnly=true)` queries to replica
- **Caching**: Add Redis (`spring-boot-starter-data-redis`) for hot ISBN lookups
- **Search at scale**: Replace LIKE queries with Elasticsearch
- **API rate limiting**: Add Spring Cloud Gateway or Bucket4j
- **Observability**: Actuator + Micrometer + Prometheus + Grafana
