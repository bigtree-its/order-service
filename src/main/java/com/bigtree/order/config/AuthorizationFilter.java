package com.bigtree.order.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class AuthorizationFilter extends OncePerRequestFilter {

    @Value("#{'${auth.permitAll}'.split(',')}")
    List<String> permitAll;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String servletPath = request.getServletPath();
        log.info("Authorizing the request {}", servletPath);
        if (permitAll.contains(servletPath.trim())) {
            log.info("The requested url is whitelisted..");
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                    "PermitAll", null, null);
            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            log.info("Authorized");
        }
        chain.doFilter(request, response);
    }

}