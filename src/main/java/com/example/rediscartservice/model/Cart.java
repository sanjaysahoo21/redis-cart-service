package com.example.rediscartservice.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class Cart implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;
    private List<CartItem> items;
    private BigDecimal totalAmount;
    private int itemCount;

    public Cart() {}

    public Cart(String sessionId, List<CartItem> items) {
        this.sessionId = sessionId;
        this.items = items;
        // Calculate derived fields
        this.itemCount = items != null ? items.size() : 0;
        this.totalAmount = items != null
                ? items.stream()
                    .filter(i -> i.getPrice() != null)
                    .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }
}
