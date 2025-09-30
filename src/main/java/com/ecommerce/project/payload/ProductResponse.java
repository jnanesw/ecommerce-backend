package com.ecommerce.project.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private int pageSize;
    private int pageNumber;
    private Long totalElements;
    private int totalPages;
    private boolean lastPage;

    List<ProductDTO> content;
}
