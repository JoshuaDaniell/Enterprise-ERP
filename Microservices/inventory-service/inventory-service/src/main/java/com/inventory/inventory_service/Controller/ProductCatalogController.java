package com.inventory.inventory_service.Controller;

import com.inventory.inventory_service.Dto.ApiResponse;
import com.inventory.inventory_service.Dto.ProductResponseDto;
import com.inventory.inventory_service.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only product catalog exposed to authenticated sales users for order entry. */
@RestController
@RequestMapping("/api/catalog/products")
@RequiredArgsConstructor
public class ProductCatalogController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getOrderableProducts() {
        return ResponseEntity.ok(ApiResponse.success(
                "Orderable products retrieved successfully",
                productService.getOrderableProducts()));
    }
}
