package com.ecommerce.project.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
    private Long productId;
    private String ProductName;
    private String description;
    private Integer quantity;
    private double price;
    private double specialPrice;
    private String image;
}
