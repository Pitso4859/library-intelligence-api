# Architecture Notes

## Objective

The project is designed as a small production-style Java service rather than a tutorial CRUD API. The main design goal is to keep HTTP concerns, business rules and persistence concerns separate enough that each can be tested independently.

## Request flow

```text
Client
  |
  v
RequestIdFilter
  |
  v
BookController / CatalogIntelligenceController
  |
  v
BookService / CatalogIntelligenceService
  |
  v
BookRepository
  |
  v
Hibernate / JPA
  |
  +--> H2 (development and tests)
  +--> PostgreSQL (production)
```

## Key decisions

### Thin controllers
Controllers own request mapping, response status codes and bounded pagination. Business rules remain in services.

### Transaction boundaries in services
All writes are wrapped by service-level `@Transactional` methods. Bulk creation validates every item before `saveAll`, providing an all-or-nothing contract.

### Polymorphic domain model
`Book` is an abstract JPA entity. `EBook` and `PrintBook` use JOINED inheritance so common data is stored once while subtype fields stay normalized.

### Explainable recommendation scoring
`CatalogIntelligenceService` deliberately uses deterministic Java scoring. The API returns the reasons behind each score so ranking behaviour can be tested and discussed during code review.

### Repository-level analytics
Aggregates such as unique authors and average sizes are calculated in the database with JPQL rather than loading the entire catalog into JVM memory.

### Request correlation
`RequestIdFilter` accepts or generates `X-Request-ID`. The ID is returned to the caller and included in error responses, providing a foundation for distributed tracing and support diagnostics.

### Production schema ownership
Flyway owns PostgreSQL schema migration in production and Hibernate runs with `ddl-auto=validate`. H2 development/test profiles use disposable schemas for speed.

## Scalability boundaries

- normal list endpoints are paginated and capped at 100 records per page
- recommendation candidate evaluation is capped at 200 records per request
- recommendation results are capped at 20
- bulk creation is capped at 100 books
- analytics uses database aggregates and a maximum of five top authors/newest books

These limits make resource usage predictable while keeping the project simple enough for a portfolio codebase.
