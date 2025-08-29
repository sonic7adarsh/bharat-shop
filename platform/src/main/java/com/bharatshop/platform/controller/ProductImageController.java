package com.bharatshop.platform.controller;

import com.bharatshop.platform.service.ProductImageService;
import com.bharatshop.shared.entity.ProductImage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/platform/products")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService productImageService;

    @GetMapping("/{productId}/images")
    public ResponseEntity<List<ProductImage>> getProductImages(
            @PathVariable UUID productId,
            Authentication authentication) {
        
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            List<ProductImage> images = productImageService.getProductImages(productId, tenantId);
            return ResponseEntity.ok(images);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found") || e.getMessage().contains("access denied")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{productId}/images/primary")
    public ResponseEntity<ProductImage> getPrimaryImage(
            @PathVariable UUID productId,
            Authentication authentication) {
        
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            Optional<ProductImage> primaryImage = productImageService.getPrimaryImage(productId, tenantId);
            return primaryImage.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found") || e.getMessage().contains("access denied")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{productId}/images")
    public ResponseEntity<Map<String, Object>> uploadProductImage(
            @PathVariable UUID productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "altText", required = false) String altText,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary,
            Authentication authentication) {
        
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            ProductImage uploadedImage = productImageService.uploadProductImage(
                    productId, tenantId, file, altText, isPrimary);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Image uploaded successfully");
            response.put("image", uploadedImage);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            if (e.getMessage().contains("not found") || e.getMessage().contains("access denied")) {
                errorResponse.put("error", "Product not found or access denied");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error uploading file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/{productId}/images/multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleImages(
            @PathVariable UUID productId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "altTexts", required = false) String[] altTexts,
            Authentication authentication) {
        
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            
            Map<String, Object> response = new HashMap<>();
            response.put("uploadedImages", new java.util.ArrayList<>());
            response.put("errors", new java.util.ArrayList<>());
            
            @SuppressWarnings("unchecked")
            List<ProductImage> uploadedImages = (List<ProductImage>) response.get("uploadedImages");
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) response.get("errors");
            
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                String altText = (altTexts != null && i < altTexts.length) ? altTexts[i] : null;
                
                try {
                    ProductImage uploadedImage = productImageService.uploadProductImage(
                            productId, tenantId, file, altText, false);
                    uploadedImages.add(uploadedImage);
                } catch (Exception e) {
                    errors.add("File " + file.getOriginalFilename() + ": " + e.getMessage());
                }
            }
            
            response.put("successCount", uploadedImages.size());
            response.put("errorCount", errors.size());
            response.put("totalFiles", files.length);
            
            if (errors.isEmpty()) {
                response.put("success", true);
                response.put("message", "All images uploaded successfully");
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else if (uploadedImages.isEmpty()) {
                response.put("success", false);
                response.put("message", "No images were uploaded");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            } else {
                response.put("success", true);
                response.put("message", "Some images uploaded successfully");
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
            }
            
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            if (e.getMessage().contains("not found") || e.getMessage().contains("access denied")) {
                errorResponse.put("error", "Product not found or access denied");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/images/{imageId}")
    public ResponseEntity<ProductImage> updateProductImage(
            @PathVariable UUID imageId,
            @RequestBody Map<String, Object> updateData,
            Authentication authentication) {
        
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            
            String altText = (String) updateData.get("altText");
            Boolean isPrimary = (Boolean) updateData.get("isPrimary");
            Integer sortOrder = updateData.get("sortOrder") != null ? 
                    Integer.valueOf(updateData.get("sortOrder").toString()) : null;
            
            ProductImage updatedImage = productImageService.updateProductImage(
                    imageId, tenantId, altText, isPrimary, sortOrder);
            
            return ResponseEntity.ok(updatedImage);
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found") || e.getMessage().contains("access denied")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Map<String, Object>> deleteProductImage(
            @PathVariable UUID imageId,
            Authentication authentication) {
        
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            productImageService.deleteProductImage(imageId, tenantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Image deleted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            if (e.getMessage().contains("not found") || e.getMessage().contains("access denied")) {
                errorResponse.put("error", "Image not found or access denied");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/images/{imageId}/primary")
    public ResponseEntity<ProductImage> setPrimaryImage(
            @PathVariable UUID imageId,
            Authentication authentication) {
        
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            ProductImage primaryImage = productImageService.setPrimaryImage(imageId, tenantId);
            return ResponseEntity.ok(primaryImage);
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found") || e.getMessage().contains("access denied")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{productId}/images/reorder")
    public ResponseEntity<List<ProductImage>> reorderProductImages(
            @PathVariable UUID productId,
            @RequestBody Map<String, List<UUID>> reorderData,
            Authentication authentication) {
        
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            List<UUID> imageIds = reorderData.get("imageIds");
            
            if (imageIds == null || imageIds.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            List<ProductImage> reorderedImages = productImageService.reorderProductImages(
                    productId, tenantId, imageIds);
            
            return ResponseEntity.ok(reorderedImages);
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found") || e.getMessage().contains("access denied")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{productId}/images")
    public ResponseEntity<Map<String, Object>> deleteAllProductImages(
            @PathVariable UUID productId,
            Authentication authentication) {
        
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            productImageService.deleteAllProductImages(productId, tenantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All images deleted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            if (e.getMessage().contains("not found") || e.getMessage().contains("access denied")) {
                errorResponse.put("error", "Product not found or access denied");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/{productId}/images/count")
    public ResponseEntity<Map<String, Object>> getImageCount(
            @PathVariable UUID productId,
            Authentication authentication) {
        
        try {
            UUID tenantId = getTenantIdFromAuth(authentication);
            long imageCount = productImageService.getImageCount(productId, tenantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("imageCount", imageCount);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found") || e.getMessage().contains("access denied")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private UUID getTenantIdFromAuth(Authentication authentication) {
        // Extract tenant ID from JWT token or user details
        // This is a placeholder - implement based on your JWT structure
        return UUID.fromString("00000000-0000-0000-0000-000000000001"); // For now, return a default tenant ID
    }
}