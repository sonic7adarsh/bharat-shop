package com.bharatshop.storefront.service;

import com.bharatshop.storefront.dto.PageResponseDto;
import com.bharatshop.storefront.model.Page;
import com.bharatshop.storefront.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing CMS pages in the storefront.
 * Provides read-only operations for customer-facing page content.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PageService {
    
    private final PageRepository pageRepository;
    
    /**
     * Get page by slug (tenant-aware)
     */
    @Cacheable(value = "page", key = "'slug_' + #slug + '_' + #tenantId")
    public PageResponseDto getPageBySlug(String slug, UUID tenantId) {
        log.debug("Fetching page with slug: {} for tenant: {}", slug, tenantId);
        
        Page page = pageRepository.findBySlugAndTenantId(slug, tenantId)
                .orElseThrow(() -> new RuntimeException("Page not found with slug: " + slug));
        
        return mapToResponseDto(page);
    }
    
    /**
     * Get page by slug (without tenant - for backward compatibility)
     */
    @Cacheable(value = "page", key = "'slug_' + #slug")
    public PageResponseDto getPageBySlug(String slug) {
        log.debug("Fetching page with slug: {}", slug);
        
        Page page = pageRepository.findBySlugAndActiveAndPublished(slug, true, true)
                .orElseThrow(() -> new RuntimeException("Page not found with slug: " + slug));
        
        return mapToResponseDto(page);
    }
    
    /**
     * Get all active and published pages for a tenant
     */
    @Cacheable(value = "pages", key = "'tenant_' + #tenantId")
    public List<PageResponseDto> getAllPages(UUID tenantId) {
        log.debug("Fetching all pages for tenant: {}", tenantId);
        
        List<Page> pages = pageRepository.findActivePublishedByTenantId(tenantId);
        return pages.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get pages by type for a tenant
     */
    @Cacheable(value = "pagesByType", key = "#pageType + '_' + #tenantId")
    public List<PageResponseDto> getPagesByType(Page.PageType pageType, UUID tenantId) {
        log.debug("Fetching pages of type: {} for tenant: {}", pageType, tenantId);
        
        List<Page> pages = pageRepository.findByPageTypeAndTenantId(pageType, tenantId);
        return pages.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Map Page entity to PageResponseDto
     */
    private PageResponseDto mapToResponseDto(Page page) {
        return PageResponseDto.builder()
                .id(page.getId())
                .title(page.getTitle())
                .slug(page.getSlug())
                .content(page.getContent())
                .excerpt(page.getExcerpt())
                .metaTitle(page.getMetaTitle())
                .metaDescription(page.getMetaDescription())
                .metaKeywords(page.getMetaKeywords())
                .pageType(page.getPageType())
                .template(page.getTemplate())
                .sortOrder(page.getSortOrder())
                .createdAt(page.getCreatedAt())
                .updatedAt(page.getUpdatedAt())
                .build();
    }
}