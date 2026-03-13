package com.example.rediscartservice.controller;

import com.example.rediscartservice.dto.AddItemRequest;
import com.example.rediscartservice.dto.CacheStatsResponse;
import com.example.rediscartservice.model.Cart;
import com.example.rediscartservice.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@Validated
public class CartController {

    private static final String SESSION_ID_PATTERN = "^[a-zA-Z0-9_-]{1,128}$";
    private static final String SESSION_ID_PATH_PATTERN = "[a-zA-Z0-9_-]{1,128}";
    private static final String PRODUCT_ID_PATTERN = "^[a-zA-Z0-9_-]{1,128}$";

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * GET /api/cart/cache-stats
     * Returns cache statistics: total active carts and hit rate.
     */
    @GetMapping("/cache-stats")
    public ResponseEntity<CacheStatsResponse> getCacheStats() {
        CacheStatsResponse stats = cartService.getCacheStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * POST /api/cart/{sessionId}/items
     * Adds or updates an item in the session's cart.
     * Returns 201 Created with the full updated cart.
     */
    @PostMapping("/{sessionId:" + SESSION_ID_PATH_PATTERN + "}/items")
    public ResponseEntity<Cart> addItem(
            @PathVariable @Pattern(regexp = SESSION_ID_PATTERN, message = "Invalid session ID format") String sessionId,
            @Valid @RequestBody AddItemRequest request) {

        Cart updatedCart = cartService.addItem(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(updatedCart);
    }

    /**
     * GET /api/cart/{sessionId}
     * Retrieves the current state of a cart with calculated totals.
     * Returns 200 OK.
     */
    @GetMapping("/{sessionId:" + SESSION_ID_PATH_PATTERN + "}")
    public ResponseEntity<Cart> getCart(
            @PathVariable @Pattern(regexp = SESSION_ID_PATTERN, message = "Invalid session ID format") String sessionId) {
        Cart cart = cartService.getCart(sessionId);
        return ResponseEntity.ok(cart);
    }

    /**
     * DELETE /api/cart/{sessionId}/items/{productId}
     * Removes a single item from the cart.
     * Returns 200 OK with the updated cart.
     */
    @DeleteMapping("/{sessionId:" + SESSION_ID_PATH_PATTERN + "}/items/{productId}")
    public ResponseEntity<Cart> removeItem(
            @PathVariable @Pattern(regexp = SESSION_ID_PATTERN, message = "Invalid session ID format") String sessionId,
            @PathVariable @Pattern(regexp = PRODUCT_ID_PATTERN, message = "Invalid product ID format") String productId) {

        Cart updatedCart = cartService.removeItem(sessionId, productId);
        return ResponseEntity.ok(updatedCart);
    }

    /**
     * DELETE /api/cart/{sessionId}
     * Clears all items from the cart (deletes the Redis key).
     * Returns 204 No Content.
     */
    @DeleteMapping("/{sessionId:" + SESSION_ID_PATH_PATTERN + "}")
    public ResponseEntity<Void> clearCart(
            @PathVariable @Pattern(regexp = SESSION_ID_PATTERN, message = "Invalid session ID format") String sessionId) {
        cartService.clearCart(sessionId);
        return ResponseEntity.noContent().build();
    }
}
