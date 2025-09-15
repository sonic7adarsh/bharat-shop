package com.bharatshop.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class InvoiceListResponse {
    
    public List<InvoiceDto> invoices;
    public long totalElements;
    public int totalPages;
    public int currentPage;
    public int pageSize;
    public boolean hasNext;
    public boolean hasPrevious;
    
    public static InvoiceListResponse of(List<InvoiceDto> invoices, long totalElements, 
                                       int totalPages, int currentPage, int pageSize) {
        InvoiceListResponse response = new InvoiceListResponse();
        response.invoices = invoices;
        response.totalElements = totalElements;
        response.totalPages = totalPages;
        response.currentPage = currentPage;
        response.pageSize = pageSize;
        response.hasNext = currentPage < totalPages - 1;
        response.hasPrevious = currentPage > 0;
        return response;
    }
}