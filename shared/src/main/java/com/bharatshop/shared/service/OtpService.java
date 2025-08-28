package com.bharatshop.shared.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private final SecureRandom random = new SecureRandom();
    
    // In-memory storage for OTPs (in production, use Redis or database)
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();

    public String generateOtp(String phoneNumber) {
        // Generate 6-digit OTP
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        
        String otpCode = otp.toString();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        
        // Store OTP with expiry time
        otpStorage.put(phoneNumber, new OtpData(otpCode, expiryTime));
        
        // Simulate SMS sending (in production, integrate with SMS provider)
        simulateSmsDelivery(phoneNumber, otpCode);
        
        log.info("OTP generated for phone number: {} (expires at: {})", phoneNumber, expiryTime);
        return otpCode;
    }

    public boolean verifyOtp(String phoneNumber, String providedOtp) {
        OtpData storedOtpData = otpStorage.get(phoneNumber);
        
        if (storedOtpData == null) {
            log.warn("No OTP found for phone number: {}", phoneNumber);
            return false;
        }
        
        if (LocalDateTime.now().isAfter(storedOtpData.getExpiryTime())) {
            log.warn("OTP expired for phone number: {}", phoneNumber);
            otpStorage.remove(phoneNumber); // Clean up expired OTP
            return false;
        }
        
        boolean isValid = storedOtpData.getOtpCode().equals(providedOtp);
        
        if (isValid) {
            otpStorage.remove(phoneNumber); // Remove OTP after successful verification
            log.info("OTP verified successfully for phone number: {}", phoneNumber);
        } else {
            log.warn("Invalid OTP provided for phone number: {}", phoneNumber);
        }
        
        return isValid;
    }

    public boolean isOtpValid(String phoneNumber) {
        OtpData storedOtpData = otpStorage.get(phoneNumber);
        return storedOtpData != null && LocalDateTime.now().isBefore(storedOtpData.getExpiryTime());
    }

    public void invalidateOtp(String phoneNumber) {
        otpStorage.remove(phoneNumber);
        log.info("OTP invalidated for phone number: {}", phoneNumber);
    }

    private void simulateSmsDelivery(String phoneNumber, String otp) {
        // Simulate SMS delivery with a delay
        log.info("[SMS SIMULATION] Sending OTP {} to phone number: {}", otp, phoneNumber);
        log.info("[SMS SIMULATION] Message: Your BharatShop verification code is: {}. Valid for {} minutes.", 
                otp, OTP_EXPIRY_MINUTES);
    }

    // Clean up expired OTPs periodically (in production, use @Scheduled)
    public void cleanupExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        otpStorage.entrySet().removeIf(entry -> now.isAfter(entry.getValue().getExpiryTime()));
        log.debug("Cleaned up expired OTPs");
    }

    private static class OtpData {
        private final String otpCode;
        private final LocalDateTime expiryTime;

        public OtpData(String otpCode, LocalDateTime expiryTime) {
            this.otpCode = otpCode;
            this.expiryTime = expiryTime;
        }

        public String getOtpCode() {
            return otpCode;
        }

        public LocalDateTime getExpiryTime() {
            return expiryTime;
        }
    }
}