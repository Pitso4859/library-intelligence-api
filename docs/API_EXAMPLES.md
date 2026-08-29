# API Examples

## Create an EBook

```bash
curl -X POST http://localhost:8080/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Effective Java",
    "author":"Joshua Bloch",
    "isbnNo":"012345671B",
    "fileSizeKb":2200
  }'
```

## Bulk create

```bash
curl -X POST http://localhost:8080/api/v1/books/bulk \
  -H "Content-Type: application/json" \
  -d '{
    "books":[
      {
        "title":"Effective Java",
        "author":"Joshua Bloch",
        "isbnNo":"012345670B",
        "fileSizeKb":2400
      },
      {
        "title":"Java Concurrency in Practice",
        "author":"Brian Goetz",
        "isbnNo":"112345670B",
        "noOfPages":424,
        "weightGrams":620.0
      }
    ]
  }'
```

## Recommendations

```bash
curl "http://localhost:8080/api/v1/catalog/recommendations?q=java&preferredType=EBOOK&limit=5"
```

## Catalog insights

```bash
curl http://localhost:8080/api/v1/catalog/insights
```

## Trace a request

```bash
curl -i http://localhost:8080/api/v1/books \
  -H "X-Request-ID: recruiter-demo-001"
```

The response returns the same `X-Request-ID` header. Error responses also include it in JSON.
