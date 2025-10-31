package com.campusmarket.security.jwt;

import com.campusmarket.entity.User;
import com.campusmarket.service.LoginSessionService;
import com.campusmarket.service.UserService;
import com.campusmarket.service.SessionAccessException;
import com.campusmarket.service.LoginSessionService.LoginSession;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final LoginSessionService loginSessionService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   UserService userService,
                                   LoginSessionService loginSessionService) {
        this.tokenProvider = tokenProvider;
        this.userService = userService;
        this.loginSessionService = loginSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String jwt = resolveToken(request);
        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            try {
                LoginSession session = loginSessionService.getSession(jwt).orElse(null);
                if (session != null) {
                    authenticateWithUser(request, session.getUserId());
                    loginSessionService.refreshSession(jwt);
                } else {
                    authenticateWithJwtClaims(request, jwt);
                }
            } catch (SessionAccessException ex) {
                authenticateWithJwtClaims(request, jwt);
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateWithJwtClaims(HttpServletRequest request, String jwt) {
        Long userId = tokenProvider.getUserId(jwt);
        User user = userService.findById(userId);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void authenticateWithUser(HttpServletRequest request, Long userId) {
        User user = userService.findById(userId);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
