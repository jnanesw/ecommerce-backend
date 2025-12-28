package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.CartItemDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.repo.CartItemRepo;
import com.ecommerce.project.repo.CartRepo;
import com.ecommerce.project.repo.ProductRepo;
import com.ecommerce.project.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartServiceImpl implements CartService{
    private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);

    @Autowired
    private CartRepo cartRepo;

    @Autowired
    private CartItemRepo cartItemRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public CartDTO addProductTOCart(Long productId, Integer quantity) {
        logger.info("Adding product {} with quantity {} to cart", productId, quantity);
        // 1. Find existing cart or Create new One
        Cart cart = createCart();

        // 2. Retrieve product Details
        Product product = productRepo.findById(productId).
                orElseThrow(()->new ResourceNotFoundException("Product", "productId", productId));

        // 3. Perform Validations
        CartItem cartItem = cartItemRepo.findCartItemsByProductIdAndCartId(productId, cart.getCartId());
        if (cartItem != null) {
            throw new APIException("Product " + product.getProductName() + " already exists in the cart");
        }
        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is not available");
        }
        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the " + product.getProductName()
                    + " less than or equal to the quantity " + product.getQuantity() + ".");
        }
        // 4. Create CartItems
        CartItem cartItem1 = new CartItem();
        cartItem1.setCart(cart);
        cartItem1.setProduct(product);
        cartItem1.setQuantity(quantity);
        cartItem1.setDiscount(product.getDiscount());
        cartItem1.setProductPrice(product.getSpecialPrice());
        // 5. Save cartItems
        cartItemRepo.save(cartItem1);

        product.setQuantity(product.getQuantity());

        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
        cartRepo.save(cart);
        // 6. Return Updated cart
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<CartItem> cartItemList = cart.getCartItems();
        List<ProductDTO> productDTOList = cartItemList.stream().map(item ->{
            ProductDTO productDTO = modelMapper.map(item.getProduct(), ProductDTO.class);
            productDTO.setQuantity(item.getQuantity());
            return productDTO;
        }).toList();

        cartDTO.setProducts(productDTOList);
        return cartDTO;
    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepo.findAll();
        if(carts.isEmpty()){
            throw new APIException("No cart Exists!");
        }
        List<CartDTO> cartDTOs = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
            List<ProductDTO> productDTOs = cart.getCartItems().stream().map(cartItem -> {
                ProductDTO productDTO = modelMapper.map(cartItem.getProduct(), ProductDTO.class);
                productDTO.setQuantity(cartItem.getQuantity());
                return  productDTO;
            }).toList();

            cartDTO.setProducts(productDTOs);
            return cartDTO;
        }).toList();

        return cartDTOs;
    }

    @Override
    public CartDTO getCart(){
        String email = authUtil.loggedInEmail();
        Cart cart = cartRepo.findCartByEmail(email);
        Long cartId = cart.getCartId();

        Cart cart2 = cartRepo.findCartByEmailAndCartId(email, cartId);
        CartDTO cartDTO = modelMapper.map(cart2, CartDTO.class);
        List<ProductDTO> productDTOs = cart2.getCartItems().stream().map(cartItem -> {
            ProductDTO productDTO = modelMapper.map(cartItem.getProduct(), ProductDTO.class);
            productDTO.setQuantity(cartItem.getQuantity());
            return productDTO;
        }).toList();

        cartDTO.setProducts(productDTOs);

        return cartDTO;
    }

    @Override
    public CartDTO updateProductQuantityInCart(Long productId, int quantity){
        // 1. get cart using mail and cartId. perform the verifications
        // Method-1:
        User user = authUtil.loggedInUser();
        Cart cart = user.getCart();
        if (cart == null) {
            throw new APIException("Cart for the user " + user.getUserName() + " is not available");
        }

        Long cartId = cart.getCartId();

        Product product = productRepo.findById(productId).orElseThrow(()->
                new ResourceNotFoundException("Product", "productId", productId));
        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is not available");
        }

        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the " + product.getProductName()
                    + " less than or equal to the quantity " + product.getQuantity() + ".");
        }

        CartItem cartItem = cartItemRepo.findCartItemsByProductIdAndCartId(productId, cartId);
        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " not available in the cart!!!");
        }

        // Calculate new quantity
        int newQuantity = cartItem.getQuantity() + quantity;

        // Validation to prevent negative quantities
        if (newQuantity < 0) {
            throw new APIException("The resulting quantity cannot be negative.");
        }

        if (newQuantity == 0){
            deleteProductFromCart(cartId, productId);
        }else{
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setDiscount(product.getDiscount());
            cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice()*quantity));
            cartRepo.save(cart);
        }

        CartItem cartItem1 = cartItemRepo.save(cartItem);
        if(cartItem1.getQuantity() == 0){
            cartItemRepo.deleteById(cartItem1.getCartItemId());
        }

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        List<CartItem> cartItems = cart.getCartItems();
        List<ProductDTO> productDTOs = cartItems.stream().map(cartItem3 -> {
            ProductDTO productDTO = modelMapper.map(cartItem3.getProduct(), ProductDTO.class);
            productDTO.setQuantity(cartItem3.getQuantity());
            return productDTO;
        }).toList();

        cartDTO.setProducts(productDTOs);

        return cartDTO;
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepo.findById(cartId).orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));
        Product product = productRepo.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepo.findCartItemsByProductIdAndCartId(productId, cartId);
        if(cartItem == null){
            throw new ResourceNotFoundException("Product", "productId", productId);
        }

        cart.setTotalPrice(cart.getTotalPrice() - (cartItem.getProductPrice()*cartItem.getQuantity()));
        cartRepo.save(cart);

        cartItemRepo.deleteCartItemByProductIdAndCartId(cartId, productId);

        return "Product " + cartItem.getProduct().getProductName() + " removed successfully!";
    }


    private Cart createCart(){
        Cart userCart = cartRepo.findCartByEmail(authUtil.loggedInEmail());
        if(userCart != null) return  userCart;

        Cart newCart = new Cart();
        newCart.setUser(authUtil.loggedInUser());
        newCart.setTotalPrice(0.0);

        return cartRepo.save(newCart);
    }

    @Override
    public void updateProductInCarts(Long cartId, Long productId) {
        Cart cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepo.findCartItemsByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " not available in the cart!!!");
        }

        double cartPrice = cart.getTotalPrice()
                - (cartItem.getProductPrice() * cartItem.getQuantity());

        cartItem.setProductPrice(product.getSpecialPrice());

        cart.setTotalPrice(cartPrice
                + (cartItem.getProductPrice() * cartItem.getQuantity()));

        cartItem = cartItemRepo.save(cartItem);
    }

    @Transactional
    @Override
    public String createOrUpdateCartWithItems(List<CartItemDTO> cartItems) {
        // Get user's email
        String emailId = authUtil.loggedInEmail();

        // Check if an existing cart is available or create a new one
        Cart existingCart = cartRepo.findCartByEmail(emailId);
        if (existingCart == null) {
            existingCart = new Cart();
            existingCart.setTotalPrice(0.00);
            existingCart.setUser(authUtil.loggedInUser());
            existingCart = cartRepo.save(existingCart);
        } else {
            // Clear all current items in the existing cart
            cartItemRepo.deleteAllByCartId(existingCart.getCartId());
        }

        double totalPrice = 0.00;

        // Process each item in the request to add to the cart
        for (CartItemDTO cartItemDTO : cartItems) {
            Long productId = cartItemDTO.getProductId();
            Integer quantity = cartItemDTO.getQuantity();

            // Find the product by ID
            Product product = productRepo.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

            // Directly update product stock and total price
            // product.setQuantity(product.getQuantity() - quantity);
            totalPrice += product.getSpecialPrice() * quantity;

            // Create and save cart item
            CartItem cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setCart(existingCart);
            cartItem.setQuantity(quantity);
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setDiscount(product.getDiscount());
            cartItemRepo.save(cartItem);
        }

        // Update the cart's total price and save
        existingCart.setTotalPrice(totalPrice);
        cartRepo.save(existingCart);
        return "Cart created/updated with the new items successfully";
    }
}
