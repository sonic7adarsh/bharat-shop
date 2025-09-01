package com.bharatshop.storefront.service;

import com.bharatshop.storefront.dto.PageResponseDto;
import com.bharatshop.storefront.model.Page;
import com.bharatshop.storefront.repository.PageRepository;
import com.bharatshop.shared.entity.Template;
import com.bharatshop.shared.service.TemplateService;
import com.bharatshop.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing CMS pages in the storefront.
 * Provides read-only operations for customer-facing page content.
 */
@Service("storefrontPageService")
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StorefrontPageService {
    
    private final PageRepository pageRepository;
    private final TemplateService templateService;
    
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
     * Get page with merged template and layout configuration for rendering
     */
    @Transactional(readOnly = true)
    public Optional<PageRenderData> getPageRenderData(String slug) {
        String tenantIdStr = TenantContext.getCurrentTenant();
        UUID tenantId = UUID.fromString(tenantIdStr);
        Optional<Page> pageOpt = pageRepository.findBySlugAndTenantId(slug, tenantId);
        
        if (pageOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Page page = pageOpt.get();
        PageRenderData renderData = new PageRenderData();
        renderData.setPage(page);
        
        // Get template configuration if template is specified
        if (page.getTemplate() != null && !page.getTemplate().trim().isEmpty()) {
            Optional<Template> templateOpt = templateService.getTemplateByName(page.getTemplate());
            templateOpt.ifPresent(renderData::setTemplate);
        }
        
        return Optional.of(renderData);
    }
    
    /**
     * Update page layout (for template customization)
     */
    public Page updatePageLayout(UUID id, String layoutJson) {
        Page page = pageRepository.findActiveById(id)
            .orElseThrow(() -> new IllegalArgumentException("Page not found with ID: " + id));
        
        if (layoutJson != null && !isValidJson(layoutJson)) {
            throw new IllegalArgumentException("Invalid JSON format in layout configuration");
        }
        
        page.setLayout(layoutJson);
        
        log.info("Updated layout for page: {}", page.getTitle());
        return pageRepository.save(page);
    }
    
    /**
     * Update page SEO settings
     */
    public Page updatePageSeo(UUID id, String seoJson) {
        Page page = pageRepository.findActiveById(id)
            .orElseThrow(() -> new IllegalArgumentException("Page not found with ID: " + id));
        
        if (seoJson != null && !isValidJson(seoJson)) {
            throw new IllegalArgumentException("Invalid JSON format in SEO configuration");
        }
        
        page.setSeo(seoJson);
        
        log.info("Updated SEO for page: {}", page.getTitle());
        return pageRepository.save(page);
    }
    
    /**
     * Basic JSON validation
     */
    private boolean isValidJson(String json) {
        json = json.trim();
        return (json.startsWith("{") && json.endsWith("}")) || 
               (json.startsWith("[") && json.endsWith("]"));
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
                .layout(page.getLayout())
                .seo(page.getSeo())
                .sortOrder(page.getSortOrder())
                .createdAt(page.getCreatedAt())
                .updatedAt(page.getUpdatedAt())
                .build();
    }
    
    /**
     * Data class for page rendering with template
     */
    public static class PageRenderData {
        private Page page;
        private Template template;
        
        public Page getPage() { return page; }
        public void setPage(Page page) { this.page = page; }
        
        public Template getTemplate() { return template; }
        public void setTemplate(Template template) { this.template = template; }
    }
}