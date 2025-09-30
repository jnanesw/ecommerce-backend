package com.ecommerce.project.service;

import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.payload.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ProductService {

    ProductResponse getAllProducts(int pageSize, int pageNumber, String sortBy, String sortOrder);
    ProductDTO addProduct(ProductDTO productDTO, Long categoryId);
    ProductResponse getProductsByCategory(Long categoryId, int pageSize, int pageNumber, String sortBy, String sortOrder);

    ProductResponse getProductsByKeyword(String keyword, int pageSize, int pageNumber, String sortBy, String sortOrder);
    ProductDTO updateProduct(Long productId, ProductDTO productDTO);

    ProductDTO deleteProduct(Long productId);

    ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException;
}
