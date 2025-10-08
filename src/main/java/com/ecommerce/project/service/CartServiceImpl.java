package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItems;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.CartDTO;
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
        CartItems cartItems = cartItemRepo.findCartItemsByProductIdAndCartId(productId, cart.getCartId());
        if (cartItems != null) {
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
        CartItems cartItems1 = new CartItems();
        cartItems1.setCart(cart);
        cartItems1.setProduct(product);
        cartItems1.setQuantity(quantity);
        cartItems1.setDiscount(product.getDiscount());
        cartItems1.setProductPrice(product.getSpecialPrice());
        // 5. Save cartItems
        cartItemRepo.save(cartItems1);

        product.setQuantity(product.getQuantity());

        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
        cartRepo.save(cart);
        // 6. Return Updated cart
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<CartItems> cartItemsList = cart.getCartItems();
        List<ProductDTO> productDTOList = cartItemsList.stream().map(item ->{
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
        List<ProductDTO> productDTOs = cart2.getCartItems().stream().map(cartItems -> {
            ProductDTO productDTO = modelMapper.map(cartItems.getProduct(), ProductDTO.class);
            productDTO.setQuantity(cartItems.getQuantity());
            return productDTO;
        }).toList();

        cartDTO.setProducts(productDTOs);

        return cartDTO;
    }

    @Override
    public CartDTO updateProductQuantityInCart(Long productId, int quantity){
        // 1. get cart using mail and cartId. Verify the
        //
    }

    private Cart createCart(){
        Cart userCart = cartRepo.findCartByEmail(authUtil.loggedInEmail());
        if(userCart != null) return  userCart;

        Cart newCart = new Cart();
        newCart.setUser(authUtil.loggedInUser());
        newCart.setTotalPrice(0.0);

        return cartRepo.save(newCart);
    }
}
