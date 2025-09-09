package com.bharatshop.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceListResponse {
    
    private List<InvoiceDto> invoices;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
    
    public static InvoiceListResponse of(List<InvoiceDto> invoices, long totalElements, 
                                       int totalPages, int currentPage, int pageSize) {
        return InvoiceListResponse.builder()
            .invoices(invoices)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .currentPage(currentPage)
            .pageSize(pageSize)
            .hasNext(currentPage < totalPages - 1)
            .hasPrevious(currentPage > 0)
            .build();
    }
}