package com.ecommerce.project.service;

import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.CartItemDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public interface CartService {
    CartDTO addProductTOCart(Long productId, Integer quantity);

    List<CartDTO> getAllCarts();

    CartDTO getCart();

    CartDTO updateProductQuantityInCart(Long productId, int quantity);

    String deleteProductFromCart(Long cartId, Long productId);

    void updateProductInCarts(Long cartId, Long productId);

    @Transactional
    String createOrUpdateCartWithItems(List<CartItemDTO> cartItems);
}
