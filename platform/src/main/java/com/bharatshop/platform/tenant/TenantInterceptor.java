package com.bharatshop.platform.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);
    
    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String TENANT_PARAM = "tenantId";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantIdStr = extractTenantId(request);
        
        if (StringUtils.hasText(tenantIdStr)) {
            try {
                Long tenantId = Long.parseLong(tenantIdStr);
                TenantContext.setTenantId(tenantId);
                log.debug("Tenant context set for request: {} - Tenant: {}", request.getRequestURI(), tenantId);
            } catch (NumberFormatException e) {
                log.warn("Invalid tenant ID format in request: {} - Tenant: {}", request.getRequestURI(), tenantIdStr);
                return false;
            }
        } else {
            log.warn("No tenant ID found in request: {}", request.getRequestURI());
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
    
    private String extractTenantId(HttpServletRequest request) {
        // Try header first
        String tenantId = request.getHeader(TENANT_HEADER);
        
        // Fallback to query parameter
        if (!StringUtils.hasText(tenantId)) {
            tenantId = request.getParameter(TENANT_PARAM);
        }
        
        // Could also extract from subdomain or path if needed
        // String host = request.getServerName();
        // if (host.contains(".")) {
        //     tenantId = host.substring(0, host.indexOf("."));
        // }
        
        return tenantId;
    }
}