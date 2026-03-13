# Redis Cart Service - YouTube Presentation Guide

## 1. Video Goal
This video explains the Redis Cart Service project, the vulnerability and hardcoded-value analysis, the fixes implemented, and a live demo of running and testing the service.

Title suggestion:
`Spring Boot + Redis Cart Service: Security Hardening, Vulnerability Fixes, and Live Demo`

## 2. Recommended Video Length
Target: 8 to 12 minutes.

Suggested timeline:
1. 00:00 - 00:45: Introduction and problem statement
2. 00:45 - 02:00: Project architecture overview
3. 02:00 - 05:30: Security and hardcoded-value findings + fixes
4. 05:30 - 09:00: Live run and API testing demo
5. 09:00 - 10:00: Results, lessons learned, and conclusion

## 3. Pre-Recording Checklist
1. Open these files in your editor tabs:
`docker-compose.yml`, `src/main/resources/application.yml`, `src/main/java/com/example/rediscartservice/config/RedisConfig.java`, `src/main/java/com/example/rediscartservice/service/CartService.java`, `src/main/java/com/example/rediscartservice/controller/CartController.java`, `src/main/java/com/example/rediscartservice/config/SecurityConfig.java`.
2. Keep one terminal ready at project root.
3. Ensure Docker is running.
4. Optional: set env variable before demo:
```bash
export REDIS_PASSWORD="yourStrongPassword123"
```

## 4. Presentation Script (Ready to Speak)

### 4.1 Intro (00:00 - 00:45)
"Hello everyone. In this video, I will explain my Redis Cart Service project built with Spring Boot and Redis. This is a session-based shopping cart microservice. I will show the architecture, then walk through vulnerability and hardcoded-value checks, then show the fixes I implemented, and finally run and test the application live."

### 4.2 Architecture (00:45 - 02:00)
"At a high level, the service exposes REST APIs under `/api/cart`. Cart data is stored in Redis using a hash structure with key pattern `cart:{sessionId}`. The app runs in one container, Redis runs in another container, and both are managed through Docker Compose. The service supports add item, get cart, remove item, clear cart, and cache stats APIs."

"The design uses Redis hash commands for item-level updates and a TTL strategy to auto-expire abandoned carts."

### 4.3 Findings and Fixes (02:00 - 05:30)
"I analyzed the code for vulnerabilities and hardcoded values. Here are the key issues and what I fixed."

Issue 1: Insecure deserialization risk.
"In Redis serialization config, permissive polymorphic typing can be dangerous. I replaced permissive subtype handling with a restricted validator and safer type boundaries in `RedisConfig.java`."

Issue 2: Redis exposure and missing password.
"Redis was previously easy to access. I hardened `docker-compose.yml` by adding password protection, limiting bind address to localhost, and adding a persistent volume."

Issue 3: KEYS command performance risk.
"The cache stats implementation used `keys`, which can block Redis in production. I replaced that with cursor-based scan logic in `CartService.java`."

Issue 4: Input validation gaps.
"I added strict regex validation for `sessionId` and `productId` in `CartController.java` to prevent malformed identifiers."

Issue 5: Money precision issue.
"I changed monetary fields from `double` to `BigDecimal` in model and request classes to avoid floating-point precision errors."

Issue 6: Hardcoded operational values.
"I externalized values such as cart TTL and Redis password into configuration, so behavior can be changed via environment variables without code edits."

Issue 7: Logging and actuator hardening.
"I reduced overly verbose runtime output and tightened actuator exposure in `application.yml` for safer defaults."

Security note you can mention honestly:
"In this setup, endpoint authentication is intentionally open in `SecurityConfig` and expected to be enforced upstream, for example by an API gateway or JWT-based edge service."

### 4.4 Live Demo (05:30 - 09:00)
"Now I will run the project and test APIs."

Use these commands on screen:

```bash
# From project root
docker compose down -v
docker compose up --build
```

Then test APIs in another terminal:

```bash
# Add item
curl -s -X POST http://localhost:8080/api/cart/session-123/items \
  -H "Content-Type: application/json" \
  -d '{"productId":"prod-1","productName":"Widget","price":10.50,"quantity":2}'

# Get cart
curl -s http://localhost:8080/api/cart/session-123

# Add same item again (quantity should increase)
curl -s -X POST http://localhost:8080/api/cart/session-123/items \
  -H "Content-Type: application/json" \
  -d '{"productId":"prod-1","productName":"Widget","price":10.50,"quantity":3}'

# Remove item
curl -s -X DELETE http://localhost:8080/api/cart/session-123/items/prod-1

# Cache stats
curl -s http://localhost:8080/api/cart/cache-stats

# Health check
curl -s http://localhost:8080/actuator/health
```

Optional Redis verification (inside container):
```bash
docker exec -it redis-cart-service-redis-1 redis-cli -a "$REDIS_PASSWORD"
SCAN 0 MATCH cart:* COUNT 100
TTL cart:session-123
```

What to narrate during demo:
1. Show service startup and health status.
2. Show cart creation and quantity aggregation behavior.
3. Show delete and clear operations.
4. Show cache stats endpoint result.
5. Show that key TTL exists and cart auto-expiry is configured.

### 4.5 Conclusion (09:00 - 10:00)
"To conclude, this project demonstrates a production-style Redis-backed cart service with practical hardening improvements. I addressed security, configuration hygiene, and reliability concerns, then validated behavior through live tests. Future improvements can include full JWT authentication, role-based endpoint authorization, and integration tests with test containers. Thank you for watching."

## 5. On-Screen Slide Content (Quick Copy)
Use these headings for your slides:
1. Problem and Objective
2. Architecture
3. Initial Risks Found
4. Security and Configuration Fixes
5. Live API Demo
6. Test Results
7. Lessons Learned and Next Steps

## 6. Speaking Tips for YouTube
1. Keep each sentence short and clear.
2. While coding/demo is on screen, narrate only intent and result.
3. Use zoom/highlight for critical lines in `RedisConfig.java`, `CartService.java`, and `docker-compose.yml`.
4. End with 2 clear takeaways:
"secure defaults matter" and "externalized configuration improves maintainability."

## 7. Description Template for YouTube
`In this video, I explain a Spring Boot + Redis cart microservice, perform vulnerability and hardcoded-value review, apply security and reliability fixes, and run live API tests. Topics include Redis hardening, safer serialization, SCAN over KEYS, BigDecimal for money, validation improvements, and operational configuration best practices.`
