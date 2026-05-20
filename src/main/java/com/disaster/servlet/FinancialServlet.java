package com.disaster.servlet;

import com.disaster.dao.FinancialDAO;
import com.disaster.model.FinancialTransaction;
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

@WebServlet("/api/financial/*")
public class FinancialServlet extends HttpServlet {
    private FinancialDAO financialDAO = new FinancialDAO();
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
        if (!SecurityUtil.requireAnyRole(response, currentUser, "finance", "admin")) {
            return;
        }
        
        String path = request.getPathInfo();
        
        try {
            if ("/all".equals(path)) {
                out.print(gson.toJson(financialDAO.getAllTransactions()));
            } else if ("/summary".equals(path)) {
                Map<String, Double> summary = new HashMap<>();
                summary.put("donations", financialDAO.getTotalDonations());
                summary.put("expenses", financialDAO.getTotalExpenses());
                out.print(gson.toJson(summary));
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
        if (!SecurityUtil.requireAnyRole(response, currentUser, "finance", "admin")) {
            return;
        }
        
        try {
            BufferedReader reader = request.getReader();
            FinancialTransaction transaction = gson.fromJson(reader, FinancialTransaction.class);
            transaction.setUserId(currentUser.getUserId());
            
            boolean success = financialDAO.addTransaction(transaction);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            out.print(gson.toJson(result));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
