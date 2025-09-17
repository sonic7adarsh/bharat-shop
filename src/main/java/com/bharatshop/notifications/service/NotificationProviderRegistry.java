package com.bharatshop.notifications.service;

import com.bharatshop.notifications.enums.NotificationChannel;
import com.bharatshop.notifications.provider.NotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for managing notification providers.
 * Handles provider registration, selection, and availability checking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProviderRegistry {
    
    private final Map<NotificationChannel, List<NotificationProvider>> providersByChannel = new ConcurrentHashMap<>();
    private final Map<String, NotificationProvider> providersByName = new ConcurrentHashMap<>();
    
    @Autowired(required = false)
    private List<NotificationProvider> allProviders = new ArrayList<>();
    
    @PostConstruct
    public void initializeProviders() {
        log.info("Initializing notification provider registry with {} providers", allProviders.size());
        
        for (NotificationProvider provider : allProviders) {
            try {
                registerProvider(provider);
            } catch (Exception e) {
                log.error("Failed to register provider: {}", provider.getProviderName(), e);
            }
        }
        
        logRegisteredProviders();
    }
    
    /**
     * Register a notification provider
     */
    public void registerProvider(NotificationProvider provider) {
        String providerName = provider.getProviderName();
        List<NotificationChannel> supportedChannels = provider.getSupportedChannels();
        
        log.debug("Registering provider: {} for channels: {}", providerName, supportedChannels);
        
        // Register by name
        providersByName.put(providerName, provider);
        
        // Register by supported channels
        for (NotificationChannel channel : supportedChannels) {
            providersByChannel.computeIfAbsent(channel, k -> new ArrayList<>()).add(provider);
        }
        
        // Initialize provider if needed
        try {
            if (provider.isAvailable()) {
                provider.initialize(Collections.emptyMap());
                log.info("Successfully registered and initialized provider: {} for channels: {}", 
                        providerName, supportedChannels);
            } else {
                log.warn("Provider {} is not available, skipping initialization", providerName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize provider: {}", providerName, e);
        }
    }
    
    /**
     * Get provider for a specific channel (returns the first available provider)
     */
    public Optional<NotificationProvider> getProvider(NotificationChannel channel) {
        List<NotificationProvider> providers = providersByChannel.get(channel);
        
        if (providers == null || providers.isEmpty()) {
            log.debug("No providers found for channel: {}", channel);
            return Optional.empty();
        }
        
        // Return the first available provider
        for (NotificationProvider provider : providers) {
            if (provider.isAvailable()) {
                log.debug("Selected provider: {} for channel: {}", provider.getProviderName(), channel);
                return Optional.of(provider);
            }
        }
        
        log.warn("No available providers found for channel: {}", channel);
        return Optional.empty();
    }
    
    /**
     * Get provider by name
     */
    public Optional<NotificationProvider> getProvider(String providerName) {
        NotificationProvider provider = providersByName.get(providerName);
        
        if (provider != null && provider.isAvailable()) {
            return Optional.of(provider);
        }
        
        log.debug("Provider not found or not available: {}", providerName);
        return Optional.empty();
    }
    
    /**
     * Get all providers for a channel
     */
    public List<NotificationProvider> getAllProviders(NotificationChannel channel) {
        List<NotificationProvider> providers = providersByChannel.get(channel);
        return providers != null ? new ArrayList<>(providers) : Collections.emptyList();
    }
    
    /**
     * Get all available providers for a channel
     */
    public List<NotificationProvider> getAvailableProviders(NotificationChannel channel) {
        return getAllProviders(channel).stream()
            .filter(NotificationProvider::isAvailable)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all registered providers
     */
    public List<NotificationProvider> getAllProviders() {
        return new ArrayList<>(providersByName.values());
    }
    
    /**
     * Get all available providers
     */
    public List<NotificationProvider> getAvailableProviders() {
        return providersByName.values().stream()
            .filter(NotificationProvider::isAvailable)
            .collect(Collectors.toList());
    }
    
    /**
     * Check if any provider is available for a channel
     */
    public boolean hasAvailableProvider(NotificationChannel channel) {
        return getProvider(channel).isPresent();
    }
    
    /**
     * Get supported channels
     */
    public Set<NotificationChannel> getSupportedChannels() {
        return providersByChannel.keySet().stream()
            .filter(this::hasAvailableProvider)
            .collect(Collectors.toSet());
    }
    
    /**
     * Get provider statistics
     */
    public ProviderStatistics getStatistics() {
        Map<NotificationChannel, Integer> providerCountByChannel = new HashMap<>();
        Map<NotificationChannel, Integer> availableProviderCountByChannel = new HashMap<>();
        
        for (NotificationChannel channel : NotificationChannel.values()) {
            List<NotificationProvider> allChannelProviders = getAllProviders(channel);
            List<NotificationProvider> availableChannelProviders = getAvailableProviders(channel);
            
            providerCountByChannel.put(channel, allChannelProviders.size());
            availableProviderCountByChannel.put(channel, availableChannelProviders.size());
        }
        
        return ProviderStatistics.builder()
            .totalProviders(providersByName.size())
            .availableProviders((int) providersByName.values().stream().filter(NotificationProvider::isAvailable).count())
            .providerCountByChannel(providerCountByChannel)
            .availableProviderCountByChannel(availableProviderCountByChannel)
            .supportedChannels(getSupportedChannels())
            .build();
    }
    
    /**
     * Refresh provider availability
     */
    public void refreshProviderAvailability() {
        log.info("Refreshing provider availability for {} providers", providersByName.size());
        
        for (NotificationProvider provider : providersByName.values()) {
            try {
                boolean wasAvailable = provider.isAvailable();
                // Force availability check (implementation dependent)
                boolean isAvailable = provider.isAvailable();
                
                if (wasAvailable != isAvailable) {
                    log.info("Provider {} availability changed: {} -> {}", 
                            provider.getProviderName(), wasAvailable, isAvailable);
                }
            } catch (Exception e) {
                log.error("Error checking availability for provider: {}", provider.getProviderName(), e);
            }
        }
    }
    
    /**
     * Unregister a provider
     */
    public void unregisterProvider(String providerName) {
        NotificationProvider provider = providersByName.remove(providerName);
        
        if (provider != null) {
            // Remove from channel mappings
            for (List<NotificationProvider> channelProviders : providersByChannel.values()) {
                channelProviders.remove(provider);
            }
            
            // Shutdown provider
            try {
                provider.shutdown();
                log.info("Unregistered and shutdown provider: {}", providerName);
            } catch (Exception e) {
                log.error("Error shutting down provider: {}", providerName, e);
            }
        }
    }
    
    /**
     * Shutdown all providers
     */
    public void shutdownAllProviders() {
        log.info("Shutting down all {} providers", providersByName.size());
        
        for (NotificationProvider provider : providersByName.values()) {
            try {
                provider.shutdown();
                log.debug("Shutdown provider: {}", provider.getProviderName());
            } catch (Exception e) {
                log.error("Error shutting down provider: {}", provider.getProviderName(), e);
            }
        }
        
        providersByName.clear();
        providersByChannel.clear();
    }
    
    /**
     * Log registered providers
     */
    private void logRegisteredProviders() {
        log.info("Provider registry initialized with {} providers:", providersByName.size());
        
        for (Map.Entry<NotificationChannel, List<NotificationProvider>> entry : providersByChannel.entrySet()) {
            NotificationChannel channel = entry.getKey();
            List<String> providerNames = entry.getValue().stream()
                .map(NotificationProvider::getProviderName)
                .collect(Collectors.toList());
            
            log.info("  {}: {}", channel, providerNames);
        }
    }
    
    /**
     * Provider statistics data class
     */
    @lombok.Builder
    @lombok.Data
    public static class ProviderStatistics {
        private int totalProviders;
        private int availableProviders;
        private Map<NotificationChannel, Integer> providerCountByChannel;
        private Map<NotificationChannel, Integer> availableProviderCountByChannel;
        private Set<NotificationChannel> supportedChannels;
    }
}