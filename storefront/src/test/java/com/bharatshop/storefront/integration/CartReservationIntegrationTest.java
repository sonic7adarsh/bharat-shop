package com.bharatshop.storefront.integration;

import com.bharatshop.shared.entity.*;
import com.bharatshop.shared.repository.*;
import com.bharatshop.shared.service.ReservationService;
import com.bharatshop.storefront.dto.CartItemRequest;
import com.bharatshop.storefront.repository.StorefrontOrderRepository;
import com.bharatshop.storefront.repository.StorefrontProductRepository;
import com.bharatshop.storefront.service.CartService;
import com.bharatshop.storefront.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for cart and reservation system
 * Tests the complete flow from cart operations to order creation with real database
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class CartReservationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    @Qualifier("storefrontProductRepository")
    private StorefrontProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    @Qualifier("storefrontOrderRepository")
    private StorefrontOrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID tenantId;
    private User testUser;
    private Product testProduct;
    private ProductVariant testVariant;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        
        // Create test user
        testUser = User.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        // Create test product
        testProduct = Product.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(99.99))
                .build();
        productRepository.save(testProduct);

        // Create test product variant with limited stock
        testVariant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .productId(testProduct.getId())
                .sku("TEST-SKU-001")
                .stock(10) // Limited stock for testing
                .reservedStock(0)
                .price(BigDecimal.valueOf(99.99))
                .build();
        productVariantRepository.save(testVariant);

        // Create test cart
        testCart = Cart.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .userId(testUser.getId())
                .build();
        cartRepository.save(testCart);
    }

    @Test
    void testAddToCartWithStockValidation() {
        // Test: Add item to cart within stock limits
        CartItemRequest request = new CartItemRequest();
        request.setProductId(testProduct.getId());
        request.setVariantId(testVariant.getId());
        request.setQuantity(5);

        // Should succeed - within stock limits
        assertDoesNotThrow(() -> {
            cartService.addItemToCart(testCart.getId(), request, tenantId);
        });

        // Verify cart item was created
        List<CartItem> cartItems = cartItemRepository.findByCartId(testCart.getId());
        assertEquals(1, cartItems.size());
        assertEquals(5, cartItems.get(0).getQuantity());
        assertEquals(testVariant.getId(), cartItems.get(0).getVariantId());
    }

    @Test
    void testAddToCartExceedsStock() {
        // Test: Try to add more items than available stock
        CartItemRequest request = new CartItemRequest();
        request.setProductId(testProduct.getId());
        request.setVariantId(testVariant.getId());
        request.setQuantity(15); // Exceeds available stock of 10

        // Should fail - exceeds stock
        assertThrows(IllegalArgumentException.class, () -> {
            cartService.addItemToCart(testCart.getId(), request, tenantId);
        });

        // Verify no cart item was created
        List<CartItem> cartItems = cartItemRepository.findByCartId(testCart.getId());
        assertEquals(0, cartItems.size());
    }

    @Test
    void testConcurrentCartOperations() throws InterruptedException {
        // Test: Multiple users trying to add the same product to cart concurrently
        int numberOfUsers = 15;
        int quantityPerUser = 1;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfUsers);
        
        AtomicInteger successfulAdditions = new AtomicInteger(0);
        AtomicInteger failedAdditions = new AtomicInteger(0);

        // Create multiple carts for different users
        for (int i = 0; i < numberOfUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    // Create cart for this user
                    Cart userCart = Cart.builder()
                            .id(UUID.randomUUID())
                            .tenantId(tenantId)
                            .userId(UUID.randomUUID())
                            .build();
                    cartRepository.save(userCart);

                    CartItemRequest request = new CartItemRequest();
                    request.setProductId(testProduct.getId());
                    request.setVariantId(testVariant.getId());
                    request.setQuantity(quantityPerUser);

                    cartService.addItemToCart(userCart.getId(), request, tenantId);
                    successfulAdditions.incrementAndGet();
                    
                } catch (Exception e) {
                    failedAdditions.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all operations
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "All cart operations should complete");
        
        executor.shutdown();

        // Verify: Only 10 additions should succeed (matching available stock)
        assertEquals(10, successfulAdditions.get(), 
                "Should have exactly 10 successful additions matching available stock");
        assertEquals(5, failedAdditions.get(), 
                "Should have 5 failed additions due to insufficient stock");
    }

    @Test
    void testCheckoutWithReservations() {
        // Setup: Add items to cart
        CartItemRequest request = new CartItemRequest();
        request.setProductId(testProduct.getId());
        request.setVariantId(testVariant.getId());
        request.setQuantity(3);
        
        cartService.addItemToCart(testCart.getId(), request, tenantId);

        // Test: Checkout process should create reservations
        assertDoesNotThrow(() -> {
            Order order = orderService.createOrderFromCart(testCart.getId(), tenantId);
            assertNotNull(order);
            
            // Verify reservations were created
            List<Reservation> reservations = reservationRepository
                    .findByOrderIdAndTenantId(order.getId(), tenantId);
            assertEquals(1, reservations.size());
            assertEquals(3, reservations.get(0).getQuantity());
            assertEquals(testVariant.getId(), reservations.get(0).getProductVariantId());
            assertEquals(Reservation.ReservationStatus.ACTIVE, reservations.get(0).getStatus());
        });
    }

    @Test
    void testCheckoutFailureReleasesReservations() {
        // Setup: Add items to cart
        CartItemRequest request = new CartItemRequest();
        request.setProductId(testProduct.getId());
        request.setVariantId(testVariant.getId());
        request.setQuantity(2);
        
        cartService.addItemToCart(testCart.getId(), request, tenantId);

        // Test: Simulate checkout failure (e.g., payment failure)
        try {
            Order order = orderService.createOrderFromCart(testCart.getId(), tenantId);
            
            // Simulate payment failure
            order.setStatus(Order.OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            
            // Process payment failure - should release reservations
            orderService.processPayment(order.getId(), false, tenantId);
            
            // Verify reservations were released
            List<Reservation> reservations = reservationRepository
                    .findByOrderIdAndTenantId(order.getId(), tenantId);
            assertTrue(reservations.isEmpty() || 
                    reservations.stream().allMatch(r -> r.getStatus() == Reservation.ReservationStatus.RELEASED),
                    "All reservations should be released after payment failure");
            
        } catch (Exception e) {
            fail("Checkout process should handle failures gracefully");
        }
    }

    @Test
    void testReservationExpiry() throws InterruptedException {
        // Test: Create reservation with short expiry
        Reservation reservation = reservationService.reserveStock(
                tenantId, testVariant.getId(), 2, 0); // 0 minutes = immediate expiry
        
        assertNotNull(reservation);
        assertEquals(Reservation.ReservationStatus.ACTIVE, reservation.getStatus());
        
        // Manually set expiry to past for testing
        reservation.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        reservationRepository.save(reservation);
        
        // Run cleanup
        int releasedCount = reservationService.cleanupExpiredReservations();
        
        // Verify reservation was released
        assertEquals(1, releasedCount, "Should have released 1 expired reservation");
        
        // Verify stock is available again
        int availableStock = reservationService.getAvailableStock(tenantId, testVariant.getId());
        assertEquals(10, availableStock, "Stock should be fully available after expiry cleanup");
    }

    @Test
    void testConcurrentCheckoutPreventsOversell() throws InterruptedException {
        // Setup: Create multiple carts with items that would exceed stock if all processed
        int numberOfCarts = 8;
        int itemsPerCart = 2; // 8 * 2 = 16 items, but only 10 available
        
        List<Cart> carts = new ArrayList<>();
        for (int i = 0; i < numberOfCarts; i++) {
            Cart cart = Cart.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .userId(UUID.randomUUID())
                    .build();
            cartRepository.save(cart);
            
            CartItemRequest request = new CartItemRequest();
            request.setProductId(testProduct.getId());
            request.setVariantId(testVariant.getId());
            request.setQuantity(itemsPerCart);
            
            cartService.addItemToCart(cart.getId(), request, tenantId);
            carts.add(cart);
        }

        // Test: Concurrent checkout operations
        ExecutorService executor = Executors.newFixedThreadPool(numberOfCarts);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numberOfCarts);
        
        AtomicInteger successfulCheckouts = new AtomicInteger(0);
        AtomicInteger failedCheckouts = new AtomicInteger(0);

        for (Cart cart : carts) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    Order order = orderService.createOrderFromCart(cart.getId(), tenantId);
                    if (order != null) {
                        successfulCheckouts.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    failedCheckouts.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all checkouts
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "All checkout operations should complete");
        
        executor.shutdown();

        // Verify: Only 5 checkouts should succeed (10 stock / 2 items per cart)
        assertEquals(5, successfulCheckouts.get(), 
                "Should have exactly 5 successful checkouts");
        assertEquals(3, failedCheckouts.get(), 
                "Should have 3 failed checkouts due to insufficient stock");
        
        // Verify total reserved stock doesn't exceed available
        List<Reservation> activeReservations = reservationRepository
                .findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, Reservation.ReservationStatus.ACTIVE);
        
        int totalReserved = activeReservations.stream()
                .mapToInt(Reservation::getQuantity)
                .sum();
        
        assertTrue(totalReserved <= testVariant.getStock(), 
                "Total reserved stock should not exceed available stock");
    }

    @Test
    void testOrderCancellationReleasesReservations() {
        // Setup: Create order with reservations
        CartItemRequest request = new CartItemRequest();
        request.setProductId(testProduct.getId());
        request.setVariantId(testVariant.getId());
        request.setQuantity(4);
        
        cartService.addItemToCart(testCart.getId(), request, tenantId);
        Order order = orderService.createOrderFromCart(testCart.getId(), tenantId);
        
        // Verify reservations exist
        List<Reservation> reservations = reservationRepository
                .findByOrderIdAndTenantId(order.getId(), tenantId);
        assertEquals(1, reservations.size());
        assertEquals(Reservation.ReservationStatus.ACTIVE, reservations.get(0).getStatus());
        
        // Test: Cancel order
        orderService.cancelOrder(order.getId(), tenantId);
        
        // Verify reservations were released
        reservations = reservationRepository.findByOrderIdAndTenantId(order.getId(), tenantId);
        assertTrue(reservations.isEmpty() || 
                reservations.stream().allMatch(r -> r.getStatus() == Reservation.ReservationStatus.RELEASED),
                "All reservations should be released after order cancellation");
        
        // Verify stock is available again
        int availableStock = reservationService.getAvailableStock(tenantId, testVariant.getId());
        assertEquals(10, availableStock, "Stock should be fully available after order cancellation");
    }

    @Test
    void testPaymentSuccessCommitsReservations() {
        // Setup: Create order with reservations
        CartItemRequest request = new CartItemRequest();
        request.setProductId(testProduct.getId());
        request.setVariantId(testVariant.getId());
        request.setQuantity(3);
        
        cartService.addItemToCart(testCart.getId(), request, tenantId);
        Order order = orderService.createOrderFromCart(testCart.getId(), tenantId);
        
        // Verify initial reservations
        List<Reservation> reservations = reservationRepository
                .findByOrderIdAndTenantId(order.getId(), tenantId);
        assertEquals(1, reservations.size());
        assertEquals(Reservation.ReservationStatus.ACTIVE, reservations.get(0).getStatus());
        
        // Test: Process successful payment
        orderService.processPayment(order.getId(), true, tenantId);
        
        // Verify reservations were committed (converted to actual stock reduction)
        reservations = reservationRepository.findByOrderIdAndTenantId(order.getId(), tenantId);
        assertTrue(reservations.isEmpty() || 
                reservations.stream().allMatch(r -> r.getStatus() == Reservation.ReservationStatus.COMMITTED),
                "All reservations should be committed after successful payment");
        
        // Verify actual stock was reduced
        ProductVariant updatedVariant = productVariantRepository.findById(testVariant.getId()).orElse(null);
        assertNotNull(updatedVariant);
        assertEquals(7, updatedVariant.getStock(), "Actual stock should be reduced after payment");
    }
}