package com.disaster.servlet;

import com.disaster.dao.EventDAO;
import com.disaster.model.DisasterEvent;
import com.disaster.model.User;
import com.disaster.util.SecurityUtil;
import com.google.gson.Gson;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/events/*")
public class EventServlet extends HttpServlet {
    private EventDAO eventDAO = new EventDAO();
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
                out.print(gson.toJson(eventDAO.getAllEvents()));
            } else if ("/active".equals(path)) {
                out.print(gson.toJson(eventDAO.getActiveEvents()));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        User currentUser = SecurityUtil.getAuthenticatedUser(request);
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Not authenticated\"}");
            return;
        }
        if (!SecurityUtil.requireAnyRole(response, currentUser, "operator", "admin")) {
            return;
        }
        
        try {
            BufferedReader reader = request.getReader();
            DisasterEvent event = gson.fromJson(reader, DisasterEvent.class);
            boolean success = eventDAO.addEvent(event);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            out.print(gson.toJson(result));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
