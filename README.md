# Aviation API

A Spring Boot microservice that fetches airport details by ICAO code from public aviation data APIs.

## Tech Stack

- Java 21, Spring Boot 4
- Resilience4j (retry + circuit breaker)
- Caffeine (in-memory caching)
- Spring Boot Actuator (health, metrics)

## Setup & Run

### Prerequisites

- Java 21+
- Maven 3.9+

### Build

```bash
mvn clean package
```

### Run

```bash
mvn spring-boot:run
```

The service starts on `http://localhost:8080`.

### Configuration

Key properties in `application.yaml`:

| Property | Default | Description |
|---|---|---|
| `aviation.api-key` | _(blank — auth disabled)_ | Set a value to require `X-API-Key` header on requests |
| `resilience4j.retry.instances.airportLookup.max-attempts` | `3` | Max retry attempts per provider call |
| `resilience4j.circuitbreaker.instances.airportLookup.failure-rate-threshold` | `50` | Failure % to open circuit |

## Testing

### Unit Tests

```bash
mvn test
```

### Manual Testing (Postman)

A Postman collection is included at:

```
postman/aviation-api.postman_collection.json
```

Import it into Postman via File → Import. The collection includes requests for the airport lookup endpoint (happy path and 404), plus actuator endpoints.

An OpenAPI specification is available at:

```
openapi/openapi.yaml
```

### Manual Testing (curl)

```bash
# Successful lookup
curl http://localhost:8080/api/airports/KJFK

# Not found
curl http://localhost:8080/api/airports/XXXX

# Health check
curl http://localhost:8080/actuator/health
```

## API

### Get Airport

```
GET /api/airports/{icaoCode}
```

Response:
```json
{
  "icaoCode": "KJFK",
  "iataCode": "JFK",
  "name": "NEW YORK/JOHN F KENNEDY INTL",
  "country": "US",
  "state": "NY",
  "latitude": 40.6399,
  "longitude": -73.7787,
  "elevationFt": 4,
  "source": "AviationWeather.gov"
}
```

### Error Responses

| Status | Reason |
|---|---|
| `404` | Airport not found for the given ICAO code |
| `401` | Missing or invalid API key (when auth is enabled) |
| `503` | All upstream providers unavailable |

Errors follow the [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) Problem Detail format.

### Actuator Endpoints

```
GET /actuator/health
GET /actuator/metrics
GET /actuator/caches
```

## Architecture & Design Decisions

```
controller → service → provider(s)
                ↕            ↕
             cache     external APIs
```

### Layering

- **Controller** — thin REST layer, delegates to the service. No business logic.
- **Service** — orchestrates the provider chain. Owns caching, retry, and circuit breaker concerns.
- **Provider** — each provider implements the `AirportDataProvider` interface. Providers are `@Order`-ed and tried sequentially, making the system decoupled from any single upstream source.
  - `AviationWeatherProvider` (`@Order(1)`) — primary, queries [aviationweather.gov](https://aviationweather.gov/data/api/).
  - `AirportsApiProvider` (`@Order(2)`) — fallback, queries [airportsapi.com](https://airportsapi.com).

### Design Patterns

- **Chain of Responsibility** — providers are ordered via `@Order` and tried sequentially. If one fails or returns empty, the service falls through to the next. No provider knows about the others.
- **Strategy** — each provider is an interchangeable implementation of `AirportDataProvider`. The service doesn't care which one succeeds.
- **Proxy/Decorator** — Resilience4j wraps the service method with retry and circuit breaker behaviour via AOP, without modifying the service code.
- **Filter** — `ApiKeyFilter` implements the servlet filter pattern for cross-cutting authentication.

### Extensibility

The provider interface ensures the service is not tightly coupled to one data source. Adding a new provider is a single class: implement `AirportDataProvider`, annotate with `@Component @Order(n)`, and the service picks it up automatically — no changes to existing code.

### Resilience & Error Handling

- **Timeouts:** RestClient is configured with 5s connect and 10s read timeouts to avoid hanging on slow upstreams.
- **Retry:** Resilience4j retries failed provider calls up to 3 times with a 500ms wait between attempts.
- **Circuit breaker:** Opens after 50% failure rate over a sliding window of 10 calls. Stays open for 30s before half-opening. Prevents cascading failures when an upstream is down.
- **Provider fallback:** If the primary provider fails or returns no result, the service falls through to the next provider in the chain.
- **Error responses:** All errors are returned as RFC 9457 Problem Detail JSON via a global exception handler — consistent, machine-readable error format.

### Caching

- Caffeine in-memory cache (500 entries, 30-minute TTL). Airport data is relatively static, so aggressive caching reduces upstream calls significantly.
- Cache is per-instance. For multi-instance deployments, a shared cache (e.g. Redis) would avoid redundant upstream calls across replicas.

### Authentication

- Optional API key filter. When `aviation.api-key` is set, all `/api/**` requests require a matching `X-API-Key` header. Actuator endpoints are excluded.
- When the property is blank (default), authentication is disabled entirely.

### Assumptions

- Only ICAO code lookup is in scope — no search, no bulk queries.
- The upstream API (aviationweather.gov) is publicly accessible but may be unstable, hence the resilience patterns.
- No persistent storage is needed — the service is a stateless proxy with caching.
- No user management or role-based access — a single shared API key is sufficient for this scope.
- No frontend is required.

## Deployment, Scaling & Monitoring

### Deployment

The application is packaged as a standalone JAR via `mvn package`. It can be containerised with a minimal Dockerfile:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
COPY target/aviation-api-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

This image can be deployed to any container orchestrator (Kubernetes, ECS) or a PaaS like AWS App Runner / Google Cloud Run.

### Scaling

- The service is **stateless** — no session or server-side state — so it scales horizontally by adding instances behind a load balancer.
- **Caffeine cache is per-instance.** Under high load with multiple replicas, switching to a shared cache (Redis) avoids redundant upstream calls and ensures consistent cache hits across instances.
- Resilience4j circuit breaker state is also per-instance, which is acceptable — each instance independently protects itself from upstream failures.

### Monitoring

- **Health checks:** `GET /actuator/health` is ready for load balancer and Kubernetes liveness/readiness probes.
- **Metrics:** `GET /actuator/metrics` exposes JVM, HTTP, cache, and Resilience4j metrics. These can be scraped by Prometheus via the `micrometer-registry-prometheus` dependency.
- **Logging:** Structured SLF4J logging throughout. In production, configure JSON log format and ship to a centralised system (ELK, CloudWatch, Datadog).
- **Alerting:** Key metrics to alert on include circuit breaker state changes, high retry rates, and cache eviction spikes.

### AI Usage
**Tools**: Kiro-CLI

**Cases**: Documentation, Increasing test coverage
