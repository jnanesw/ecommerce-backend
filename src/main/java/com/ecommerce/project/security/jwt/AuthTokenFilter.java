package com.ecommerce.project.security.jwt;

import com.ecommerce.project.security.services.UserDetailsImpl;
import com.ecommerce.project.security.services.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter     extends OncePerRequestFilter {
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    @Autowired
    private JwtUtils jwtUtils;
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        logger.info("AuthTokenFilter called for URI: {}", request.getRequestURI());

        try {
            String token = parseJwt(request);

            if (token!=null && jwtUtils.validateJwtToken(token)) {
                // 1. Extract username from token
                String username = jwtUtils.getUsernameFromJwtToken(token);

                // 2. Load user details from DB/UserDetailsService
                UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);

                // 3. Create Authentication object with authorities
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, // principal
                                null,        // credentials (null because we already validated)
                                userDetails.getAuthorities() // roles/permissions
                        );

                // 4. Attach request details (IP, session, etc.)
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 5. Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.info("Roles from JWT: {}", userDetails.getAuthorities());
            }
        } catch (Exception e) {
            logger.info("Cannot set user Authentication: {}", e.getMessage());
        }

        // 6. Continue the filter chain
        filterChain.doFilter(request, response);
    }

    public String parseJwt(HttpServletRequest request){
        String jwt = jwtUtils.getJwtFromCookie(request);
        logger.info("Parsing token from cookie");
        return jwt;
    }
}
