package com.disaster.servlet;

import com.disaster.dao.HospitalDAO;
import com.disaster.model.Patient;
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

@WebServlet("/api/hospitals/*")
public class HospitalServlet extends HttpServlet {
    private HospitalDAO hospitalDAO = new HospitalDAO();
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
                out.print(gson.toJson(hospitalDAO.getAllHospitals()));
            } else if (path != null && path.startsWith("/patients/")) {
                int hospitalId = Integer.parseInt(path.substring(10));
                out.print(gson.toJson(hospitalDAO.getPatientsByHospital(hospitalId)));
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
        
        String path = request.getPathInfo();
        
        try {
            if ("/admit".equals(path)) {
                BufferedReader reader = request.getReader();
                Patient patient = gson.fromJson(reader, Patient.class);
                boolean success = hospitalDAO.admitPatient(patient);
                
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
