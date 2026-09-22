# Root 500 fix

The deployed application was running, but opening `/` produced a JSON 500 error.

## Cause

There was no controller mapped to `/`. Spring Boot treated the request as a
missing static resource and raised `NoResourceFoundException`. The global
`@ExceptionHandler(Exception.class)` then converted that normal 404 condition
into a misleading HTTP 500 response.

## Changes

- Added `HomeController`:
  - `/` and `/api` now return HTTP 200 with lightweight API metadata and links.
  - The handler does not query the database, so the root URL is safe during a
    database cold start or temporary database problem.
- Added an explicit `NoResourceFoundException` handler so unknown URLs return
  HTTP 404 instead of HTTP 500.
- Added server-side logging for genuine unhandled exceptions so future Vercel
  500 errors include the real stack trace in application logs.

After redeploying, opening the base Vercel URL should return a JSON response
with `"status":"UP"`. Swagger remains available at `/swagger-ui.html`.
