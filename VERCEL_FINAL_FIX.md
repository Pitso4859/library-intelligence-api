# Vercel final runtime fix

This package uses a direct Java process instead of a shell launcher.

## Why

Vercel container functions inject a certificate entrypoint wrapper. Java
containers can fail before Spring Boot starts when the wrapper cannot resolve
or correctly load the Java runtime. The Dockerfile therefore uses:

- `eclipse-temurin:17-jre-jammy`
- `/opt/java/openjdk/bin/java` as the absolute Java executable
- explicit `JAVA_HOME`, `PATH`, and `LD_LIBRARY_PATH`
- direct Docker `CMD` (no intermediate startup shell script)
- `server.address=0.0.0.0`
- `server.port=${PORT:8080}` in Spring Boot

## Required Vercel environment variables

SPRING_PROFILES_ACTIVE=prod
PORT=8080
DB_URL=jdbc:postgresql://aws-0-eu-west-2.pooler.supabase.com:5432/postgres?sslmode=require
DB_USERNAME=postgres.hfnnkgrxywlljosrbtxz
DB_PASSWORD=<your actual Supabase database password>

Apply the variables to Production, then redeploy after changing them.

## Test

/actuator/health
/swagger-ui.html
/api/v1/books

If deployment still returns 500, open Vercel Logs and inspect the first
container/application error. Production logging is intentionally INFO in this
package so Java, HikariCP and Flyway startup messages are visible.
