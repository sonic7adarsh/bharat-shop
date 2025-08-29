package com.bharatshop.shared.service;

import com.bharatshop.shared.dto.PageRequestDto;
import com.bharatshop.shared.dto.PageResponseDto;
import com.bharatshop.shared.entity.Page;
import com.bharatshop.shared.enums.PageType;
import com.bharatshop.shared.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing pages in the platform.
 * Provides CRUD operations and business logic for page management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PageService {
    
    private static final Logger log = LoggerFactory.getLogger(PageService.class);
    
    private final PageRepository pageRepository;
    
    /**
     * Get all pages with pagination
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PageResponseDto> getAllPages(Pageable pageable) {
        log.debug("Fetching all pages with pagination: {}", pageable);
        
        org.springframework.data.domain.Page<Page> pages = pageRepository.findAllActive(pageable);
        return pages.map(this::mapToResponseDto);
    }
    
    /**
     * Get page by ID
     */
    @Transactional(readOnly = true)
    public Optional<PageResponseDto> getPageById(UUID id) {
        log.debug("Fetching page with ID: {}", id);
        
        return pageRepository.findById(id)
            .filter(p -> p.getDeletedAt() == null)
            .map(this::mapToResponseDto);
    }
    
    /**
     * Get pages by type
     */
    @Transactional(readOnly = true)
    public List<PageResponseDto> getPagesByType(PageType pageType) {
        log.debug("Fetching pages of type: {}", pageType);
        
        List<Page> pages = pageRepository.findActiveByPageType(pageType);
        return pages.stream().map(this::mapToResponseDto).collect(Collectors.toList());
    }
    
    /**
     * Create new page
     */
    public PageResponseDto createPage(PageRequestDto pageRequest) {
        log.debug("Creating new page with title: {}", pageRequest.getTitle());
        
        // Check if slug already exists
        if (pageRepository.existsBySlug(pageRequest.getSlug())) {
            throw new IllegalArgumentException("Page with slug '" + pageRequest.getSlug() + "' already exists");
        }
        
        Page page = mapToEntity(pageRequest);
        Page savedPage = pageRepository.save(page);
        return mapToResponseDto(savedPage);
    }
    
    /**
     * Update existing page
     */
    public PageResponseDto updatePage(UUID id, PageRequestDto pageRequest) {
        log.debug("Updating page with ID: {}", id);
        
        Page existingPage = pageRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Page not found with ID: " + id));
        
        // Check if slug already exists (excluding current page)
        if (!existingPage.getSlug().equals(pageRequest.getSlug()) && 
            pageRepository.existsBySlugExcludingId(pageRequest.getSlug(), id)) {
            throw new IllegalArgumentException("Page with slug '" + pageRequest.getSlug() + "' already exists");
        }
        
        updateEntityFromRequest(existingPage, pageRequest);
        Page updatedPage = pageRepository.save(existingPage);
        return mapToResponseDto(updatedPage);
    }
    
    /**
     * Update page layout
     */
    public PageResponseDto updatePageLayout(UUID id, String layout) {
        log.debug("Updating layout for page with id: {}", id);
        
        Page page = pageRepository.findById(id)
            .filter(p -> p.getDeletedAt() == null)
            .orElseThrow(() -> new IllegalArgumentException("Page not found with id: " + id));
        
        page.setLayout(layout);
        page.setUpdatedAt(LocalDateTime.now());
        
        Page updatedPage = pageRepository.save(page);
        return mapToResponseDto(updatedPage);
    }
    
    /**
     * Update page SEO
     */
    public PageResponseDto updatePageSeo(UUID id, String seoJson) {
        log.debug("Updating SEO for page with id: {}", id);
        
        Page page = pageRepository.findById(id)
            .filter(p -> p.getDeletedAt() == null)
            .orElseThrow(() -> new IllegalArgumentException("Page not found with id: " + id));
        
        page.setSeo(seoJson);
        page.setUpdatedAt(LocalDateTime.now());
        
        Page updatedPage = pageRepository.save(page);
        return mapToResponseDto(updatedPage);
    }
    
    /**
     * Toggle page publish status
     */
    public PageResponseDto togglePagePublishStatus(UUID id) {
        log.debug("Toggling publish status for page with id: {}", id);
        
        Page page = pageRepository.findById(id)
            .filter(p -> p.getDeletedAt() == null)
            .orElseThrow(() -> new IllegalArgumentException("Page not found with id: " + id));
        
        page.setPublished(!page.getPublished());
        page.setUpdatedAt(LocalDateTime.now());
        
        Page updatedPage = pageRepository.save(page);
        return mapToResponseDto(updatedPage);
    }
    
    /**
     * Delete page (soft delete)
     */
    public void deletePage(UUID id) {
        log.debug("Deleting page with id: {}", id);
        
        Page page = pageRepository.findById(id)
            .filter(p -> p.getDeletedAt() == null)
            .orElseThrow(() -> new IllegalArgumentException("Page not found with id: " + id));
        
        page.setDeletedAt(LocalDateTime.now());
        pageRepository.save(page);
    }
    
    /**
     * Map Page entity to PageResponseDto
     */
    private PageResponseDto mapToResponseDto(Page page) {
        PageResponseDto dto = new PageResponseDto();
        dto.setId(page.getId());
        dto.setTitle(page.getTitle());
        dto.setSlug(page.getSlug());
        dto.setContent(page.getContent());
        dto.setExcerpt(page.getExcerpt());
        dto.setMetaTitle(page.getMetaTitle());
        dto.setMetaDescription(page.getMetaDescription());
        dto.setMetaKeywords(page.getMetaKeywords());
        dto.setLayout(page.getLayout());
        dto.setSeo(page.getSeo());
        dto.setActive(page.getActive());
        dto.setPublished(page.getPublished());
        dto.setSortOrder(page.getSortOrder());
        dto.setPageType(page.getPageType());
        dto.setTemplate(page.getTemplate());
        dto.setTemplateId(page.getTemplateId());
        dto.setCustomCss(page.getCustomCss());
        dto.setCustomJs(page.getCustomJs());
        dto.setFeaturedImage(page.getFeaturedImage());
        dto.setAuthor(page.getAuthor());
        dto.setStatus(page.getStatus());
        dto.setCreatedAt(page.getCreatedAt());
        dto.setUpdatedAt(page.getUpdatedAt());
        return dto;
    }
    
    /**
     * Map PageRequestDto to Page entity
     */
    private Page mapToEntity(PageRequestDto request) {
        return Page.builder()
                .title(request.getTitle())
                .slug(request.getSlug())
                .content(request.getContent())
                .excerpt(request.getExcerpt())
                .metaTitle(request.getMetaTitle())
                .metaDescription(request.getMetaDescription())
                .metaKeywords(request.getMetaKeywords())
                .layout(request.getLayout())
                .seo(request.getSeo())
                .active(true)
                .published(request.getPublished() != null ? request.getPublished() : false)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .pageType(request.getPageType() != null ? request.getPageType() : PageType.STATIC)
                .template(request.getTemplate())
                .templateId(request.getTemplateId())
                .customCss(request.getCustomCss())
                .customJs(request.getCustomJs())
                .featuredImage(request.getFeaturedImage())
                .author(request.getAuthor())
                .status(request.getStatus() != null ? request.getStatus() : "draft")
                .build();
    }
    
    /**
     * Update Page entity from PageRequestDto
     */
    private void updateEntityFromRequest(Page page, PageRequestDto request) {
        page.setTitle(request.getTitle());
        page.setSlug(request.getSlug());
        page.setContent(request.getContent());
        page.setExcerpt(request.getExcerpt());
        page.setMetaTitle(request.getMetaTitle());
        page.setMetaDescription(request.getMetaDescription());
        page.setMetaKeywords(request.getMetaKeywords());
        page.setLayout(request.getLayout());
        page.setSeo(request.getSeo());
        if (request.getPublished() != null) {
            page.setPublished(request.getPublished());
        }
        if (request.getSortOrder() != null) {
            page.setSortOrder(request.getSortOrder());
        }
        if (request.getPageType() != null) {
            page.setPageType(request.getPageType());
        }
        page.setTemplate(request.getTemplate());
        page.setTemplateId(request.getTemplateId());
        page.setCustomCss(request.getCustomCss());
        page.setCustomJs(request.getCustomJs());
        page.setFeaturedImage(request.getFeaturedImage());
        page.setAuthor(request.getAuthor());
        if (request.getStatus() != null) {
            page.setStatus(request.getStatus());
        }
        page.setUpdatedAt(LocalDateTime.now());
    }
}