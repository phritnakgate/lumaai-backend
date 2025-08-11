package org.bkkz.lumabackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class JwtUtil {
    @Value("${jwt.access-token.secret}")
    private String accessTokenSecret;
    @Value("${jwt.access-token.expiration.ms}")
    private long accessTokenExpirationMs;
    @Value("${jwt.refresh-token.expiration.days}")
    private long refreshTokenExpirationDays;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(accessTokenSecret.getBytes());
    }

    public String generateAccessToken(String uid, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        return Jwts.builder()
                .claim("email", email)
                .subject(uid)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String uid) {
        return Jwts.builder()
                .subject(uid)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(refreshTokenExpirationDays)))
                .signWith(key)
                .compact();
    }

    public String extractUid(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        if (claims != null) {
            return claimsResolver.apply(claims);
        }
        return null;
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            // จัดการข้อผิดพลาดของ JWT เช่น ExpiredJwtException, SignatureException
            System.err.println("Invalid JWT: " + e.getMessage());
            return null;
        }
    }

    private Boolean isTokenExpired(String token) {
        try {
            final Date expiration = extractExpiration(token);
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Boolean validateToken(String token, String uid) {
        final String extractedUid = extractUid(token);
        return (extractedUid.equals(uid) && !isTokenExpired(token));
    }
}
