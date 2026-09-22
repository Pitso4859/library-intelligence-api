# Library Intelligence Java Desktop UI

A desktop frontend for the Library Intelligence Spring Boot API built entirely with Java Swing.

## No web frontend technologies

This project intentionally uses:

- Java 21
- Swing / AWT
- Java `HttpClient`
- No HTML
- No CSS
- No JavaScript
- No React
- No Thymeleaf
- No FXML
- No third-party runtime dependencies

## Features

- Dashboard with book inventory totals and newest books
- Books table with title/author search and book type filter
- Add EBooks and PrintBooks
- Edit existing books
- Delete books
- Catalog Intelligence recommendations
- Catalog analytics and top-author insights
- Configurable API URL from the desktop application

## API

By default the client connects to:

`https://library-intelligence-api.vercel.app`

You can change this inside the application using **API Settings**, or run against your local backend.

## Run in VS Code

Open this folder in VS Code and run:

```powershell
mvn clean package
java -jar target\library-intelligence-java-ui-1.0.0.jar
```

To use the local Spring Boot API:

```powershell
java -Dlibrary.api.url=http://localhost:8080 -jar target\library-intelligence-java-ui-1.0.0.jar
```

Or set an environment variable:

```powershell
$env:LIBRARY_API_URL="http://localhost:8080"
java -jar target\library-intelligence-java-ui-1.0.0.jar
```

## Important

Swing is a desktop UI toolkit. This frontend opens as a Windows desktop application; it does not render inside a web browser and cannot itself be hosted as a browser UI on Vercel. Your Spring Boot REST API may remain deployed on Vercel, while this Java desktop client connects to it over HTTP.
