# Vercel Deployment - Pitso Book System

This repository is prepared for Vercel's Container runtime.

## Files added for Vercel

- `Dockerfile.vercel` - builds and runs the Java 17 Spring Boot application.
- `vercel.json` - exposes the container service to all public routes.
- `.dockerignore` - keeps unnecessary local files out of the container build.
- `.env.vercel.example` - lists the required environment variables without secrets.

## 1. Use a managed PostgreSQL database

Do not deploy the PostgreSQL service from `docker-compose.yml` to Vercel. Use a managed PostgreSQL provider such as Neon, Supabase, Railway, or another PostgreSQL host.

The production Spring profile already uses:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Flyway will run `src/main/resources/db/migration/V1__Initial_Schema.sql` when the production profile starts.

## 2. Push this folder to GitHub

The repository root must contain:

- `pom.xml`
- `Dockerfile.vercel`
- `vercel.json`
- `src/`

## 3. Import the repository in Vercel

Create a new Vercel project from the GitHub repository.

Use:

- Framework Preset: `Container`
- Root Directory: `./`
- Build Command: Automatic
- Output Directory: Automatic

## 4. Add environment variables in Vercel

Add these under Project Settings -> Environment Variables:

```text
SPRING_PROFILES_ACTIVE=prod
PORT=8080
DB_URL=jdbc:postgresql://YOUR_POSTGRES_HOST/YOUR_DATABASE?sslmode=require
DB_USERNAME=YOUR_DATABASE_USERNAME
DB_PASSWORD=YOUR_DATABASE_PASSWORD
```

Optional:

```text
DB_MAX_POOL_SIZE=5
DB_MIN_IDLE=0
```

Apply them to Production and Preview if both environments should use PostgreSQL.

## 5. Deploy and test

After deployment, test:

```text
https://YOUR-PROJECT.vercel.app/actuator/health
https://YOUR-PROJECT.vercel.app/swagger-ui.html
https://YOUR-PROJECT.vercel.app/api/v1/books
```

The root `/` may return the application's own 404 because this repository is a REST API rather than a frontend website.

## Local development

The default profile still uses H2, so you can continue to run locally with:

```bash
mvn spring-boot:run
```

The application defaults to port 8080 locally.

## Cold-start optimization

This build defers JPA/database initialization so Vercel can receive an HTTP connection inside its container startup window. See `VERCEL_COLD_START_FIX.md`.
