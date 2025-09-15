package com.bharatshop.shared.service;

import com.bharatshop.shared.entity.JwksKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;


@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtService.class);
    
    private final JwtKeyRotationService jwtKeyRotationService;

    @Value("${jwt.access-token.expiration:86400000}") // 24 hours
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration:604800000}") // 7 days
    private long refreshTokenExpiration;
    
    @Value("${jwt.algorithm:HS256}")
    private String defaultAlgorithm;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public String extractKeyId(String token) {
        return extractKeyIdFromHeader(token);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    


    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(new HashMap<>(), userDetails);
    }

    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, accessTokenExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshTokenExpiration);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        JwksKey signingKey = jwtKeyRotationService.getCurrentSigningKey();
        SecretKey secretKey = jwtKeyRotationService.getSecretKey(signingKey);
        
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .setHeaderParam("kid", signingKey.getKid())
                .setHeaderParam("alg", signingKey.getAlg())
                .signWith(secretKey, SignatureAlgorithm.valueOf(signingKey.getAlg()))
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    public boolean isTokenValidWithKeyRotation(String token, UserDetails userDetails) {
        try {
            // Extract key ID from token header
            String kid = extractKeyId(token);
            
            if (kid != null) {
                // Try to validate with specific key
                Optional<JwksKey> key = jwtKeyRotationService.getKeyByKid(kid);
                if (key.isPresent() && jwtKeyRotationService.isKeyValidForVerification(key.get())) {
                    return isTokenValidWithKey(token, userDetails, key.get());
                }
            }
            
            // Fallback: try all valid verification keys
            return tryValidateWithAllKeys(token, userDetails);
            
        } catch (Exception e) {
            log.debug("Token validation with key rotation failed: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean isTokenValidWithKey(String token, UserDetails userDetails, JwksKey key) {
        try {
            SecretKey secretKey = jwtKeyRotationService.getSecretKey(key);
            
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            String username = claims.getSubject();
            Date expiration = claims.getExpiration();
            
            return username.equals(userDetails.getUsername()) && 
                   expiration.after(new Date());
                   
        } catch (Exception e) {
            log.debug("Token validation failed with key {}: {}", key.getKid(), e.getMessage());
            return false;
        }
    }
    
    private boolean tryValidateWithAllKeys(String token, UserDetails userDetails) {
        var validKeys = jwtKeyRotationService.getValidVerificationKeys();
        
        for (JwksKey key : validKeys) {
            if (isTokenValidWithKey(token, userDetails, key)) {
                log.debug("Token validated successfully with key: {}", key.getKid());
                return true;
            }
        }
        
        log.debug("Token validation failed with all available keys");
        return false;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        // Try with key rotation support first
        String kid = extractKeyId(token);
        
        if (kid != null) {
            Optional<JwksKey> key = jwtKeyRotationService.getKeyByKid(kid);
            if (key.isPresent() && jwtKeyRotationService.isKeyValidForVerification(key.get())) {
                SecretKey secretKey = jwtKeyRotationService.getSecretKey(key.get());
                return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            }
        }
        
        // Fallback: try all valid keys
        var validKeys = jwtKeyRotationService.getValidVerificationKeys();
        
        for (JwksKey key : validKeys) {
            try {
                SecretKey secretKey = jwtKeyRotationService.getSecretKey(key);
                return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            } catch (Exception e) {
                log.debug("Failed to parse token with key {}: {}", key.getKid(), e.getMessage());
            }
        }
        
        throw new RuntimeException("Unable to parse token with any available key");
    }
    
    private String extractKeyIdFromHeader(String token) {
        try {
            // Extract header without verification for key ID lookup
            String[] chunks = token.split("\\.");
            if (chunks.length != 3) {
                return null;
            }
            
            Header header = Jwts.parserBuilder()
                .build()
                .parseClaimsJwt(chunks[0] + "." + chunks[1] + ".")
                .getHeader();
                
            return (String) header.get("kid");
        } catch (Exception e) {
            log.debug("Failed to extract key ID from token header: {}", e.getMessage());
            return null;
        }
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}