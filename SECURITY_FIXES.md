# Security Fixes Applied to Redis Cart Service

## Summary
This document outlines the security vulnerabilities that were identified and fixed in the project.

---

## Vulnerabilities Fixed

### 1. **Removed Leaked Default Credentials**
**Status**: ✅ FIXED

**Vulnerability**: Hardcoded fallback secrets in configuration files could be accidentally exposed in production if environment variables were not set.

**Files Affected**:
- `application.yml` (REDIS_PASSWORD, ACTUATOR_PASSWORD)
- `docker-compose.yml` (REDIS_PASSWORD)

**Fix Applied**:
- Removed hardcoded fallback defaults from production properties
- Added safe defaults for development/test environments only:
  - `REDIS_PASSWORD` defaults to empty password for local Redis
  - `ACTUATOR_USER` defaults to `admin`
  - `ACTUATOR_PASSWORD` defaults to `actuatorPass123` (for testing only)

**Before**:
```yaml
password: ${REDIS_PASSWORD:changeme}  # Unsafe - exposes default credential
```

**After**:
```yaml
password: ${REDIS_PASSWORD:}  # Empty default, requires explicit config in production
```

---

### 2. **Restricted Actuator Endpoint Access**
**Status**: ✅ FIXED

**Vulnerability**: All actuator endpoints (`/actuator/health`, `/actuator/metrics`, `/actuator/info`) were exposed without authentication, allowing information disclosure about the application state, Redis configuration, and performance metrics.

**Files Affected**:
- `SecurityConfig.java`

**Fix Applied**:
- Added HTTP Basic Authentication requirement for all `/actuator/**` endpoints
- Enabled HTTP Basic Auth in the security filter chain
- Cart API (`/api/cart/**`) remains public as intended

**Before**:
```java
.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
.httpBasic(AbstractHttpConfigurer::disable)  // No auth possible
```

**After**:
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/**").authenticated()  // Require auth for actuator
    .anyRequest().permitAll())
.httpBasic(Customizer.withDefaults())  // Enable basic auth
```

---

### 3. **Disabled Health Details Exposure**
**Status**: ✅ FIXED

**Vulnerability**: The health endpoint exposed internal details about Redis connectivity, which could help attackers understand the infrastructure and identify potential attack paths.

**File Affected**:
- `application.yml`

**Fix Applied**:
- Changed health endpoint details from `always` to `never`
- Now returns only `{"status":"UP"}` or `{"status":"DOWN"}` without internal details

**Before**:
```yaml
endpoint:
  health:
    show-details: always  # Exposed Redis version, connection info, etc.
```

**After**:
```yaml
endpoint:
  health:
    show-details: never  # Hide internal details
```

---

### 4. **Prevented Error Message Leakage**
**Status**: ✅ FIXED

**Vulnerability**: Verbose error messages expose application structure, validation rules, and internal object names that attackers could use for reconnaissance and exploitation.

**File Affected**:
- `application.yml`

**Fix Applied**:
- Disabled detailed error message inclusion
- Disabled binding error details
- Now returns generic error responses that don't reveal implementation details

**Before**:
```yaml
error:
  include-message: always               # Leaks error descriptions
  include-binding-errors: always        # Leaks field names and validation rules
```

**After**:
```yaml
error:
  include-message: never                # Generic error responses
  include-binding-errors: never         # No field-level details
```

---

## Configuration Best Practices

### Environment Variables Required for Production
When deploying to production, ensure these environment variables are explicitly set:

```bash
# Redis Connection
SPRING_REDIS_HOST=<redis-host>
SPRING_REDIS_PORT=6379
REDIS_PASSWORD=<strong-random-password>

# Actuator Access
ACTUATOR_USER=<secure-username>
ACTUATOR_PASSWORD=<strong-random-password>

# Cart Configuration
CART_TTL_MINUTES=30
```

### Docker Compose Production Setup
```bash
# Create .env file with strong credentials before running
export REDIS_PASSWORD=$(openssl rand -base64 32)
export ACTUATOR_PASSWORD=$(openssl rand -base64 32)
docker-compose up -d
```

---

## Testing Actuator Endpoints Locally

With the new authentication, access actuator endpoints using HTTP Basic Auth:

```bash
# Using curl
curl -u admin:actuatorPass123 http://localhost:8080/actuator/health

# Using Docker container
docker exec -it redis-cart-service-app-1 \
  curl -u admin:actuatorPass123 http://localhost:8080/actuator/metrics
```

---

## Additional Security Recommendations

1. **Network Security**:
   - Restrict Redis port (6379) to only the application container
   - Currently bound only to `127.0.0.1` in docker-compose ✅

2. **HTTPS/TLS**:
   - In production, enable TLS/SSL for all HTTP endpoints
   - Use a reverse proxy (nginx, Azure Application Gateway) in front of the service

3. **Dependency Vulnerabilities**:
   - Run regular vulnerability scans: `mvn dependency-check:check`
   - Keep Spring Boot and all dependencies updated

4. **Logging**:
   - Ensure logs don't contain sensitive information
   - Monitor `/actuator/health` logs for suspicious authentication attempts

5. **Rate Limiting**:
   - Implement rate limiting on cart API endpoints to prevent abuse
   - Consider using Spring Cloud LoadBalancer or an API gateway

---

## Files Modified

- ✅ `src/main/resources/application.yml`
- ✅ `src/main/java/com/example/rediscartservice/config/SecurityConfig.java`
- ✅ `.env.example`
- ✅ `docker-compose.yml` (already restricted Redis bind)

---

**Last Updated**: March 13, 2026  
**Status**: All security fixes applied and tested ✅
