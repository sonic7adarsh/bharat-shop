package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.Product;
import com.bharatshop.shared.entity.TaxRate;
import com.bharatshop.shared.repository.TaxRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Service for calculating prices with GST tax components
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PriceCalculationService {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PriceCalculationService.class);

    private final TaxRateRepository taxRateRepository;

    /**
     * Calculate price breakdown with tax components
     */
    public PriceBreakdown calculatePrice(Product product, BigDecimal basePrice, 
                                       String customerStateCode, String merchantStateCode, 
                                       Long tenantId) {
        
        if (product.getTaxPreference() == Product.TaxPreference.EXEMPT) {
            return PriceBreakdown.builder()
                    .basePrice(basePrice)
                    .netPrice(basePrice)
                    .cgstAmount(BigDecimal.ZERO)
                    .sgstAmount(BigDecimal.ZERO)
                    .igstAmount(BigDecimal.ZERO)
                    .cessAmount(BigDecimal.ZERO)
                    .totalTaxAmount(BigDecimal.ZERO)
                    .totalPrice(basePrice)
                    .taxType(TaxRate.TaxType.INTRA_STATE)
                    .build();
        }

        // Determine tax type based on state codes
        TaxRate.TaxType taxType = customerStateCode.equals(merchantStateCode) 
                ? TaxRate.TaxType.INTRA_STATE 
                : TaxRate.TaxType.INTER_STATE;

        // Find applicable tax rate
        Optional<TaxRate> taxRateOpt = taxRateRepository.findByTenantIdAndHsnCodeAndStateCodeAndTaxTypeAndStatus(
                tenantId, product.getHsnCode(), customerStateCode, taxType, TaxRate.TaxRateStatus.ACTIVE);

        if (taxRateOpt.isEmpty()) {
            log.warn("No tax rate found for HSN: {}, State: {}, Type: {}", 
                    product.getHsnCode(), customerStateCode, taxType);
            // Return without tax if no rate found
            return PriceBreakdown.builder()
                    .basePrice(basePrice)
                    .netPrice(basePrice)
                    .cgstAmount(BigDecimal.ZERO)
                    .sgstAmount(BigDecimal.ZERO)
                    .igstAmount(BigDecimal.ZERO)
                    .cessAmount(BigDecimal.ZERO)
                    .totalTaxAmount(BigDecimal.ZERO)
                    .totalPrice(basePrice)
                    .taxType(taxType)
                    .build();
        }

        TaxRate taxRate = taxRateOpt.get();
        
        // Calculate based on tax inclusive/exclusive
        if (product.getIsTaxInclusive()) {
            return calculateTaxInclusivePrice(basePrice, taxRate, taxType);
        } else {
            return calculateTaxExclusivePrice(basePrice, taxRate, taxType);
        }
    }

    private PriceBreakdown calculateTaxInclusivePrice(BigDecimal totalPrice, TaxRate taxRate, TaxRate.TaxType taxType) {
        BigDecimal totalTaxRate = taxRate.getTotalTaxRate();
        BigDecimal divisor = BigDecimal.valueOf(100).add(totalTaxRate);
        
        // Net price = Total price / (1 + tax rate)
        BigDecimal netPrice = totalPrice.multiply(BigDecimal.valueOf(100))
                .divide(divisor, 2, RoundingMode.HALF_UP);
        
        return calculateTaxComponents(netPrice, totalPrice, taxRate, taxType);
    }

    private PriceBreakdown calculateTaxExclusivePrice(BigDecimal netPrice, TaxRate taxRate, TaxRate.TaxType taxType) {
        BigDecimal totalTaxRate = taxRate.getTotalTaxRate();
        
        // Total tax amount = Net price * (tax rate / 100)
        BigDecimal totalTaxAmount = netPrice.multiply(totalTaxRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        BigDecimal totalPrice = netPrice.add(totalTaxAmount);
        
        return calculateTaxComponents(netPrice, totalPrice, taxRate, taxType);
    }

    private PriceBreakdown calculateTaxComponents(BigDecimal netPrice, BigDecimal totalPrice, 
                                                TaxRate taxRate, TaxRate.TaxType taxType) {
        
        BigDecimal cgstAmount = BigDecimal.ZERO;
        BigDecimal sgstAmount = BigDecimal.ZERO;
        BigDecimal igstAmount = BigDecimal.ZERO;
        BigDecimal cessAmount = BigDecimal.ZERO;

        if (taxType == TaxRate.TaxType.INTRA_STATE) {
            if (taxRate.getCgstRate() != null) {
                cgstAmount = netPrice.multiply(taxRate.getCgstRate())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            if (taxRate.getSgstRate() != null) {
                sgstAmount = netPrice.multiply(taxRate.getSgstRate())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        } else {
            if (taxRate.getIgstRate() != null) {
                igstAmount = netPrice.multiply(taxRate.getIgstRate())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        }

        if (taxRate.getCessRate() != null) {
            cessAmount = netPrice.multiply(taxRate.getCessRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalTaxAmount = cgstAmount.add(sgstAmount).add(igstAmount).add(cessAmount);

        return PriceBreakdown.builder()
                .basePrice(netPrice)
                .netPrice(netPrice)
                .cgstAmount(cgstAmount)
                .sgstAmount(sgstAmount)
                .igstAmount(igstAmount)
                .cessAmount(cessAmount)
                .totalTaxAmount(totalTaxAmount)
                .totalPrice(totalPrice)
                .taxType(taxType)
                .build();
    }

    /**
     * Price breakdown result
     */
    public static class PriceBreakdown {
        private final BigDecimal basePrice;
        private final BigDecimal netPrice;
        private final BigDecimal cgstAmount;
        private final BigDecimal sgstAmount;
        private final BigDecimal igstAmount;
        private final BigDecimal cessAmount;
        private final BigDecimal totalTaxAmount;
        private final BigDecimal totalPrice;
        private final TaxRate.TaxType taxType;

        private PriceBreakdown(Builder builder) {
            this.basePrice = builder.basePrice;
            this.netPrice = builder.netPrice;
            this.cgstAmount = builder.cgstAmount;
            this.sgstAmount = builder.sgstAmount;
            this.igstAmount = builder.igstAmount;
            this.cessAmount = builder.cessAmount;
            this.totalTaxAmount = builder.totalTaxAmount;
            this.totalPrice = builder.totalPrice;
            this.taxType = builder.taxType;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public BigDecimal getBasePrice() { return basePrice; }
        public BigDecimal getNetPrice() { return netPrice; }
        public BigDecimal getCgstAmount() { return cgstAmount; }
        public BigDecimal getSgstAmount() { return sgstAmount; }
        public BigDecimal getIgstAmount() { return igstAmount; }
        public BigDecimal getCessAmount() { return cessAmount; }
        public BigDecimal getTotalTaxAmount() { return totalTaxAmount; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public TaxRate.TaxType getTaxType() { return taxType; }

        public static class Builder {
            private BigDecimal basePrice;
            private BigDecimal netPrice;
            private BigDecimal cgstAmount;
            private BigDecimal sgstAmount;
            private BigDecimal igstAmount;
            private BigDecimal cessAmount;
            private BigDecimal totalTaxAmount;
            private BigDecimal totalPrice;
            private TaxRate.TaxType taxType;

            public Builder basePrice(BigDecimal basePrice) {
                this.basePrice = basePrice;
                return this;
            }

            public Builder netPrice(BigDecimal netPrice) {
                this.netPrice = netPrice;
                return this;
            }

            public Builder cgstAmount(BigDecimal cgstAmount) {
                this.cgstAmount = cgstAmount;
                return this;
            }

            public Builder sgstAmount(BigDecimal sgstAmount) {
                this.sgstAmount = sgstAmount;
                return this;
            }

            public Builder igstAmount(BigDecimal igstAmount) {
                this.igstAmount = igstAmount;
                return this;
            }

            public Builder cessAmount(BigDecimal cessAmount) {
                this.cessAmount = cessAmount;
                return this;
            }

            public Builder totalTaxAmount(BigDecimal totalTaxAmount) {
                this.totalTaxAmount = totalTaxAmount;
                return this;
            }

            public Builder totalPrice(BigDecimal totalPrice) {
                this.totalPrice = totalPrice;
                return this;
            }

            public Builder taxType(TaxRate.TaxType taxType) {
                this.taxType = taxType;
                return this;
            }

            public PriceBreakdown build() {
                return new PriceBreakdown(this);
            }
        }
    }

    /**
     * Calculate tax for given parameters and return TaxCalculationResponse
     */
    public com.bharatshop.shared.dto.TaxCalculationResponse calculateTax(
            Long tenantId, String hsnCode, String sellerStateCode, 
            String buyerStateCode, BigDecimal amount, boolean isTaxInclusive) {
        
        // Determine tax type based on state codes
        TaxRate.TaxType taxType = sellerStateCode.equals(buyerStateCode) 
                ? TaxRate.TaxType.INTRA_STATE 
                : TaxRate.TaxType.INTER_STATE;

        // Find applicable tax rate
        Optional<TaxRate> taxRateOpt = taxRateRepository.findByTenantIdAndHsnCodeAndStateCodeAndTaxTypeAndStatus(
                tenantId, hsnCode, sellerStateCode, taxType, TaxRate.TaxRateStatus.ACTIVE);

        if (taxRateOpt.isEmpty()) {
            log.warn("No tax rate found for HSN: {}, State: {}, Type: {}", 
                    hsnCode, sellerStateCode, taxType);
            // Return response with zero tax
            return com.bharatshop.shared.dto.TaxCalculationResponse.create(
                    hsnCode, sellerStateCode, buyerStateCode, amount, isTaxInclusive,
                    amount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        TaxRate taxRate = taxRateOpt.get();
        
        // Calculate based on tax inclusive/exclusive
        PriceBreakdown breakdown;
        if (isTaxInclusive) {
            breakdown = calculateTaxInclusivePrice(amount, taxRate, taxType);
        } else {
            breakdown = calculateTaxExclusivePrice(amount, taxRate, taxType);
        }

        // Convert to TaxCalculationResponse
        return com.bharatshop.shared.dto.TaxCalculationResponse.create(
                hsnCode, sellerStateCode, buyerStateCode, amount, isTaxInclusive,
                breakdown.getNetPrice(), 
                taxRate.getCgstRate(), taxRate.getSgstRate(), 
                taxRate.getIgstRate(), taxRate.getCessRate(),
                breakdown.getCgstAmount(), breakdown.getSgstAmount(),
                breakdown.getIgstAmount(), breakdown.getCessAmount());
    }
}