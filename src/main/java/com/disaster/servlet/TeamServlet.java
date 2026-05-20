package com.disaster.servlet;

import com.disaster.dao.TeamDAO;
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

@WebServlet("/api/teams/*")
public class TeamServlet extends HttpServlet {
    private TeamDAO teamDAO = new TeamDAO();
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
                out.print(gson.toJson(teamDAO.getAllTeams()));
            } else if ("/available".equals(path)) {
                out.print(gson.toJson(teamDAO.getAvailableTeams()));
            } else if ("/assignments".equals(path)) {
                out.print(gson.toJson(teamDAO.getTeamAssignments()));
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
        if (!SecurityUtil.requireAnyRole(response, currentUser, "operator", "field", "admin")) {
            return;
        }
        
        try {
            int teamId = Integer.parseInt(request.getParameter("teamId"));
            int reportId = Integer.parseInt(request.getParameter("reportId"));
            Integer authorizedBy = "field".equalsIgnoreCase(currentUser.getRole()) ? currentUser.getUserId() : null;
            
            boolean success = teamDAO.assignTeamToReport(teamId, reportId, authorizedBy, currentUser.getUserId());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            out.print(gson.toJson(result));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
