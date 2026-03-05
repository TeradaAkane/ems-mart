package com.springmart.service;

import com.springmart.dto.OrderItemRequest;
import com.springmart.dto.OrderRequest;
import com.springmart.entity.Inventory;
import com.springmart.entity.Product;
import com.springmart.repository.InventoryRepository;
import com.springmart.repository.OrderRepository;
import com.springmart.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
public class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @SpyBean
    private OrderRepository orderRepository;

    private Long productId;
    private int initialStock;

    @BeforeEach
    void setUp() {
        // テスト用の商品と在庫を確認
        Product product = productRepository.findAll().get(0);
        productId = product.getId();
        Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow();
        initialStock = inventory.getStockQuantity();
    }

    @Test
    void testCreateOrder_Success() {
        // Given
        OrderRequest request = new OrderRequest();
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(5);
        request.setItems(List.of(item));

        // When
        orderService.createOrder(request);

        // Then
        Inventory updatedInventory = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(updatedInventory.getStockQuantity()).isEqualTo(initialStock - 5);
    }

    @Test
    void testCreateOrder_RollbackOnException() {
        // Given
        // OrderRepository.save が呼ばれた時に例外を投げるように設定
        doThrow(new RuntimeException("模擬的なDBエラー")).when(orderRepository).save(any());

        OrderRequest request = new OrderRequest();
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(10);
        request.setItems(List.of(item));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(request);
        });

        // 重要：例外が発生しても @Transactional があれば在庫の更新はロールバックされているはず
        Inventory updatedInventory = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(updatedInventory.getStockQuantity()).isEqualTo(initialStock);
    }
}
