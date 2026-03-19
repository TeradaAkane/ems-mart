package com.springmart.service;

import com.springmart.dto.ProductRequest;
import com.springmart.dto.ProductResponse;
import com.springmart.entity.Inventory;
import com.springmart.entity.Product;
import com.springmart.repository.InventoryRepository;
import com.springmart.repository.OrderDetailRepository;
import com.springmart.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderDetailRepository orderDetailRepository;

    public ProductService(ProductRepository productRepository, InventoryRepository inventoryRepository,
            OrderDetailRepository orderDetailRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderDetailRepository = orderDetailRepository;
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(p -> new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getVersion()))
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品が見つかりません: " + id));
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getVersion());
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product = productRepository.save(product);

        // 在庫テーブルに初期在庫数を登録
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setStockQuantity(request.getInitialStock());
        inventoryRepository.save(inventory);

        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getVersion());
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商品が見つかりません: " + id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setVersion(request.getVersion());
        productRepository.save(product);

        if (request.getInitialStock() != null) {
            Inventory inventory = inventoryRepository.findByProductId(id)
                    .orElseThrow(() -> new RuntimeException("在庫データが見つかりません: " + id));
            inventory.setStockQuantity(request.getInitialStock());
            inventoryRepository.save(inventory);
        }

        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getVersion());
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "商品が見つかりません: " + id);
        }

        // 注文履歴があるかチェック（外部キー制約エラーを避けるため）
        if (orderDetailRepository.existsByProductId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "この商品は注文履歴に使用されているため削除できません");
        }

        // 在庫データを先に削除する必要がある（外部キー制約）
        inventoryRepository.deleteById(id);
        productRepository.deleteById(id);
    }
}
