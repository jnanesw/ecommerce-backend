package com.ecommerce.project.security.jwt;

import com.ecommerce.project.security.services.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    @Value("${spring.app.jwtSecret}")
    private String secretKey;
    @Value("${spring.app.jwtExpirationMs}")
    private int jwtExpirationMs;
    @Value("${spring.app.jwtCookie}")
    private String jwtCookie;

    // Getting token from Headers
    public String getTokenFromHeaders(HttpServletRequest request){
        String token = request.getHeader("Authorization");
        logger.info("Authorization token: {}", token);
        if(token != null && token.startsWith("Bearer")){
            return token.substring(7);
        }else if(token != null){
            return token;
        }

        return null;
    }

    public String getJwtFromCookie(HttpServletRequest request){
        Cookie cookie = WebUtils.getCookie(request, jwtCookie);
        if(cookie != null){
            return cookie.getValue();
        }else {
            return null;
        }
    }

    public ResponseCookie generateJwtCookie(UserDetailsImpl userDetails){
        String jwt = generateTokenFromUsername(userDetails.getUsername());
        return ResponseCookie.from(jwtCookie, jwt)
                .path("/api")
                .maxAge(24*60*60)
                .httpOnly(false)
                .secure(false)
                .build();
    }
    public ResponseCookie getCleanJwtCookie() {
        ResponseCookie cookie = ResponseCookie.from(jwtCookie, null).path("/api").build();
        return cookie;  
    }

    // Generating Token from Username
    public String generateTokenFromUsername(String username){
//        String username = userDetails.getUsername();
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
            logger.info("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.info("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.info("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.info("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }


}
