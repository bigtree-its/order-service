package com.bigtree.order.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Capture start time of API call
        long startTime = System.currentTimeMillis();
        log.info("Received request for URL: {}" , request.getRequestURL().toString());
        request.setAttribute("startTime", startTime);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Capture end time of API call and calculate response time
        long startTime = (long) request.getAttribute("startTime");
        long endTime = System.currentTimeMillis();
        long timeTaken = endTime - startTime;
        log.info("Response Headers: {}", response.getHeaderNames());
        log.info("Request processing completed for URL: {}", request.getRequestURL().toString() + ". Total Time Taken: " + timeTaken + "ms");
    }
}
