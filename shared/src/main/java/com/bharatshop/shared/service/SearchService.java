package com.bharatshop.shared.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling product search with advanced filtering capabilities
 */
@Service
public class SearchService {
    
    /**
     * Search criteria for filtering products
     */
    public static class SearchCriteria {
        private String query;
        private List<Long> categoryIds;
        private List<String> brands;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private List<String> attributes;
        private Map<String, List<String>> attributeFilters;
        private Boolean inStock;
        private String sortBy;
        private String sortDirection;
        private String vendorId;
        private Double minRating;
        private List<String> tags;
        
        // Constructors
        public SearchCriteria() {
            this.categoryIds = new ArrayList<>();
            this.brands = new ArrayList<>();
            this.attributes = new ArrayList<>();
            this.attributeFilters = new HashMap<>();
            this.tags = new ArrayList<>();
            this.sortBy = "relevance";
            this.sortDirection = "desc";
        }
        
        // Getters and Setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public List<Long> getCategoryIds() { return categoryIds; }
        public void setCategoryIds(List<Long> categoryIds) { this.categoryIds = categoryIds; }
        
        public List<String> getBrands() { return brands; }
        public void setBrands(List<String> brands) { this.brands = brands; }
        
        public BigDecimal getMinPrice() { return minPrice; }
        public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
        
        public BigDecimal getMaxPrice() { return maxPrice; }
        public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
        
        public List<String> getAttributes() { return attributes; }
        public void setAttributes(List<String> attributes) { this.attributes = attributes; }
        
        public Map<String, List<String>> getAttributeFilters() { return attributeFilters; }
        public void setAttributeFilters(Map<String, List<String>> attributeFilters) { this.attributeFilters = attributeFilters; }
        
        public Boolean getInStock() { return inStock; }
        public void setInStock(Boolean inStock) { this.inStock = inStock; }
        
        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }
        
        public String getSortDirection() { return sortDirection; }
        public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }
        
        public String getVendorId() { return vendorId; }
        public void setVendorId(String vendorId) { this.vendorId = vendorId; }
        
        public Double getMinRating() { return minRating; }
        public void setMinRating(Double minRating) { this.minRating = minRating; }
        
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        
        // Helper methods
        public void addCategoryId(Long categoryId) {
            if (categoryId != null) {
                this.categoryIds.add(categoryId);
            }
        }
        
        public void addBrand(String brand) {
            if (StringUtils.hasText(brand)) {
                this.brands.add(brand);
            }
        }
        
        public void addAttribute(String attribute) {
            if (StringUtils.hasText(attribute)) {
                this.attributes.add(attribute);
            }
        }
        
        public void addAttributeFilter(String attributeName, String attributeValue) {
            if (StringUtils.hasText(attributeName) && StringUtils.hasText(attributeValue)) {
                this.attributeFilters.computeIfAbsent(attributeName, k -> new ArrayList<>()).add(attributeValue);
            }
        }
        
        public void addTag(String tag) {
            if (StringUtils.hasText(tag)) {
                this.tags.add(tag);
            }
        }
        
        public boolean hasFilters() {
            return StringUtils.hasText(query) ||
                   !categoryIds.isEmpty() ||
                   !brands.isEmpty() ||
                   minPrice != null ||
                   maxPrice != null ||
                   !attributes.isEmpty() ||
                   !attributeFilters.isEmpty() ||
                   inStock != null ||
                   StringUtils.hasText(vendorId) ||
                   minRating != null ||
                   !tags.isEmpty();
        }
    }
    
    /**
     * Search result wrapper
     */
    public static class SearchResult<T> {
        private List<T> items;
        private long totalElements;
        private int totalPages;
        private int currentPage;
        private int pageSize;
        private Map<String, Long> facets;
        private List<String> suggestions;
        
        public SearchResult() {
            this.items = new ArrayList<>();
            this.facets = new HashMap<>();
            this.suggestions = new ArrayList<>();
        }
        
        public SearchResult(List<T> items, long totalElements, int currentPage, int pageSize) {
            this();
            this.items = items;
            this.totalElements = totalElements;
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
        }
        
        // Getters and Setters
        public List<T> getItems() { return items; }
        public void setItems(List<T> items) { this.items = items; }
        
        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
        
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
        
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
        
        public Map<String, Long> getFacets() { return facets; }
        public void setFacets(Map<String, Long> facets) { this.facets = facets; }
        
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
        
        public boolean hasNext() {
            return currentPage < totalPages - 1;
        }
        
        public boolean hasPrevious() {
            return currentPage > 0;
        }
    }
    
    /**
     * Available sort options
     */
    public enum SortOption {
        RELEVANCE("relevance", "Relevance"),
        PRICE_LOW_TO_HIGH("price_asc", "Price: Low to High"),
        PRICE_HIGH_TO_LOW("price_desc", "Price: High to Low"),
        NAME_A_TO_Z("name_asc", "Name: A to Z"),
        NAME_Z_TO_A("name_desc", "Name: Z to A"),
        RATING_HIGH_TO_LOW("rating_desc", "Rating: High to Low"),
        NEWEST_FIRST("created_desc", "Newest First"),
        OLDEST_FIRST("created_asc", "Oldest First"),
        POPULARITY("popularity_desc", "Most Popular");
        
        private final String value;
        private final String displayName;
        
        SortOption(String value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }
        
        public String getValue() { return value; }
        public String getDisplayName() { return displayName; }
        
        public static SortOption fromValue(String value) {
            for (SortOption option : values()) {
                if (option.value.equals(value)) {
                    return option;
                }
            }
            return RELEVANCE;
        }
    }
    
    /**
     * Build search query from criteria
     * @param criteria Search criteria
     * @return Query string for search engine
     */
    public String buildSearchQuery(SearchCriteria criteria) {
        if (criteria == null || !criteria.hasFilters()) {
            return "*:*"; // Match all
        }
        
        List<String> queryParts = new ArrayList<>();
        
        // Text search
        if (StringUtils.hasText(criteria.getQuery())) {
            queryParts.add("(name:*" + escapeQuery(criteria.getQuery()) + "* OR description:*" + escapeQuery(criteria.getQuery()) + "* OR tags:*" + escapeQuery(criteria.getQuery()) + "*)");
        }
        
        // Category filter
        if (!criteria.getCategoryIds().isEmpty()) {
            String categoryFilter = criteria.getCategoryIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(" OR ", "categoryId:(", ")"));
            queryParts.add(categoryFilter);
        }
        
        // Brand filter
        if (!criteria.getBrands().isEmpty()) {
            String brandFilter = criteria.getBrands().stream()
                .map(this::escapeQuery)
                .collect(Collectors.joining(" OR ", "brand:(", ")"));
            queryParts.add(brandFilter);
        }
        
        // Price range filter
        if (criteria.getMinPrice() != null || criteria.getMaxPrice() != null) {
            String minPrice = criteria.getMinPrice() != null ? criteria.getMinPrice().toString() : "*";
            String maxPrice = criteria.getMaxPrice() != null ? criteria.getMaxPrice().toString() : "*";
            queryParts.add("price:[" + minPrice + " TO " + maxPrice + "]");
        }
        
        // Stock filter
        if (criteria.getInStock() != null) {
            queryParts.add("inStock:" + criteria.getInStock());
        }
        
        // Vendor filter
        if (StringUtils.hasText(criteria.getVendorId())) {
            queryParts.add("vendorId:" + escapeQuery(criteria.getVendorId()));
        }
        
        // Rating filter
        if (criteria.getMinRating() != null) {
            queryParts.add("rating:[" + criteria.getMinRating() + " TO *]");
        }
        
        // Attribute filters
        for (Map.Entry<String, List<String>> entry : criteria.getAttributeFilters().entrySet()) {
            String attributeName = entry.getKey();
            List<String> attributeValues = entry.getValue();
            if (!attributeValues.isEmpty()) {
                String attributeFilter = attributeValues.stream()
                    .map(this::escapeQuery)
                    .collect(Collectors.joining(" OR ", "attributes." + attributeName + ":(", ")"));
                queryParts.add(attributeFilter);
            }
        }
        
        // Tags filter
        if (!criteria.getTags().isEmpty()) {
            String tagFilter = criteria.getTags().stream()
                .map(this::escapeQuery)
                .collect(Collectors.joining(" OR ", "tags:(", ")"));
            queryParts.add(tagFilter);
        }
        
        return queryParts.isEmpty() ? "*:*" : String.join(" AND ", queryParts);
    }
    
    /**
     * Escape special characters in search query
     * @param query The query to escape
     * @return Escaped query
     */
    private String escapeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return "";
        }
        
        // Escape special Solr/Lucene characters
        return query.replaceAll("([+\\-!(){}\\[\\]^\"~*?:\\\\/])", "\\\\$1");
    }
    
    /**
     * Generate search suggestions based on query
     * @param query The search query
     * @param maxSuggestions Maximum number of suggestions
     * @return List of search suggestions
     */
    public List<String> generateSuggestions(String query, int maxSuggestions) {
        List<String> suggestions = new ArrayList<>();
        
        if (!StringUtils.hasText(query) || query.length() < 2) {
            return suggestions;
        }
        
        // This would typically query a suggestion index or database
        // For now, return some mock suggestions
        String lowerQuery = query.toLowerCase();
        
        // Mock suggestions - in real implementation, this would query a suggestions database
        List<String> mockSuggestions = Arrays.asList(
            "smartphone", "laptop", "headphones", "camera", "tablet",
            "watch", "shoes", "clothing", "books", "electronics",
            "home decor", "kitchen appliances", "sports equipment",
            "beauty products", "jewelry", "toys", "games"
        );
        
        suggestions = mockSuggestions.stream()
            .filter(suggestion -> suggestion.toLowerCase().contains(lowerQuery))
            .limit(maxSuggestions)
            .collect(Collectors.toList());
        
        return suggestions;
    }
    
    /**
     * Get available filter options for faceted search
     * @param criteria Current search criteria
     * @return Map of filter options
     */
    public Map<String, List<String>> getFilterOptions(SearchCriteria criteria) {
        Map<String, List<String>> filterOptions = new HashMap<>();
        
        // This would typically query the search index to get available facets
        // For now, return some mock filter options
        
        filterOptions.put("brands", Arrays.asList("Apple", "Samsung", "Sony", "LG", "Nike", "Adidas"));
        filterOptions.put("categories", Arrays.asList("Electronics", "Clothing", "Books", "Home & Garden", "Sports"));
        filterOptions.put("colors", Arrays.asList("Red", "Blue", "Green", "Black", "White", "Yellow"));
        filterOptions.put("sizes", Arrays.asList("XS", "S", "M", "L", "XL", "XXL"));
        filterOptions.put("materials", Arrays.asList("Cotton", "Polyester", "Leather", "Metal", "Plastic", "Wood"));
        
        return filterOptions;
    }
    
    /**
     * Get price range statistics for current search
     * @param criteria Search criteria
     * @return Price range information
     */
    public Map<String, BigDecimal> getPriceRange(SearchCriteria criteria) {
        Map<String, BigDecimal> priceRange = new HashMap<>();
        
        // This would typically query the search index for price statistics
        // For now, return mock data
        priceRange.put("min", new BigDecimal("10.00"));
        priceRange.put("max", new BigDecimal("50000.00"));
        priceRange.put("avg", new BigDecimal("2500.00"));
        
        return priceRange;
    }
    
    /**
     * Build sort clause for search query
     * @param criteria Search criteria
     * @return Sort clause
     */
    public String buildSortClause(SearchCriteria criteria) {
        if (criteria == null || !StringUtils.hasText(criteria.getSortBy())) {
            return "score desc"; // Default relevance sort
        }
        
        String sortBy = criteria.getSortBy();
        String direction = StringUtils.hasText(criteria.getSortDirection()) ? criteria.getSortDirection() : "desc";
        
        switch (sortBy.toLowerCase()) {
            case "price":
            case "price_asc":
                return "price asc";
            case "price_desc":
                return "price desc";
            case "name":
            case "name_asc":
                return "name asc";
            case "name_desc":
                return "name desc";
            case "rating":
            case "rating_desc":
                return "rating desc";
            case "rating_asc":
                return "rating asc";
            case "created":
            case "created_desc":
                return "createdAt desc";
            case "created_asc":
                return "createdAt asc";
            case "popularity":
            case "popularity_desc":
                return "popularity desc";
            case "relevance":
            default:
                return "score desc";
        }
    }
    
    /**
     * Parse search criteria from query parameters
     * @param queryParams Map of query parameters
     * @return SearchCriteria object
     */
    public SearchCriteria parseSearchCriteria(Map<String, String[]> queryParams) {
        SearchCriteria criteria = new SearchCriteria();
        
        // Parse query
        if (queryParams.containsKey("q")) {
            criteria.setQuery(queryParams.get("q")[0]);
        }
        
        // Parse categories
        if (queryParams.containsKey("category")) {
            List<Long> categoryIds = Arrays.stream(queryParams.get("category"))
                .map(Long::parseLong)
                .collect(Collectors.toList());
            criteria.setCategoryIds(categoryIds);
        }
        
        // Parse brands
        if (queryParams.containsKey("brand")) {
            criteria.setBrands(Arrays.asList(queryParams.get("brand")));
        }
        
        // Parse price range
        if (queryParams.containsKey("minPrice")) {
            criteria.setMinPrice(new BigDecimal(queryParams.get("minPrice")[0]));
        }
        if (queryParams.containsKey("maxPrice")) {
            criteria.setMaxPrice(new BigDecimal(queryParams.get("maxPrice")[0]));
        }
        
        // Parse stock filter
        if (queryParams.containsKey("inStock")) {
            criteria.setInStock(Boolean.parseBoolean(queryParams.get("inStock")[0]));
        }
        
        // Parse sort
        if (queryParams.containsKey("sort")) {
            criteria.setSortBy(queryParams.get("sort")[0]);
        }
        if (queryParams.containsKey("order")) {
            criteria.setSortDirection(queryParams.get("order")[0]);
        }
        
        // Parse vendor
        if (queryParams.containsKey("vendor")) {
            criteria.setVendorId(queryParams.get("vendor")[0]);
        }
        
        // Parse rating
        if (queryParams.containsKey("minRating")) {
            criteria.setMinRating(Double.parseDouble(queryParams.get("minRating")[0]));
        }
        
        return criteria;
    }
}