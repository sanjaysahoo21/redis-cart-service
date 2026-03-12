# 🛒 Redis Cart Service

A **production-ready, session-based shopping cart microservice** built with **Spring Boot 3** and **Redis**. Implements distributed caching for session management, uses Redis Hash data structures for efficient cart storage, and provides a resilient REST API with health monitoring.

---

## 📐 Architecture Overview

```
┌────────────────────────────────────────────────────────┐
│                   Docker Compose Network               │
│                                                        │
│  ┌──────────────────────┐    ┌───────────────────────┐ │
│  │   Spring Boot App    │    │     Redis 7-alpine    │ │
│  │   (Port 8080)        │◄──►│     (Port 6379)       │ │
│  │                      │    │                       │ │
│  │  ┌────────────────┐  │    │  cart:session-123 {   │ │
│  │  │  CartController│  │    │    prod-1: {...}       │ │
│  │  └───────┬────────┘  │    │    prod-2: {...}       │ │
│  │          │           │    │  }  TTL: 1800s         │ │
│  │  ┌───────▼────────┐  │    └───────────────────────┘ │
│  │  │  CartService   │  │                              │
│  │  └───────┬────────┘  │                              │
│  │          │           │                              │
│  │  ┌───────▼────────┐  │                              │
│  │  │ RedisTemplate  │  │                              │
│  │  │ (JSON Serializ)│  │                              │
│  │  └────────────────┘  │                              │
│  └──────────────────────┘                              │
└────────────────────────────────────────────────────────┘
```

### Key Design Decisions

| Decision | Choice | Reason |
|---|---|---|
| Redis Data Structure | **Hash** (`HSET`/`HGETALL`/`HDEL`) | Enables atomic per-item updates without rewriting the whole cart |
| Serialization | **GenericJackson2JsonRedisSerializer** | Human-readable JSON, language-agnostic, production-safe |
| TTL Strategy | **Sliding 30-min expiry** | Reset on every write operation to expire only truly abandoned carts |
| Connection Library | **Lettuce** (default with Spring Data Redis) | Thread-safe, non-blocking, built-in connection pooling |
| Totals Calculation | **On-the-fly** (not stored) | Prevents data inconsistency between stored and actual values |

---

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17+ (for local development without Docker)
- Maven (for local build)

### Run with Docker Compose

```bash
# Clone the repository
cd redis-cart-service

# Build and start all services
docker-compose up --build

# All services healthy within ~3 minutes
# App available at http://localhost:8080
```

### Run Locally (without Docker)

```bash
# Ensure Redis is running locally on port 6379
# Build and run
mvn clean package -DskipTests
java -jar target/redis-cart-service-0.0.1-SNAPSHOT.jar
```

---

## ⚙️ Configuration

Copy `.env.example` to `.env` and adjust as needed:

```bash
cp .env.example .env
```

| Variable | Default | Description |
|---|---|---|
| `SPRING_REDIS_HOST` | `redis` | Redis server hostname |
| `SPRING_REDIS_PORT` | `6379` | Redis server port |

---

## 📡 API Documentation

Base URL: `http://localhost:8080`

### Add Item to Cart
```
POST /api/cart/{sessionId}/items
```
**Request Body:**
```json
{
  "productId": "prod-1",
  "productName": "Widget Pro",
  "price": 29.99,
  "quantity": 2
}
```
**Response: 201 Created**
```json
{
  "sessionId": "session-123",
  "items": [
    { "productId": "prod-1", "productName": "Widget Pro", "price": 29.99, "quantity": 2 }
  ],
  "totalAmount": 59.98,
  "itemCount": 1
}
```
> If the `productId` already exists, its quantity is **summed** (not duplicated).

---

### Get Cart
```
GET /api/cart/{sessionId}
```
**Response: 200 OK**
```json
{
  "sessionId": "session-123",
  "items": [...],
  "totalAmount": 89.97,
  "itemCount": 2
}
```
- `itemCount` = number of **unique** products
- `totalAmount` = sum of `price × quantity` for all items

---

### Remove Single Item
```
DELETE /api/cart/{sessionId}/items/{productId}
```
**Response: 200 OK** — returns the updated cart (without the removed item).

---

### Clear Cart
```
DELETE /api/cart/{sessionId}
```
**Response: 204 No Content**

---

### Cache Statistics
```
GET /api/cart/cache-stats
```
**Response: 200 OK**
```json
{
  "totalCarts": 5,
  "hitRate": 0.83
}
```
- `totalCarts` = number of active `cart:*` keys in Redis
- `hitRate` = ratio of cache hits to total lookups (`-1` if no requests made yet)

---

### Health Check
```
GET /actuator/health
```
**Response: 200 OK**
```json
{
  "status": "UP",
  "components": {
    "redis": { "status": "UP" },
    ...
  }
}
```

---

## 🔍 Verifying Redis Data

After adding items, connect to Redis directly:
```bash
# Connect to Redis CLI via Docker
docker exec -it redis-cart-service-redis-1 redis-cli

# View cart hash
HGETALL cart:session-123

# Check TTL (should be ~1800 seconds)
TTL cart:session-123

# Count active carts
KEYS cart:*

# Check cart key exists
EXISTS cart:session-123
```

---

## 🧪 Example Test Flow

```bash
# 1. Add item to cart
curl -s -X POST http://localhost:8080/api/cart/session-123/items \
  -H "Content-Type: application/json" \
  -d '{"productId":"prod-1","productName":"Widget","price":10.00,"quantity":2}' | jq

# 2. Add same item again (quantity should be summed → 5)
curl -s -X POST http://localhost:8080/api/cart/session-123/items \
  -H "Content-Type: application/json" \
  -d '{"productId":"prod-1","productName":"Widget","price":10.00,"quantity":3}' | jq

# 3. Get cart
curl -s http://localhost:8080/api/cart/session-123 | jq

# 4. Remove item
curl -s -X DELETE http://localhost:8080/api/cart/session-123/items/prod-1 | jq

# 5. Clear entire cart
curl -s -X DELETE http://localhost:8080/api/cart/session-123

# 6. Cache stats
curl -s http://localhost:8080/api/cart/cache-stats | jq
```

---

## 📊 Monitoring

Available Actuator endpoints:
- `GET /actuator/health` — Service and Redis health
- `GET /actuator/metrics` — All available metrics
- `GET /actuator/metrics/cart.cache.hits` — Cache hit count
- `GET /actuator/metrics/cart.cache.misses` — Cache miss count

---

## 🛡️ Error Handling

| Scenario | HTTP Status | Description |
|---|---|---|
| Redis unavailable | `503 Service Unavailable` | Graceful degradation |
| Invalid request body | `400 Bad Request` | Validation error details |
| Unexpected error | `500 Internal Server Error` | Generic fallback |

---

## 📁 Project Structure

```
redis-cart-service/
├── src/
│   └── main/
│       ├── java/com/example/rediscartservice/
│       │   ├── RedisCartServiceApplication.java   # Entry point
│       │   ├── config/
│       │   │   └── RedisConfig.java               # Redis + Cache config
│       │   ├── controller/
│       │   │   └── CartController.java            # REST endpoints
│       │   ├── dto/
│       │   │   ├── AddItemRequest.java            # Request DTO
│       │   │   ├── CacheStatsResponse.java        # Stats response DTO
│       │   │   └── ErrorResponse.java             # Error response DTO
│       │   ├── exception/
│       │   │   └── GlobalExceptionHandler.java    # @ControllerAdvice
│       │   ├── model/
│       │   │   ├── Cart.java                      # Cart POJO
│       │   │   └── CartItem.java                  # CartItem POJO
│       │   └── service/
│       │       └── CartService.java               # Business logic
│       └── resources/
│           └── application.yml                    # Configuration
├── Dockerfile                                     # Container image
├── docker-compose.yml                             # Service orchestration
├── .env.example                                   # Environment variable docs
├── pom.xml                                        # Maven build config
└── README.md                                      # This file
```

---

## 🧠 Technical Notes

- **Thread Safety**: `RedisTemplate` is thread-safe. Individual hash operations (`HSET`, `HDEL`) are atomic at the Redis level.
- **Abandoned Cart Cleanup**: Redis TTL automatically removes carts after 30 minutes of inactivity — no manual cleanup needed.
- **JSON Serialization**: Values in Redis are stored as JSON strings, making them inspectable and language-agnostic.
- **12-Factor Compliance**: All configuration is externalized via environment variables.
