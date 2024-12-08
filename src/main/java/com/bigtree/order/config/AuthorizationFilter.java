package com.bigtree.order.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class AuthorizationFilter extends OncePerRequestFilter {

    @Value("#{'${auth.permitAll}'.split(',')}")
    List<String> permitAll;


    // Enable this if you want to control individual Urls
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
//            throws ServletException, IOException {
//
//        String servletPath = request.getServletPath();
//        log.info("Authorizing the request {}", servletPath);
//        if (permitAll.contains(servletPath.trim())) {
//            log.info("The requested url is whitelisted..");
//            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
//                    "PermitAll", null, null);
//            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
//            log.info("Authorized");
//        }else{
//            log.info("This endpoint is not authorised. Please whitelist {}",servletPath.trim());
//        }
//        chain.doFilter(request, response);
//    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                "PermitAll", null, null);
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        chain.doFilter(request, response);
    }

}