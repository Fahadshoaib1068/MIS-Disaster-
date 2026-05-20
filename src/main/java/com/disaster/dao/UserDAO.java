package com.disaster.dao;

import com.disaster.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    
    public User authenticate(String username, String password, String role) throws SQLException {
        String sql = "SELECT u.UserID, u.UserName, u.Email, u.PhoneNumber, u.IsActive, " +
                     "CASE " +
                     "  WHEN a.UserID IS NOT NULL THEN 'admin' " +
                     "  WHEN eo.UserID IS NOT NULL THEN 'operator' " +
                     "  WHEN fo.UserID IS NOT NULL THEN 'field' " +
                     "  WHEN wm.UserID IS NOT NULL THEN 'warehouse' " +
                     "  WHEN fo2.UserID IS NOT NULL THEN 'finance' " +
                     "  ELSE 'user' " +
                     "END as Role " +
                     "FROM Users u " +
                     "LEFT JOIN Administrator a ON u.UserID = a.UserID " +
                     "LEFT JOIN Emergency_Operator eo ON u.UserID = eo.UserID " +
                     "LEFT JOIN Field_Officer fo ON u.UserID = fo.UserID " +
                     "LEFT JOIN Warehouse_Manager wm ON u.UserID = wm.UserID " +
                     "LEFT JOIN Finance_Officer fo2 ON u.UserID = fo2.UserID " +
                     "WHERE u.UserName = ? AND u.PasswordHash = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getInt("UserID"));
                    user.setUserName(rs.getString("UserName"));
                    user.setEmail(rs.getString("Email"));
                    user.setPhoneNumber(rs.getString("PhoneNumber"));
                    user.setActive(rs.getBoolean("IsActive"));
                    user.setRole(rs.getString("Role"));
                    
                    // Verify role matches selected role
                    if (user.getRole().equals(role)) {
                        return user;
                    }
                }
            }
        }
        return null;
    }
    
    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.UserID, u.UserName, u.Email, u.PhoneNumber, u.IsActive, u.CreatedAt, " +
                     "CASE " +
                     "  WHEN a.UserID IS NOT NULL THEN 'Administrator' " +
                     "  WHEN eo.UserID IS NOT NULL THEN 'Emergency Operator' " +
                     "  WHEN fo.UserID IS NOT NULL THEN 'Field Officer' " +
                     "  WHEN wm.UserID IS NOT NULL THEN 'Warehouse Manager' " +
                     "  WHEN fo2.UserID IS NOT NULL THEN 'Finance Officer' " +
                     "  ELSE 'User' " +
                     "END as RoleName " +
                     "FROM Users u " +
                     "LEFT JOIN Administrator a ON u.UserID = a.UserID " +
                     "LEFT JOIN Emergency_Operator eo ON u.UserID = eo.UserID " +
                     "LEFT JOIN Field_Officer fo ON u.UserID = fo.UserID " +
                     "LEFT JOIN Warehouse_Manager wm ON u.UserID = wm.UserID " +
                     "LEFT JOIN Finance_Officer fo2 ON u.UserID = fo2.UserID " +
                     "WHERE u.IsActive = 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("UserID"));
                user.setUserName(rs.getString("UserName"));
                user.setEmail(rs.getString("Email"));
                user.setPhoneNumber(rs.getString("PhoneNumber"));
                user.setActive(true);
                user.setRole(rs.getString("RoleName"));
                user.setCreatedAt(rs.getString("CreatedAt"));
                users.add(user);
            }
        }
        return users;
    }
}