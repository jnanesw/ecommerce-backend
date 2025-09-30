package com.ecommerce.project.repo;

import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.ProductDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {
    Page<Product> findByCategoryOrderBySpecialPriceAsc(Category category, Pageable pageDetails);
    List<Product> findByProductNameContainingIgnoreCase(String keyword);
}
