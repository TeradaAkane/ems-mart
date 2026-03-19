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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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
        
        // 常に在庫を100にリセットしてテストデータの依存をなくす
        inventory.setStockQuantity(100);
        inventoryRepository.save(inventory);
        initialStock = 100;
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
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "password", List.of()));
        SecurityContextHolder.setContext(context);
        try {
            orderService.createOrder(request);
        } finally {
            SecurityContextHolder.clearContext();
        }

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
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "password", List.of()));
        SecurityContextHolder.setContext(context);
        try {
            assertThrows(RuntimeException.class, () -> {
                orderService.createOrder(request);
            });
        } finally {
            SecurityContextHolder.clearContext();
        }

        // 重要：例外が発生しても @Transactional があれば在庫の更新はロールバックされているはず
        Inventory updatedInventory = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(updatedInventory.getStockQuantity()).isEqualTo(initialStock);
    }

    @Test
    void testCreateOrder_Concurrency_Success() throws InterruptedException {
        // 1. 正常な同時注文の検証
        // 設定: 在庫を 100 に設定
        Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow();
        inventory.setStockQuantity(100);
        inventoryRepository.save(inventory);

        int threadCount = 10;
        int orderQuantity = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    // 各スレッドで認証情報をセット
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "password", List.of()));
                    SecurityContextHolder.setContext(context);

                    OrderRequest request = new OrderRequest();
                    OrderItemRequest item = new OrderItemRequest();
                    item.setProductId(productId);
                    item.setQuantity(orderQuantity);
                    request.setItems(List.of(item));

                    orderService.createOrder(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Order failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                    SecurityContextHolder.clearContext();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // 期待結果: 全ての注文が成功し、最終的な在庫数が0になる
        Inventory updatedInventory = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(updatedInventory.getStockQuantity()).isEqualTo(0);
    }

    @Test
    void testCreateOrder_Concurrency_InsufficientStock() throws InterruptedException {
        // 2. 在庫不足時の同時注文の検証
        // 設定: 在庫を 99 に設定
        Inventory inventory = inventoryRepository.findByProductId(productId).orElseThrow();
        inventory.setStockQuantity(99);
        inventoryRepository.save(inventory);

        int threadCount = 10;
        int orderQuantity = 10; // 合計 100 個注文しようとする
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "password", List.of()));
                    SecurityContextHolder.setContext(context);

                    OrderRequest request = new OrderRequest();
                    OrderItemRequest item = new OrderItemRequest();
                    item.setProductId(productId);
                    item.setQuantity(orderQuantity);
                    request.setItems(List.of(item));

                    orderService.createOrder(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                    SecurityContextHolder.clearContext();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // 期待結果: 9人の注文が成功し、1人が失敗する。最終的な在庫数は 9 になる。
        Inventory updatedInventory = inventoryRepository.findByProductId(productId).orElseThrow();
        assertThat(successCount.get()).isEqualTo(9);
        assertThat(failureCount.get()).isEqualTo(1);
        assertThat(updatedInventory.getStockQuantity()).isEqualTo(9); // 99 - 90 = 9
    }
}
