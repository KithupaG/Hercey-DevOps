package com.hercey.middleware;

import com.hercey.Annotation.IsUser;
import com.hercey.entity.User;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
@Priority(Priorities.AUTHENTICATION)
@IsUser
public class AuthFilter implements ContainerRequestFilter {
    @Context
    private HttpServletRequest request;
    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        String path = containerRequestContext.getUriInfo().getRequestUri().toString();
        System.out.println("[DEBUG] AuthFilter — Checking auth for: " + path);

        HttpSession httpSession = request.getSession(false);

        if (httpSession == null || httpSession.getAttribute("user") == null) {
            String redirectTo = request.getContextPath() + "/sign_in.html";
            if (httpSession != null) {
                System.out.println("[DEBUG] AuthFilter — user attribute null? " + (httpSession.getAttribute("user") == null));
            }
            containerRequestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .header("X-Auth-Required", "true")
                    .header("X-Auth-Message", "Session expired or not logged in")
                    .entity("{\"status\":false,\"message\":\"Authentication required. Please log in again.\"}")
                    .type("application/json")
                    .build());
        } else {
            User user = (User) httpSession.getAttribute("user");
            System.out.println("[DEBUG] AuthFilter — Authenticated: user ID=" + user.getId() + ", email=" + user.getEmail());
        }

    }
}
