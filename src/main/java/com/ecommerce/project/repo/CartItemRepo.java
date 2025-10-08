package com.ecommerce.project.repo;

import com.ecommerce.project.model.CartItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepo extends JpaRepository<CartItems, Long> {
    @Query("SELECT ci from CartItems ci WHERE ci.cart.id=?2 AND ci.product.id=?1")
    CartItems findCartItemsByProductIdAndCartId(Long productId, Long cartId);
}
