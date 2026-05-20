package com.disaster.dao;

import com.disaster.model.Hospital;
import com.disaster.model.Patient;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HospitalDAO {
    
    public List<Hospital> getAllHospitals() throws SQLException {
        List<Hospital> hospitals = new ArrayList<>();
        String sql = "SELECT * FROM Hospital";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Hospital h = new Hospital();
                h.setHospitalId(rs.getInt("HospitalID"));
                h.setName(rs.getString("Name"));
                h.setStreetNo(rs.getString("StreetNo"));
                h.setCity(rs.getString("City"));
                h.setTotalBeds(rs.getInt("Totalbeds"));
                h.setAvailableBeds(rs.getInt("Availablebeds"));
                h.setContactNumber(rs.getString("Contactnumber"));
                hospitals.add(h);
            }
        }
        return hospitals;
    }
    
    public List<Patient> getPatientsByHospital(int hospitalId) throws SQLException {
        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT * FROM Patient WHERE Hospitalid = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, hospitalId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Patient p = new Patient();
                p.setPatientId(rs.getInt("PatientID"));
                p.setName(rs.getString("Name"));
                p.setAge(rs.getInt("Age"));
                p.setGender(rs.getString("Gender"));
                p.setCondition(rs.getString("Condition"));
                p.setAdmissionTime(rs.getString("Admissiontime"));
                p.setDischargeTime(rs.getString("Dischargetime"));
                p.setHospitalId(rs.getInt("Hospitalid"));
                p.setReportId(rs.getInt("Reportid"));
                patients.add(p);
            }
        }
        return patients;
    }
    
    public boolean admitPatient(Patient patient) throws SQLException {
        String sql = "INSERT INTO Patient (Name, Age, Gender, Condition, Admissiontime, Hospitalid, Reportid) VALUES (?, ?, ?, ?, GETDATE(), ?, ?)";

        String updateBeds = "UPDATE Hospital SET Availablebeds = Availablebeds - 1 WHERE HospitalID = ? AND Availablebeds > 0";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement insertStmt = conn.prepareStatement(sql);
                 PreparedStatement updateBedsStmt = conn.prepareStatement(updateBeds)) {

                insertStmt.setString(1, patient.getName());
                insertStmt.setInt(2, patient.getAge());
                insertStmt.setString(3, patient.getGender());
                insertStmt.setString(4, patient.getCondition());
                insertStmt.setInt(5, patient.getHospitalId());
                insertStmt.setInt(6, patient.getReportId());

                if (insertStmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                updateBedsStmt.setInt(1, patient.getHospitalId());
                if (updateBedsStmt.executeUpdate() == 0) {
                    conn.rollback();
                    throw new SQLException("No available beds in the selected hospital.");
                }

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
