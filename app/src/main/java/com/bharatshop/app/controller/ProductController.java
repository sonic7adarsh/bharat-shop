package com.bharatshop.app.controller;

import com.bharatshop.shared.dto.ApiResponse;
import com.bharatshop.shared.dto.PagedResponse;
import com.bharatshop.shared.dto.ProductDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Product management controller
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management operations")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public class ProductController {

    @Operation(
        summary = "Get all products",
        description = "Retrieve a paginated list of products with optional filtering"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Products retrieved successfully",
            content = @Content(schema = @Schema(implementation = PagedResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @GetMapping
    public ResponseEntity<PagedResponse<ProductDto>> getAllProducts(
            @Parameter(description = "Category ID to filter products")
            @RequestParam(required = false) Long categoryId,
            
            @Parameter(description = "Search term for product name or description")
            @RequestParam(required = false) String search,
            
            @Parameter(description = "Product status filter")
            @RequestParam(required = false) String status,
            
            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false) Double minPrice,
            
            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false) Double maxPrice,
            
            @Parameter(description = "Pagination parameters")
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("Fetching products with filters - categoryId: {}, search: {}, status: {}", 
                categoryId, search, status);
        
        // Mock response for demonstration
        PagedResponse<ProductDto> response = PagedResponse.<ProductDto>builder()
                .content(List.of())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(0L)
                .totalPages(0)
                .first(true)
                .last(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get product by ID",
        description = "Retrieve a specific product by its unique identifier"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Product found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getProductById(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id) {
        
        log.info("Fetching product with ID: {}", id);
        
        ApiResponse<ProductDto> response = ApiResponse.<ProductDto>builder()
                .success(true)
                .message("Product retrieved successfully")
                .data(null) // Mock data would go here
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Create new product",
        description = "Create a new product in the system"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Product created successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid product data",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @Parameter(description = "Product data", required = true)
            @Valid @RequestBody ProductDto productDto) {
        
        log.info("Creating new product: {}", productDto.getName());
        
        ApiResponse<ProductDto> response = ApiResponse.<ProductDto>builder()
                .success(true)
                .message("Product created successfully")
                .data(productDto)
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Update product",
        description = "Update an existing product"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Product updated successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id,
            
            @Parameter(description = "Updated product data", required = true)
            @Valid @RequestBody ProductDto productDto) {
        
        log.info("Updating product with ID: {}", id);
        
        ApiResponse<ProductDto> response = ApiResponse.<ProductDto>builder()
                .success(true)
                .message("Product updated successfully")
                .data(productDto)
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Delete product",
        description = "Delete a product from the system"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Product deleted successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Product not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long id) {
        
        log.info("Deleting product with ID: {}", id);
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Product deleted successfully")
                .timestamp(LocalDateTime.now())
                .build();
        
        return ResponseEntity.ok(response);
    }
}