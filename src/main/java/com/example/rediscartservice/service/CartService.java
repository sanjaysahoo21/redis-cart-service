package com.example.rediscartservice.service;

import com.example.rediscartservice.dto.AddItemRequest;
import com.example.rediscartservice.dto.CacheStatsResponse;
import com.example.rediscartservice.model.Cart;
import com.example.rediscartservice.model.CartItem;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);
    private static final String CART_KEY_PREFIX = "cart:";

    @Value("${cart.ttl-minutes:30}")
    private long cartTtlMinutes;

    private final RedisTemplate<String, Object> redisTemplate;
    private final MeterRegistry meterRegistry;

    // In-memory counters for hit/miss tracking via Micrometer
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    public CartService(RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;

        // Register Micrometer gauges for hit/miss counts
        meterRegistry.gauge("cart.cache.hits", cacheHits, AtomicLong::doubleValue);
        meterRegistry.gauge("cart.cache.misses", cacheMisses, AtomicLong::doubleValue);
    }

    /**
     * Builds the Redis hash key for a given session ID.
     */
    private String cartKey(String sessionId) {
        return CART_KEY_PREFIX + sessionId;
    }

    /**
     * Resets the TTL of a cart to 30 minutes (sliding expiration).
     */
    private void resetTtl(String sessionId) {
        redisTemplate.expire(cartKey(sessionId), cartTtlMinutes, TimeUnit.MINUTES);
    }

    /**
     * Adds or updates an item in the cart.
     * If the productId already exists, quantity is summed.
     */
    public Cart addItem(String sessionId, AddItemRequest request) {
        String key = cartKey(sessionId);

        // Check if item already exists in hash
        Object existing = redisTemplate.opsForHash().get(key, request.getProductId());

        CartItem item;
        if (existing instanceof CartItem existingItem) {
            // Update quantity by summing
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            item = existingItem;
            logger.info("Updated item {} in cart {}", request.getProductId(), sessionId);
        } else {
            // New item
            item = new CartItem(
                    request.getProductId(),
                    request.getProductName(),
                    request.getPrice(),
                    request.getQuantity()
            );
            logger.info("Added new item {} to cart {}", request.getProductId(), sessionId);
        }

        // HSET: store/update item in hash
        redisTemplate.opsForHash().put(key, request.getProductId(), item);

        // Reset TTL (sliding expiration)
        resetTtl(sessionId);

        return getCart(sessionId);
    }

    /**
     * Retrieves the current state of a cart, calculating totals dynamically.
     */
    public Cart getCart(String sessionId) {
        String key = cartKey(sessionId);

        // HGETALL: get all items from hash
        Map<Object, Object> rawEntries = redisTemplate.opsForHash().entries(key);

        if (rawEntries == null || rawEntries.isEmpty()) {
            cacheMisses.incrementAndGet();
            // Return empty cart rather than null
            return new Cart(sessionId, new ArrayList<>());
        }

        cacheHits.incrementAndGet();

        List<CartItem> items = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : rawEntries.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof CartItem cartItem) {
                items.add(cartItem);
            } else {
                logger.warn("Unexpected value type in cart hash: {}", value.getClass());
            }
        }

        return new Cart(sessionId, items);
    }

    /**
     * Removes a single item from the cart.
     */
    public Cart removeItem(String sessionId, String productId) {
        String key = cartKey(sessionId);

        // HDEL: remove specific field from hash
        Long deleted = redisTemplate.opsForHash().delete(key, productId);
        if (deleted != null && deleted > 0) {
            logger.info("Removed item {} from cart {}", productId, sessionId);
            resetTtl(sessionId);
        } else {
            logger.warn("Item {} not found in cart {}", productId, sessionId);
        }

        return getCart(sessionId);
    }

    /**
     * Clears the entire cart (deletes the Redis key).
     */
    public void clearCart(String sessionId) {
        String key = cartKey(sessionId);
        // DEL: delete the entire hash key
        redisTemplate.delete(key);
        logger.info("Cleared cart {}", sessionId);
    }

    /**
     * Returns cache statistics: total active carts and hit rate.
     */
    public CacheStatsResponse getCacheStats() {
        long totalCarts = 0;
        try (
            Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(CART_KEY_PREFIX + "*").count(100).build()
            )
        ) {
            while (cursor.hasNext()) {
                cursor.next();
                totalCarts++;
            }
        } catch (Exception e) {
            logger.error("Error counting cart keys: {}", e.getMessage());
        }

        // Calculate hit rate from our atomic counters
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;

        double hitRate = total > 0 ? (double) hits / total : -1.0;

        logger.debug("Cache stats - totalCarts: {}, hitRate: {}", totalCarts, hitRate);
        return new CacheStatsResponse(totalCarts, hitRate);
    }
}
