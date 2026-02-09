# Loyalty Points Quote Service

A Vert.x–based HTTP service that calculates loyalty points for flight booking requests.
The service applies FX conversion, tier bonuses, promotional rules, validation, and caps total points according to defined business rules.

---

## 🛠 Tech Stack

- Java 21
- Vert.x 4.x
- Maven
- JUnit 5
- WireMock
- AssertJ
- JaCoCo (code coverage)

> **Note:** Java 21 is used to ensure compatibility with JaCoCo coverage tooling.

---

## 🚀 API Endpoint

### POST `/v1/points/quote`

#### Request
```json
{
  "fareAmount": 1234.50,
  "currency": "USD",
  "cabinClass": "ECONOMY",
  "customerTier": "SILVER",
  "promoCode": "SUMMER25"
}
```

#### Response
```json
{
  "basePoints": 1234,
  "tierBonus": 185,
  "promoBonus": 308,
  "totalPoints": 1727,
  "effectiveFxRate": 3.67,
  "warnings": ["PROMO_EXPIRES_SOON"]
}
```

---

## 📐 Business Rules Implemented

- Base points are calculated after FX conversion
- Tier multipliers:
  - NONE → 0%
  - SILVER → 15%
  - GOLD → 30%
  - PLATINUM → 50%
- Promotional bonuses are fetched from an external promo service
- Warnings are returned for near-expiry promotions
- Total points are capped at **50,000**
- Validation rejects:
  - Fare amount ≤ 0
  - Invalid or missing currency
  - Invalid cabin class
- Resilience handling for FX and Promo services

---

## 🧪 Testing Strategy

- Component-level tests using Vert.x Test Framework
- Application started on a **random port** per test execution
- External FX and Promo services stubbed using **WireMock**
- Data-driven tests using JSON test cases
- Assertions on HTTP status codes and response bodies
- Asynchronous behavior validated using VertxTestContext

---

## 📊 Test Coverage

- **Line coverage:** ~86%
- **Branch coverage:** ~54%

Coverage focuses on:
- Core business logic
- Validation paths
- Edge cases such as point caps and promo scenarios

Coverage reports are generated using **JaCoCo**.

---

## ▶️ Running the Project

### Run tests (via Maven)
```bash
mvn clean test
```

> If `mvn` is not available in the local shell, tests can also be executed via the Maven lifecycle inside IntelliJ IDEA.

### View coverage report
```text
target/site/jacoco/index.html
```

Open the file in a browser after running tests.

---

## 📁 Project Structure

```text
src/
 ├── main/
 │   └── java/
 │       ├── com/airline/loyalty/points/api/        # HTTP layer (Vert.x routes)
 │       ├── com/airline/loyalty/points/service/    # Business logic
 │       ├── com/airline/loyalty/points/validation/ # Request validation
 │       └── com/airline/loyalty/points/model/      # DTOs
 └── test/
     ├── java/
     │   └── com/airline/loyalty/component/         #Tests
           └── com/airline/loyalty/models/          #DTOs
           └── com/airline/loyalty/util/            #utilities
     └── resources/
         └── data/                                  # JSON test data
```

---

## 📝 Design Notes

- Component tests are preferred over isolated unit tests to validate real HTTP flows and asynchronous behavior.
- External dependencies are fully stubbed to ensure deterministic and fast test execution.
- Java 21 is intentionally used due to current JaCoCo compatibility constraints with newer Java versions.

---

## ✅ Summary

This service demonstrates:
- Clean Vert.x architecture
- Robust business rule implementation
- Realistic component testing strategy
- Data-driven test design
- Meaningful code coverage

The project is intended as a concise, production-style backend service implementation.
