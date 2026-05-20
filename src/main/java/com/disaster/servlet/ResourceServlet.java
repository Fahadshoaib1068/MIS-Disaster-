package com.disaster.servlet;

import com.disaster.dao.ResourceDAO;
import com.disaster.model.AllocationRequest;
import com.disaster.model.ProcurementRequest;
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

@WebServlet("/api/resources/*")
public class ResourceServlet extends HttpServlet {
    private ResourceDAO resourceDAO = new ResourceDAO();
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
            if ("/inventory".equals(path)) {
                out.print(gson.toJson(resourceDAO.getInventory()));
            } else if ("/lowstock".equals(path)) {
                out.print(gson.toJson(resourceDAO.getLowStockItems()));
            } else if ("/allocations".equals(path)) {
                out.print(gson.toJson(resourceDAO.getAllocations()));
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
        
        String path = request.getPathInfo();
        
        try {
            if ("/procurement".equals(path)) {
                if (!SecurityUtil.requireAnyRole(response, currentUser, "warehouse", "admin")) {
                    return;
                }

                BufferedReader reader = request.getReader();
                ProcurementRequest procurementRequest = gson.fromJson(reader, ProcurementRequest.class);
                procurementRequest.setRequestedBy(currentUser.getUserId());

                boolean success = resourceDAO.requestProcurement(procurementRequest);

                Map<String, Object> result = new HashMap<>();
                result.put("success", success);
                out.print(gson.toJson(result));
                return;
            }

            if (!SecurityUtil.requireAnyRole(response, currentUser, "field", "warehouse", "admin")) {
                return;
            }

            BufferedReader reader = request.getReader();
            AllocationRequest allocation = gson.fromJson(reader, AllocationRequest.class);
            allocation.setRequestedBy(currentUser.getUserId());
            
            boolean success = resourceDAO.requestAllocation(allocation);
            
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
        if (!SecurityUtil.requireAnyRole(response, currentUser, "warehouse")) {
            return;
        }

        String path = request.getPathInfo();
        try {
            if (path != null && path.startsWith("/forward/")) {
                int approvalId = Integer.parseInt(path.substring("/forward/".length()));
                String comments = request.getParameter("comments");
                boolean success = resourceDAO.forwardAllocationToAdmin(approvalId, currentUser.getUserId(), comments);

                Map<String, Object> result = new HashMap<>();
                result.put("success", success);
                out.print(gson.toJson(result));
                return;
            }

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Unsupported resource action\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
