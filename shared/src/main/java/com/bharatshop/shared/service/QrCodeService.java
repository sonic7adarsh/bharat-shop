package com.bharatshop.shared.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating QR codes
 */
@Service
@Slf4j
public class QrCodeService {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QrCodeService.class);

    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 200;
    private static final Color DEFAULT_FOREGROUND_COLOR = Color.BLACK;
    private static final Color DEFAULT_BACKGROUND_COLOR = Color.WHITE;

    /**
     * Generate QR code as Base64 encoded string
     */
    public String generateQrCodeBase64(String data, int width, int height) {
        try {
            byte[] qrCodeBytes = generateQrCodeBytes(data, width, height);
            return Base64.getEncoder().encodeToString(qrCodeBytes);
        } catch (Exception e) {
            log.error("Error generating QR code: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Generate QR code as byte array (PNG format)
     */
    public byte[] generateQrCodeBytes(String data, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);
        
        BufferedImage image = createQrCodeImage(bitMatrix);
        
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Generate QR code with default dimensions
     */
    public String generateQrCodeBase64(String data) {
        return generateQrCodeBase64(data, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Generate QR code as BufferedImage
     */
    public BufferedImage generateQrCodeImage(String data, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);
        
        return createQrCodeImage(bitMatrix);
    }

    /**
     * Generate QR code with custom colors
     */
    public String generateQrCodeBase64(String data, int width, int height, Color foregroundColor, Color backgroundColor) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);
            
            BufferedImage image = createQrCodeImage(bitMatrix, foregroundColor, backgroundColor);
            
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                ImageIO.write(image, "PNG", outputStream);
                byte[] qrCodeBytes = outputStream.toByteArray();
                return Base64.getEncoder().encodeToString(qrCodeBytes);
            }
        } catch (Exception e) {
            log.error("Error generating QR code with custom colors: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Create BufferedImage from BitMatrix with default colors
     */
    private BufferedImage createQrCodeImage(BitMatrix bitMatrix) {
        return createQrCodeImage(bitMatrix, DEFAULT_FOREGROUND_COLOR, DEFAULT_BACKGROUND_COLOR);
    }

    /**
     * Create BufferedImage from BitMatrix with custom colors
     */
    private BufferedImage createQrCodeImage(BitMatrix bitMatrix, Color foregroundColor, Color backgroundColor) {
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        int foregroundRGB = foregroundColor.getRGB();
        int backgroundRGB = backgroundColor.getRGB();
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? foregroundRGB : backgroundRGB);
            }
        }
        
        return image;
    }

    /**
     * Validate QR code data length
     */
    public boolean isValidQrCodeData(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        
        // QR Code capacity varies by error correction level and character type
        // For alphanumeric with Medium error correction: ~1,852 characters
        // For UTF-8 with Medium error correction: ~1,273 characters
        return data.length() <= 1000; // Conservative limit
    }

    /**
     * Generate QR code for GST invoice (specific format)
     */
    public String generateGstInvoiceQrCode(String sellerGstin, String invoiceNumber, 
                                          String invoiceDate, String totalAmount, 
                                          String taxAmount, String buyerGstin) {
        StringBuilder qrData = new StringBuilder();
        
        qrData.append("GSTIN:").append(sellerGstin).append("\n");
        qrData.append("INV:").append(invoiceNumber).append("\n");
        qrData.append("DATE:").append(invoiceDate).append("\n");
        qrData.append("AMT:").append(totalAmount).append("\n");
        qrData.append("TAX:").append(taxAmount);
        
        if (buyerGstin != null && !buyerGstin.trim().isEmpty()) {
            qrData.append("\n").append("BUYER_GSTIN:").append(buyerGstin);
        }
        
        return generateQrCodeBase64(qrData.toString());
    }

    /**
     * Generate QR code for payment (UPI format)
     */
    public String generatePaymentQrCode(String upiId, String payeeName, String amount, String transactionNote) {
        StringBuilder upiData = new StringBuilder();
        
        upiData.append("upi://pay?pa=").append(upiId);
        upiData.append("&pn=").append(payeeName);
        
        if (amount != null && !amount.isEmpty()) {
            upiData.append("&am=").append(amount);
        }
        
        if (transactionNote != null && !transactionNote.isEmpty()) {
            upiData.append("&tn=").append(transactionNote);
        }
        
        return generateQrCodeBase64(upiData.toString());
    }
}