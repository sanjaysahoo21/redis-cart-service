package com.example.rediscartservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AddItemRequest {

    @NotBlank(message = "productId must not be blank")
    private String productId;

    @NotBlank(message = "productName must not be blank")
    private String productName;

    @NotNull(message = "price must not be null")
    @DecimalMin(value = "0.01", message = "price must be greater than 0")
    private BigDecimal price;

    @Min(value = 1, message = "quantity must be > 0")
    private int quantity;

    public AddItemRequest() {}

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
