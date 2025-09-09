package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.Orders;
import com.bharatshop.shared.exception.BusinessException;
import com.bharatshop.shared.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for OrderStateMachineService covering all allowed and denied transitions.
 * Tests the state machine logic, validation guards, and event publishing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Order State Machine Service Tests")
class OrderStateMachineServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderStateMachineService orderStateMachineService;

    private Orders testOrder;
    private final Long ORDER_ID = 1L;
    private final Long TENANT_ID = 100L;
    private final String TEST_REASON = "Test transition reason";

    @BeforeEach
    void setUp() {
        testOrder = createTestOrder();
    }

    @Nested
    @DisplayName("Main Flow Transitions")
    class MainFlowTransitions {

        @Test
        @DisplayName("Should transition from PENDING_PAYMENT to CONFIRMED")
        void shouldTransitionFromPendingPaymentToConfirmed() {
            // Given
            testOrder.setStatus(Orders.OrderStatus.PENDING_PAYMENT);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Orders.class))).thenReturn(testOrder);

            // When
            Orders result = orderStateMachineService.transitionToConfirmed(ORDER_ID, TENANT_ID, TEST_REASON);

            // Then
            assertThat(result.getStatus()).isEqualTo(Orders.OrderStatus.CONFIRMED);
            assertThat(result.getConfirmedAt()).isNotNull();
            verify(orderRepository).save(testOrder);
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Should transition from CONFIRMED to PACKED")
        void shouldTransitionFromConfirmedToPacked() {
            // Given
            testOrder.setStatus(Orders.OrderStatus.CONFIRMED);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Orders.class))).thenReturn(testOrder);

            // When
            Orders result = orderStateMachineService.transitionToPacked(ORDER_ID, TENANT_ID, TEST_REASON);

            // Then
            assertThat(result.getStatus()).isEqualTo(Orders.OrderStatus.PACKED);
            verify(orderRepository).save(testOrder);
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Should transition from PACKED to SHIPPED")
        void shouldTransitionFromPackedToShipped() {
            // Given
            testOrder.setStatus(Orders.OrderStatus.PACKED);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Orders.class))).thenReturn(testOrder);

            // When
            Orders result = orderStateMachineService.transitionToShipped(ORDER_ID, TENANT_ID, TEST_REASON);

            // Then
            assertThat(result.getStatus()).isEqualTo(Orders.OrderStatus.SHIPPED);
            verify(orderRepository).save(testOrder);
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Should transition from SHIPPED to DELIVERED")
        void shouldTransitionFromShippedToDelivered() {
            // Given
            testOrder.setStatus(Orders.OrderStatus.SHIPPED);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Orders.class))).thenReturn(testOrder);

            // When
            Orders result = orderStateMachineService.transitionToDelivered(ORDER_ID, TENANT_ID, TEST_REASON);

            // Then
            assertThat(result.getStatus()).isEqualTo(Orders.OrderStatus.DELIVERED);
            verify(orderRepository).save(testOrder);
            verify(eventPublisher).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("Branch Transitions - Cancellation")
    class CancellationTransitions {

        @Test
        @DisplayName("Should allow cancellation from PENDING_PAYMENT")
        void shouldAllowCancellationFromPendingPayment() {
            // Given
            testOrder.setStatus(Orders.OrderStatus.PENDING_PAYMENT);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Orders.class))).thenReturn(testOrder);

            // When
            Orders result = orderStateMachineService.transitionToCancelled(ORDER_ID, TENANT_ID, TEST_REASON);

            // Then
            assertThat(result.getStatus()).isEqualTo(Orders.OrderStatus.CANCELLED);
            assertThat(result.getCancellationReason()).isEqualTo(TEST_REASON);
            verify(orderRepository).save(testOrder);
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Should allow cancellation from CONFIRMED")
        void shouldAllowCancellationFromConfirmed() {
            // Given
            testOrder.setStatus(Orders.OrderStatus.CONFIRMED);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Orders.class))).thenReturn(testOrder);

            // When
            Orders result = orderStateMachineService.transitionToCancelled(ORDER_ID, TENANT_ID, TEST_REASON);

            // Then
            assertThat(result.getStatus()).isEqualTo(Orders.OrderStatus.CANCELLED);
            verify(orderRepository).save(testOrder);
        }

        @Test
        @DisplayName("Should deny cancellation from DELIVERED")
        void shouldDenyCancellationFromDelivered() {
            // Given
            testOrder.setStatus(Orders.OrderStatus.DELIVERED);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));

            // When & Then
            assertThatThrownBy(() -> 
                    orderStateMachineService.transitionToCancelled(ORDER_ID, TENANT_ID, TEST_REASON))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid state transition");

            verify(orderRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should deny cancellation from terminal states")
        void shouldDenyCancellationFromTerminalStates() {
            // Test each terminal state
            Orders.OrderStatus[] terminalStates = {
                    Orders.OrderStatus.CANCELLED,
                    Orders.OrderStatus.REFUNDED,
                    Orders.OrderStatus.RETURNED
            };

            for (Orders.OrderStatus terminalStatus : terminalStates) {
                // Given
                testOrder.setStatus(terminalStatus);
                when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                        .thenReturn(Optional.of(testOrder));

                // When & Then
                assertThatThrownBy(() -> 
                        orderStateMachineService.transitionToCancelled(ORDER_ID, TENANT_ID, TEST_REASON))
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("Invalid state transition");
            }
        }
    }

    @Nested
    @DisplayName("Branch Transitions - Returns")
    class ReturnTransitions {

        @Test
        @DisplayName("Should allow return request from DELIVERED")
        void shouldAllowReturnRequestFromDelivered() {
            // Given
            testOrder.setStatus(Orders.OrderStatus.DELIVERED);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Orders.class))).thenReturn(testOrder);

            // When
            Orders result = orderStateMachineService.transitionToReturnRequested(ORDER_ID, TENANT_ID, TEST_REASON);

            // Then
            assertThat(result.getStatus()).isEqualTo(Orders.OrderStatus.RETURN_REQUESTED);
            verify(orderRepository).save(testOrder);
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Should deny return request from PENDING_PAYMENT")
        void shouldDenyReturnRequestFromPendingPayment() {
            // Given
            testOrder.setStatus(Orders.OrderStatus.PENDING_PAYMENT);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));

            // When & Then
            assertThatThrownBy(() -> 
                    orderStateMachineService.transitionToReturnRequested(ORDER_ID, TENANT_ID, TEST_REASON))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid state transition");
        }

        @Test
        @DisplayName("Should transition from RETURN_REQUESTED to RETURNED")
        void shouldTransitionFromReturnRequestedToReturned() {
            // Given
            testOrder.setStatus(Orders.OrderStatus.RETURN_REQUESTED);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Orders.class))).thenReturn(testOrder);

            // When
            Orders result = orderStateMachineService.transitionToReturned(ORDER_ID, TENANT_ID, TEST_REASON);

            // Then
            assertThat(result.getStatus()).isEqualTo(Orders.OrderStatus.RETURNED);
            verify(orderRepository).save(testOrder);
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Should transition from RETURNED to REFUNDED")
        void shouldTransitionFromReturnedToRefunded() {
            // Given
            testOrder.setStatus(Orders.OrderStatus.RETURNED);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Orders.class))).thenReturn(testOrder);

            // When
            Orders result = orderStateMachineService.transitionToRefunded(ORDER_ID, TENANT_ID, TEST_REASON);

            // Then
            assertThat(result.getStatus()).isEqualTo(Orders.OrderStatus.REFUNDED);
            verify(orderRepository).save(testOrder);
            verify(eventPublisher).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("Invalid Transitions")
    class InvalidTransitions {

        @Test
        @DisplayName("Should deny skipping states in main flow")
        void shouldDenySkippingStatesInMainFlow() {
            // Given - trying to go from PENDING_PAYMENT directly to PACKED
            testOrder.setStatus(Orders.OrderStatus.PENDING_PAYMENT);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));

            // When & Then
            assertThatThrownBy(() -> 
                    orderStateMachineService.transitionToPacked(ORDER_ID, TENANT_ID, TEST_REASON))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid state transition");
        }

        @Test
        @DisplayName("Should deny backward transitions in main flow")
        void shouldDenyBackwardTransitionsInMainFlow() {
            // Given - trying to go from SHIPPED back to CONFIRMED
            testOrder.setStatus(Orders.OrderStatus.SHIPPED);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));

            // When & Then
            assertThatThrownBy(() -> 
                    orderStateMachineService.transitionToConfirmed(ORDER_ID, TENANT_ID, TEST_REASON))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid state transition");
        }

        @Test
        @DisplayName("Should deny transitions from terminal states")
        void shouldDenyTransitionsFromTerminalStates() {
            // Given
            testOrder.setStatus(Orders.OrderStatus.REFUNDED);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));

            // When & Then - try various transitions from terminal state
            assertThatThrownBy(() -> 
                    orderStateMachineService.transitionToConfirmed(ORDER_ID, TENANT_ID, TEST_REASON))
                    .isInstanceOf(BusinessException.class);

            assertThatThrownBy(() -> 
                    orderStateMachineService.transitionToPacked(ORDER_ID, TENANT_ID, TEST_REASON))
                    .isInstanceOf(BusinessException.class);

            assertThatThrownBy(() -> 
                    orderStateMachineService.transitionToShipped(ORDER_ID, TENANT_ID, TEST_REASON))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("Validation and Error Handling")
    class ValidationAndErrorHandling {

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            // Given
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> 
                    orderStateMachineService.transitionToConfirmed(ORDER_ID, TENANT_ID, TEST_REASON))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("Should validate tenant access")
        void shouldValidateTenantAccess() {
            // Given - order exists but for different tenant
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> 
                    orderStateMachineService.transitionToConfirmed(ORDER_ID, TENANT_ID, TEST_REASON))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Should handle null reason gracefully")
        void shouldHandleNullReasonGracefully() {
            // Given
            testOrder.setStatus(Orders.OrderStatus.PENDING_PAYMENT);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Orders.class))).thenReturn(testOrder);

            // When
            Orders result = orderStateMachineService.transitionToConfirmed(ORDER_ID, TENANT_ID, null);

            // Then
            assertThat(result.getStatus()).isEqualTo(Orders.OrderStatus.CONFIRMED);
            verify(orderRepository).save(testOrder);
        }
    }

    @Nested
    @DisplayName("Event Publishing")
    class EventPublishing {

        @Test
        @DisplayName("Should publish events for all successful transitions")
        void shouldPublishEventsForAllSuccessfulTransitions() {
            // Given
            testOrder.setStatus(Orders.OrderStatus.PENDING_PAYMENT);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Orders.class))).thenReturn(testOrder);

            // When
            orderStateMachineService.transitionToConfirmed(ORDER_ID, TENANT_ID, TEST_REASON);

            // Then
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Should not publish events for failed transitions")
        void shouldNotPublishEventsForFailedTransitions() {
            // Given
            testOrder.setStatus(Orders.OrderStatus.DELIVERED);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                    .thenReturn(Optional.of(testOrder));

            // When & Then
            assertThatThrownBy(() -> 
                    orderStateMachineService.transitionToConfirmed(ORDER_ID, TENANT_ID, TEST_REASON))
                    .isInstanceOf(BusinessException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("State Machine Logic Validation")
    class StateMachineLogicValidation {

        @Test
        @DisplayName("Should validate all allowed transitions from OrderStatus enum")
        void shouldValidateAllAllowedTransitionsFromOrderStatusEnum() {
            // Test that the service respects the OrderStatus.canTransitionTo() logic
            
            // PENDING_PAYMENT can transition to CONFIRMED or CANCELLED
            testOrder.setStatus(Orders.OrderStatus.PENDING_PAYMENT);
            assertThat(testOrder.getStatus().canTransitionTo(Orders.OrderStatus.CONFIRMED)).isTrue();
            assertThat(testOrder.getStatus().canTransitionTo(Orders.OrderStatus.CANCELLED)).isTrue();
            assertThat(testOrder.getStatus().canTransitionTo(Orders.OrderStatus.PACKED)).isFalse();
            
            // CONFIRMED can transition to PACKED or CANCELLED
            testOrder.setStatus(Orders.OrderStatus.CONFIRMED);
            assertThat(testOrder.getStatus().canTransitionTo(Orders.OrderStatus.PACKED)).isTrue();
            assertThat(testOrder.getStatus().canTransitionTo(Orders.OrderStatus.CANCELLED)).isTrue();
            assertThat(testOrder.getStatus().canTransitionTo(Orders.OrderStatus.SHIPPED)).isFalse();
            
            // DELIVERED can transition to RETURN_REQUESTED
            testOrder.setStatus(Orders.OrderStatus.DELIVERED);
            assertThat(testOrder.getStatus().canTransitionTo(Orders.OrderStatus.RETURN_REQUESTED)).isTrue();
            assertThat(testOrder.getStatus().canTransitionTo(Orders.OrderStatus.CANCELLED)).isFalse();
        }

        @Test
        @DisplayName("Should validate terminal state behavior")
        void shouldValidateTerminalStateBehavior() {
            Orders.OrderStatus[] terminalStates = {
                    Orders.OrderStatus.CANCELLED,
                    Orders.OrderStatus.REFUNDED,
                    Orders.OrderStatus.RETURNED
            };

            for (Orders.OrderStatus terminalStatus : terminalStates) {
                assertThat(terminalStatus.isTerminal()).isTrue();
                
                // Terminal states should not allow any transitions
                for (Orders.OrderStatus targetStatus : Orders.OrderStatus.values()) {
                    if (targetStatus != terminalStatus) {
                        assertThat(terminalStatus.canTransitionTo(targetStatus))
                                .as("Terminal state %s should not transition to %s", terminalStatus, targetStatus)
                                .isFalse();
                    }
                }
            }
        }
    }

    // Helper methods

    private Orders createTestOrder() {
        Orders order = new Orders();
        order.setId(ORDER_ID);
        order.setTenantId(TENANT_ID);
        order.setCustomerId(1L);
        order.setStatus(Orders.OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(BigDecimal.valueOf(100.00));
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }
}