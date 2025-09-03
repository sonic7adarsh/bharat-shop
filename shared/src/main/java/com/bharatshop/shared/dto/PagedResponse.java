package com.bharatshop.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response wrapper for list endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedResponse<T> {
    
    // Manual builder method to fix Lombok issue
    public static <T> PagedResponseBuilder<T> builder() {
        return new PagedResponseBuilder<T>();
    }
    
    // Manual builder class to fix Lombok issue
    public static class PagedResponseBuilder<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
        private boolean hasNext;
        private boolean hasPrevious;
        
        public PagedResponseBuilder<T> content(List<T> content) {
            this.content = content;
            return this;
        }
        
        public PagedResponseBuilder<T> page(int page) {
            this.page = page;
            return this;
        }
        
        public PagedResponseBuilder<T> size(int size) {
            this.size = size;
            return this;
        }
        
        public PagedResponseBuilder<T> totalElements(long totalElements) {
            this.totalElements = totalElements;
            return this;
        }
        
        public PagedResponseBuilder<T> totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }
        
        public PagedResponseBuilder<T> first(boolean first) {
            this.first = first;
            return this;
        }
        
        public PagedResponseBuilder<T> last(boolean last) {
            this.last = last;
            return this;
        }
        
        public PagedResponseBuilder<T> hasNext(boolean hasNext) {
            this.hasNext = hasNext;
            return this;
        }
        
        public PagedResponseBuilder<T> hasPrevious(boolean hasPrevious) {
            this.hasPrevious = hasPrevious;
            return this;
        }
        
        public PagedResponse<T> build() {
            PagedResponse<T> response = new PagedResponse<>();
            response.content = this.content;
            response.page = this.page;
            response.size = this.size;
            response.totalElements = this.totalElements;
            response.totalPages = this.totalPages;
            response.first = this.first;
            response.last = this.last;
            response.hasNext = this.hasNext;
            response.hasPrevious = this.hasPrevious;
            return response;
        }
    }
    
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;
    
    // Static factory method
    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        return PagedResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .build();
    }
}