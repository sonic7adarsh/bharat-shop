package com.bharatshop.shared.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service to convert numbers to words for invoice amounts
 */
@Service
public class NumberToWordsService {

    private static final String[] ONES = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private static final String[] SCALE = {
        "", "Thousand", "Lakh", "Crore"
    };

    /**
     * Convert BigDecimal amount to words (Indian numbering system)
     */
    public String convertToWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Zero Rupees Only";
        }

        // Round to 2 decimal places
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        
        // Split into rupees and paise
        long rupees = amount.longValue();
        long paise = amount.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).longValue();

        StringBuilder result = new StringBuilder();

        // Convert rupees part
        if (rupees > 0) {
            result.append(convertRupeesToWords(rupees));
            result.append(" Rupee");
            if (rupees > 1) {
                result.append("s");
            }
        }

        // Convert paise part
        if (paise > 0) {
            if (rupees > 0) {
                result.append(" and ");
            }
            result.append(convertNumberToWords(paise));
            result.append(" Paise");
        }

        result.append(" Only");
        return result.toString();
    }

    /**
     * Convert rupees amount to words using Indian numbering system
     */
    private String convertRupeesToWords(long number) {
        if (number == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        // Handle crores (10,000,000)
        if (number >= 10000000) {
            long crores = number / 10000000;
            result.append(convertNumberToWords(crores)).append(" Crore");
            if (crores > 1) {
                result.append("s");
            }
            number %= 10000000;
            if (number > 0) {
                result.append(" ");
            }
        }

        // Handle lakhs (100,000)
        if (number >= 100000) {
            long lakhs = number / 100000;
            result.append(convertNumberToWords(lakhs)).append(" Lakh");
            if (lakhs > 1) {
                result.append("s");
            }
            number %= 100000;
            if (number > 0) {
                result.append(" ");
            }
        }

        // Handle thousands (1,000)
        if (number >= 1000) {
            long thousands = number / 1000;
            result.append(convertNumberToWords(thousands)).append(" Thousand");
            number %= 1000;
            if (number > 0) {
                result.append(" ");
            }
        }

        // Handle remaining hundreds, tens, and ones
        if (number > 0) {
            result.append(convertNumberToWords(number));
        }

        return result.toString();
    }

    /**
     * Convert a number (0-999) to words
     */
    private String convertNumberToWords(long number) {
        if (number == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        // Handle hundreds
        if (number >= 100) {
            result.append(ONES[(int) (number / 100)]).append(" Hundred");
            number %= 100;
            if (number > 0) {
                result.append(" ");
            }
        }

        // Handle tens and ones
        if (number >= 20) {
            result.append(TENS[(int) (number / 10)]);
            number %= 10;
            if (number > 0) {
                result.append(" ").append(ONES[(int) number]);
            }
        } else if (number > 0) {
            result.append(ONES[(int) number]);
        }

        return result.toString();
    }

    /**
     * Convert amount to words with currency (alternative method)
     */
    public String convertAmountToWords(BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Zero " + currency + " Only";
        }

        amount = amount.setScale(2, RoundingMode.HALF_UP);
        long wholePart = amount.longValue();
        long fractionalPart = amount.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).longValue();

        StringBuilder result = new StringBuilder();

        if (wholePart > 0) {
            result.append(convertRupeesToWords(wholePart));
            result.append(" ").append(currency);
            if (wholePart > 1) {
                result.append("s");
            }
        }

        if (fractionalPart > 0) {
            if (wholePart > 0) {
                result.append(" and ");
            }
            result.append(convertNumberToWords(fractionalPart));
            if ("Rupees".equalsIgnoreCase(currency)) {
                result.append(" Paise");
            } else {
                result.append(" Cents");
            }
        }

        result.append(" Only");
        return result.toString();
    }

    /**
     * Convert simple number to words (for quantities, etc.)
     */
    public String convertSimpleNumberToWords(long number) {
        if (number == 0) {
            return "Zero";
        }
        return convertRupeesToWords(number);
    }
}