package com.example.rediscartservice.dto;

public class CacheStatsResponse {

    private long totalCarts;
    private double hitRate;

    public CacheStatsResponse() {}

    public CacheStatsResponse(long totalCarts, double hitRate) {
        this.totalCarts = totalCarts;
        this.hitRate = hitRate;
    }

    public long getTotalCarts() { return totalCarts; }
    public void setTotalCarts(long totalCarts) { this.totalCarts = totalCarts; }

    public double getHitRate() { return hitRate; }
    public void setHitRate(double hitRate) { this.hitRate = hitRate; }
}
