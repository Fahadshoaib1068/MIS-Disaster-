package com.disaster.servlet;

import com.disaster.dao.AlertDAO;
import com.disaster.model.User;
import com.disaster.util.SecurityUtil;
import com.google.gson.Gson;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/alerts/*")
public class AlertServlet extends HttpServlet {
    private AlertDAO alertDAO = new AlertDAO();
    private Gson gson = new Gson();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        User currentUser = SecurityUtil.getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Not authenticated\"}");
            return;
        }
        
        String path = request.getPathInfo();
        
        try {
            if ("/all".equals(path)) {
                if ("admin".equalsIgnoreCase(currentUser.getRole())) {
                    out.print(gson.toJson(alertDAO.getAllAlerts()));
                } else {
                    out.print(gson.toJson(alertDAO.getAlertsForRecipient(currentUser.getUserId())));
                }
            } else if ("/unread".equals(path)) {
                out.print(gson.toJson(alertDAO.getUnreadAlerts(currentUser.getUserId())));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        User currentUser = SecurityUtil.getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Not authenticated\"}");
            return;
        }
        
        String path = request.getPathInfo();
        
        try {
            if (path != null && path.startsWith("/read/")) {
                int alertId = Integer.parseInt(path.substring(6));
                Integer targetUserId = "admin".equalsIgnoreCase(currentUser.getRole()) ? null : currentUser.getUserId();
                boolean success = alertDAO.markAsRead(alertId, targetUserId);
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", success);
                out.print(gson.toJson(result));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
