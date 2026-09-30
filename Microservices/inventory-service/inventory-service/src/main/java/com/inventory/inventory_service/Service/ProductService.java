package com.inventory.inventory_service.Service;

import com.inventory.inventory_service.Config.RedisConfig;
import com.inventory.inventory_service.Dto.ProductRequestDto;
import com.inventory.inventory_service.Dto.ProductResponseDto;
import com.inventory.inventory_service.Dto.StockAdjustmentDto;
import com.inventory.inventory_service.Dto.StockMovementResponseDto;
import com.inventory.inventory_service.Entity.Product;
import com.inventory.inventory_service.Entity.StockMovement;
import com.inventory.inventory_service.Entity.OutboxEvent;
import com.inventory.inventory_service.Entity.OutboxStatus;
import com.inventory.inventory_service.Exception.DuplicateResourceException;
import com.inventory.inventory_service.Exception.InsufficientStockException;
import com.inventory.inventory_service.Exception.ResourceNotFoundException;
import com.inventory.inventory_service.Repository.ProductRepository;
import com.inventory.inventory_service.Repository.StockMovementRepository;
import com.inventory.inventory_service.Repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProducts(String search, Pageable pageable) {
        return productRepository.searchProducts(search, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_PRODUCTS)
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getOrderableProducts() {
        return productRepository.findAll().stream()
                .filter(product -> product.getQuantity() != null && product.getQuantity() > 0)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_PRODUCT_BY_ID, key = "#id")
    public ProductResponseDto getProductById(Long id) {
        log.info("Cache miss for product id {}. Fetching from database.", id);
        Product product = findProductByIdOrThrow(id);
        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_PRODUCT_BY_SKU, key = "#sku")
    public ProductResponseDto getProductBySku(String sku) {
        log.info("Cache miss for product sku {}. Fetching from database.", sku);
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        return mapToResponse(product);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS, allEntries = true)
    })
    public ProductResponseDto createProduct(ProductRequestDto request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product with SKU '" + request.getSku() + "' already exists");
        }

        Product product = Product.builder()
                .sku(request.getSku().trim().toUpperCase())
                .name(request.getName().trim())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .build();

        Product savedProduct = productRepository.save(product);

        // Record initial stock movement if quantity > 0
        if (savedProduct.getQuantity() > 0) {
            recordStockMovement(
                    savedProduct.getId(),
                    savedProduct.getSku(),
                    savedProduct.getQuantity(),
                    savedProduct.getQuantity(),
                    StockMovement.MovementType.INITIAL_IMPORT,
                    "Initial product creation stock"
            );
        }

        log.info("Created product with id: {}, SKU: {}", savedProduct.getId(), savedProduct.getSku());
        return mapToResponse(savedProduct);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PRODUCT_BY_ID, key = "#id"),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCT_BY_SKU, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS, allEntries = true)
    })
    public ProductResponseDto updateProduct(Long id, ProductRequestDto request) {
        Product product = findProductByIdOrThrow(id);

        if (!product.getSku().equalsIgnoreCase(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product with SKU '" + request.getSku() + "' already exists");
        }

        product.setSku(request.getSku().trim().toUpperCase());
        product.setName(request.getName().trim());
        product.setPrice(request.getPrice());

        int quantityDiff = request.getQuantity() - product.getQuantity();
        if (quantityDiff != 0) {
            product.setQuantity(request.getQuantity());
            recordStockMovement(
                    product.getId(),
                    product.getSku(),
                    quantityDiff,
                    product.getQuantity(),
                    StockMovement.MovementType.MANUAL_ADJUSTMENT,
                    "Quantity update via product edit"
            );
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Updated product with id: {}", updatedProduct.getId());
        return mapToResponse(updatedProduct);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PRODUCT_BY_ID, key = "#id"),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCT_BY_SKU, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS, allEntries = true)
    })
    public ProductResponseDto adjustStock(Long id, StockAdjustmentDto dto) {
        Product product = findProductByIdOrThrow(id);

        int newQuantity = product.getQuantity() + dto.getAmount();
        if (newQuantity < 0) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock for product %s. Current: %d, Requested adjustment: %d",
                            product.getSku(), product.getQuantity(), dto.getAmount())
            );
        }

        product.setQuantity(newQuantity);
        Product savedProduct = productRepository.save(product);

        if (newQuantity < 10) recordStockLow(savedProduct);

        recordStockMovement(
                savedProduct.getId(),
                savedProduct.getSku(),
                dto.getAmount(),
                newQuantity,
                StockMovement.MovementType.MANUAL_ADJUSTMENT,
                dto.getReason() != null ? dto.getReason() : "Manual stock adjustment"
        );

        log.info("Adjusted stock for product {}. New quantity: {}", savedProduct.getId(), newQuantity);
        return mapToResponse(savedProduct);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = RedisConfig.CACHE_PRODUCT_BY_ID, key = "#id"),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCT_BY_SKU, allEntries = true),
            @CacheEvict(value = RedisConfig.CACHE_PRODUCTS, allEntries = true)
    })
    public void deleteProduct(Long id) {
        Product product = findProductByIdOrThrow(id);
        productRepository.delete(product);
        log.info("Deleted product with id: {}", id);
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponseDto> getStockMovements(Long productId) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::mapToMovementResponse)
                .collect(Collectors.toList());
    }

    private void recordStockMovement(Long productId, String sku, int quantityChange, int quantityAfter,
                                     StockMovement.MovementType type, String reason) {
        StockMovement movement = StockMovement.builder()
                .productId(productId)
                .sku(sku)
                .quantityChange(quantityChange)
                .quantityAfter(quantityAfter)
                .movementType(type)
                .reason(reason)
                .build();
        stockMovementRepository.save(movement);
    }

    private void recordStockLow(Product product) {
        try {
            String eventId = UUID.randomUUID().toString();
            String payload = objectMapper.writeValueAsString(Map.of(
                    "eventId", eventId,
                    "eventType", "STOCK_LOW",
                    "occurredAt", Instant.now().toString(),
                    "sku", product.getSku(),
                    "quantity", product.getQuantity()));
            outboxEventRepository.save(OutboxEvent.builder()
                    .eventId(eventId)
                    .eventType("STOCK_LOW")
                    .aggregateType("PRODUCT")
                    .aggregateId(product.getSku())
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to persist stock-low event", ex);
        }
    }

    private Product findProductByIdOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private ProductResponseDto mapToResponse(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .quantity(product.getQuantity())
                .price(product.getPrice())
                .version(product.getVersion())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private StockMovementResponseDto mapToMovementResponse(StockMovement movement) {
        return StockMovementResponseDto.builder()
                .id(movement.getId())
                .productId(movement.getProductId())
                .sku(movement.getSku())
                .quantityChange(movement.getQuantityChange())
                .quantityAfter(movement.getQuantityAfter())
                .movementType(movement.getMovementType())
                .reason(movement.getReason())
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
