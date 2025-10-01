package com.ecommerce.project.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);
    @Value("${spring.app.jwtSecret}")
    private String secretKey;
    @Value("${spring.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    // Getting token from Headers
    public String getTokenFromHeaders(HttpServletRequest request){
        String token = request.getHeader("Authorization");
        log.debug("Authorization token: {}", token);
        if(token != null && token.startsWith("Bearer")){
            return token.substring(7);
        }

        return null;
    }

    // Generating Token from Username
    public String generateTokenFromUsername(UserDetails userDetails){
        String username = userDetails.getUsername();
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + jwtExpirationMs))
                .signWith((SecretKey) key())
                .compact();
    }

    // Getting Username from JWT token
    public String getUsernameFromJwtToken(String token){
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build().parseSignedClaims(token)
                .getPayload().getSubject();
    }

    // Generating Signing key
    public Key key(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    // Validate JWT Token
    public boolean validateJwtToken(String authToken) {
        try {
            System.out.println("Validate JWT token");
            Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
