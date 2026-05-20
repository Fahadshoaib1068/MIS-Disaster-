package com.disaster.servlet;

import com.disaster.dao.ReportDAO;
import com.disaster.model.EmergencyReport;
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

@WebServlet("/api/reports/*")
public class ReportServlet extends HttpServlet {
    private ReportDAO reportDAO = new ReportDAO();
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
                out.print(gson.toJson(reportDAO.getAllReports()));
            } else if ("/active".equals(path)) {
                out.print(gson.toJson(reportDAO.getActiveReports()));
            } else if (path != null && path.startsWith("/")) {
                int id = Integer.parseInt(path.substring(1));
                out.print(gson.toJson(reportDAO.getReportById(id)));
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
            BufferedReader reader = request.getReader();
            EmergencyReport report = gson.fromJson(reader, EmergencyReport.class);
            boolean success = reportDAO.addReport(report);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            out.print(gson.toJson(result));
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
        if (!SecurityUtil.requireAnyRole(response, currentUser, "operator", "field", "admin")) {
            return;
        }

        String path = request.getPathInfo();
        if (path == null || !path.matches("/\\d+")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Invalid report path\"}");
            return;
        }

        String status = request.getParameter("status");
        if (status == null || status.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Missing status\"}");
            return;
        }

        try {
            int reportId = Integer.parseInt(path.substring(1));
            boolean success = reportDAO.updateReportStatus(reportId, status);

            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            out.print(gson.toJson(result));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
