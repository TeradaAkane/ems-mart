package com.springmart.service;

import com.springmart.dto.OrderItemRequest;
import com.springmart.dto.OrderRequest;
import com.springmart.dto.OrderResponse;
import com.springmart.entity.*;
import com.springmart.repository.InventoryRepository;
import com.springmart.repository.OrderDetailRepository;
import com.springmart.repository.OrderRepository;
import com.springmart.repository.ProductRepository;
import com.springmart.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository, OrderDetailRepository orderDetailRepository,
            ProductRepository productRepository, InventoryRepository inventoryRepository,
            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        String username;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            username = authentication.getName();
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ログインが必要です");
        }

        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません: " + username));

        Order order = new Order();
        order.setUser(user);
        order.setStatus("COMPLETED");

        List<OrderDetail> orderDetails = new ArrayList<>();
        int totalPrice = 0;

        // 各商品について在庫確認と引き当て
        for (OrderItemRequest itemRequest : request.getItems()) {
            Long productId = itemRequest.getProductId();
            Integer quantity = itemRequest.getQuantity();
            Inventory inventory = inventoryRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new RuntimeException("商品が見つかりません: " + productId));


            if (inventory.getStockQuantity() < quantity) {
                throw new OutOfStockException("在庫が不足しています。商品ID: " + productId
                        + "（残り: " + inventory.getStockQuantity() + "個、注文数: " + quantity + "個）");

            }

            inventory.setStockQuantity(inventory.getStockQuantity() - quantity);
            inventoryRepository.save(inventory);

            // 商品情報取得
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("商品が見つかりません: " + productId));

            // 注文明細作成
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(order);
            orderDetail.setProduct(product);
            orderDetail.setQuantity(quantity);
            orderDetail.setPriceAtOrder(product.getPrice());
            orderDetails.add(orderDetail);

            totalPrice += product.getPrice() * quantity;
        }

        order.setTotalPrice(totalPrice);
        order.setOrderDetails(orderDetails);

        order = orderRepository.save(order);

        return new OrderResponse(order.getId(), order.getStatus(), order.getTotalPrice());
    }
}
