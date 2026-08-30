# Changelog

## 1.1.0

- Added explainable Java recommendation engine.
- Added catalog analytics/insights API.
- Added transactional bulk book creation (up to 100 items).
- Added request correlation with `X-Request-ID`.
- Added request IDs to structured error responses.
- Added case-insensitive duplicate ISBN checks.
- Added safe pagination and sort-field whitelisting.
- Added database conflict handling.
- Enabled Flyway dependency for production migrations.
- Prevented development seed data from contaminating integration tests.
- Expanded automated test coverage to 39 tests.
- Removed unused Lombok dependency and repository build artifacts.

## Vercel runtime port binding fix
- Added `start-vercel.sh` to bind Spring Boot explicitly to `0.0.0.0:$PORT`.
- Added explicit Temurin Java runtime environment paths.
- Removed production database localhost/default-credential fallbacks.
- Added Flyway connection retries for container cold starts.
