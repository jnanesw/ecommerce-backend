package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.*;
import com.ecommerce.project.payload.OrderDTO;
import com.ecommerce.project.payload.OrderItemDTO;
import com.ecommerce.project.repo.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private CartRepo cartRepo;

    @Autowired
    private AddressRepo addressRepo;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private OrderItemRepo orderItemRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CartServiceImpl cartService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PaymentRepo paymentRepo;

    @Transactional
    @Override
    public OrderDTO placeOrder(String email, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage) {
        // Getting User Cart
        Cart cart = cartRepo.findCartByEmail(email);
        if(cart == null){
            throw new ResourceNotFoundException("Cart", "emailId", email);
        }

        Address address = addressRepo.findById(addressId).orElseThrow(()->
                new ResourceNotFoundException("Address", "addressId",addressId));

        // Create a new Order with payment Info
        Order order = new Order();
        order.setEmail(email);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus("Accepted");
        order.setAddress(address);

        Payment payment = new Payment(paymentMethod, pgPaymentId, pgStatus, pgResponseMessage, pgName);
        payment.setOrder(order);
        payment = paymentRepo.save(payment);
        order.setPayment(payment);
        Order savedOrder = orderRepo.save(order);

        // Get items from the cart into the order items
        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderItem> finalOrderItems = orderItems;
        cart.getCartItems().forEach(item->{
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(item.getProduct());
            orderItem.setOrder(savedOrder);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setDiscount(item.getDiscount());
            orderItem.setOrderedProductPrice(item.getProductPrice());
            finalOrderItems.add(orderItem);
        });

        orderItems = orderItemRepo.saveAll(finalOrderItems);
        // Update Product stock
        List<CartItem> items = new ArrayList<>(cart.getCartItems());

        for (CartItem cartItem : items) {
            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();
            product.setQuantity(product.getQuantity() - quantity);
            productRepo.save(product);

            cartService.deleteProductFromCart(cart.getCartId(), cartItem.getProduct().getProductId());
        }

        // Send back the order summary
        OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);
        orderItems.forEach(item -> {
            orderDTO.getOrderItems().add(modelMapper.map(item, OrderItemDTO.class));
        });

        return orderDTO;
    }
}
