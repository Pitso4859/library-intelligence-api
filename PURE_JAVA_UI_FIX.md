# Pure Java UI fix

The Spring Boot project is now API-only. The desktop interface is the Java Swing application in `library-intelligence-java-swing-ui`.

Changes:
- Removed the obsolete Thymeleaf `LibraryWebController`.
- Removed the Thymeleaf Maven dependency.
- Mapped both `/` and `/api` to JSON API information.
- Updated the root integration test to expect JSON rather than an HTML view.
- Kept `src/main/resources` configuration and Flyway migration files required by the backend.

Run:

```powershell
mvn clean test
mvn spring-boot:run
```

Then open `http://localhost:8080/` to verify the API response.
