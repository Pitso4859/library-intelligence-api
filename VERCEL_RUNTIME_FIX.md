# Vercel Runtime Fix

This package includes the runtime fix for the Vercel error:

`Error: could not connect to $PORT=8080`

## What changed

- `start-vercel.sh` now starts Java explicitly and binds Spring Boot to `0.0.0.0:$PORT`.
- `Dockerfile.vercel` defines Java runtime paths explicitly and launches the startup script.
- `application.properties` includes `server.address=0.0.0.0`.
- Production database variables no longer silently fall back to localhost/default credentials.
- Flyway retries the production database connection a few times during cold starts.

## Required Vercel environment variables

Use these under Vercel -> Project -> Settings -> Environment Variables:

```text
SPRING_PROFILES_ACTIVE=prod
PORT=8080
DB_URL=jdbc:postgresql://aws-0-eu-west-2.pooler.supabase.com:5432/postgres?sslmode=require
DB_USERNAME=postgres.hfnnkgrxywlljosrbtxz
DB_PASSWORD=YOUR_REAL_SUPABASE_DATABASE_PASSWORD
```

Do not commit the real database password.

After adding/updating variables, redeploy the project.

## Test URLs

- `/actuator/health`
- `/swagger-ui.html`
- `/api/v1/books`

If startup still fails, Vercel Runtime Logs should now show the startup banner and indicate whether the three database variables are present, without printing their secret values.
