package com.hercey.middleware;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

public class    AuthAccessFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        HttpSession httpSession = request.getSession(false);

        System.out.println("AuthAccessFilter called");

        if (httpSession != null && httpSession.getAttribute("user") != null) {
            System.out.println("User redirect to index.html");
            response.sendRedirect("index.html");
            System.out.println("user redirected.");
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
            response.setHeader("Cache-Control", "no-cache, no-store, invalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
            System.out.println("AuthAccessFilter else block called");
        }

    }
}
