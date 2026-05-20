package com.disaster.util;

import com.disaster.model.User;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public final class SecurityUtil {
    private SecurityUtil() {}

    public static User getAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object user = session.getAttribute("user");
        return user instanceof User ? (User) user : null;
    }

    public static boolean hasAnyRole(User user, String... roles) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        for (String role : roles) {
            if (user.getRole().equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }

    public static boolean requireAnyRole(HttpServletResponse response, User user, String... roles) throws IOException {
        if (hasAnyRole(user, roles)) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().print("{\"error\": \"Access denied\"}");
        return false;
    }
}
