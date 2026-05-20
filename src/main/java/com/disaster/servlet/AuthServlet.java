package com.disaster.servlet;

import com.disaster.dao.UserDAO;
import com.disaster.model.User;
import com.google.gson.Gson;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    private UserDAO userDAO = new UserDAO();
    private Gson gson = new Gson();
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        
        String path = request.getPathInfo();
        
        if ("/login".equals(path)) {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            String role = request.getParameter("role");
            
            try {
                User user = userDAO.authenticate(username, password, role);
                
                if (user != null && user.isActive()) {
                    HttpSession session = request.getSession();
                    session.setAttribute("user", user);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("user", user);
                    result.put("message", "Login successful");
                    out.print(gson.toJson(result));
                } else {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", false);
                    result.put("message", "Invalid credentials");
                    out.print(gson.toJson(result));
                }
            } catch (Exception e) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", e.getMessage());
                out.print(gson.toJson(result));
            }
        } else if ("/logout".equals(path)) {
            HttpSession session = request.getSession(false);
            if (session != null) session.invalidate();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            out.print(gson.toJson(result));
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(false);
        
        Map<String, Object> result = new HashMap<>();
        result.put("authenticated", session != null && session.getAttribute("user") != null);
        if (session != null && session.getAttribute("user") != null) {
            result.put("user", session.getAttribute("user"));
        }
        out.print(gson.toJson(result));
    }
}