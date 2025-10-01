package com.ecommerce.project.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @NotBlank
    @Size(min = 3, message = "Product name must contain at least 3 characters")
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "image")
    private String image;

    @NotBlank
    @Size(min = 6, message = "Product description must contain at least 6 characters")
    @Column(name = "description", nullable = false)
    private String description;

    @Min(value = 0, message = "Quantity cannot be negative")
    @Column(name = "quantity")
    private Integer quantity;

    @Min(value = 0, message = "Price cannot be negative")
    @Column(name = "price", nullable = false)
    private double price;

    @Min(value = 0, message = "Discount cannot be negative")
    @Column(name = "discount")
    private double discount;

    @Min(value = 0, message = "Special price cannot be negative")
    @Column(name = "special_price")
    private double specialPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", foreignKey = @ForeignKey(name = "FK_PRODUCT_USER"))
    private User user;

    public Product(String productName, String description, Integer quantity, double price, double discount, Category category, User user) {
        this.productName = productName;
        this.description = description;
        this.quantity = quantity;
        this.price = price;
        this.discount = discount;
        this.specialPrice = price - (price * discount / 100);
        this.category = category;
        this.user = user;
    }
}
