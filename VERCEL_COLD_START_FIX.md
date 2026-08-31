# Vercel Cold-Start Fix

## Root cause

Vercel runtime logs showed that Java started correctly and Supabase connected correctly, but Spring Boot was still initializing Flyway/JPA when Vercel's 15-second port startup window expired.

Evidence from the runtime logs included:

- `HikariPool-1 - Start completed.`
- `Flyway Community Edition ...`
- `Initialized JPA EntityManagerFactory ...`
- immediately followed by `could not connect to $PORT=8080 ... startup timeout (15s)`

The deployment therefore failed because the web server was not listening soon enough, not because the Docker image or Supabase credentials were wrong.

## Changes in this package

`application-prod.properties` now:

- Enables global lazy bean initialization.
- Uses lazy Spring Data JPA repository bootstrapping.
- Prevents Hibernate from querying JDBC metadata during application startup.
- Changes Hibernate schema handling from `validate` to `none`.
- Disables Flyway during Vercel cold starts.
- Makes Hikari avoid blocking startup for its first database connection.
- Disables database participation in the Actuator health endpoint.
- Keeps the pool small for a serverless/container workload.

## Important

Flyway is disabled at runtime because the Supabase schema was already created/validated during earlier deployments. If you later add a new migration, run that migration against Supabase deliberately before deploying the new application version, or temporarily enable Flyway for a controlled migration run.

## Required Vercel variables

```text
SPRING_PROFILES_ACTIVE=prod
PORT=8080
DB_URL=jdbc:postgresql://aws-0-eu-west-2.pooler.supabase.com:5432/postgres?sslmode=require
DB_USERNAME=postgres.hfnnkgrxywlljosrbtxz
DB_PASSWORD=<your real Supabase database password>
```

## Test order

1. Deploy the new commit to Vercel.
2. Open `/actuator/health` first.
3. Expected response: `{"status":"UP"}`.
4. Then open `/api/v1/books` to trigger the first real JPA/database initialization.
5. The first database request can be slower than subsequent requests, but Vercel should already have connected to the HTTP server by then.
