package com.disaster.model;

public class Patient {
    private int patientId;
    private String name;
    private int age;
    private String gender;
    private String condition;
    private String admissionTime;
    private String dischargeTime;
    private int hospitalId;
    private int reportId;
    
    public Patient() {}
    
    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    
    public String getAdmissionTime() { return admissionTime; }
    public void setAdmissionTime(String admissionTime) { this.admissionTime = admissionTime; }
    
    public String getDischargeTime() { return dischargeTime; }
    public void setDischargeTime(String dischargeTime) { this.dischargeTime = dischargeTime; }
    
    public int getHospitalId() { return hospitalId; }
    public void setHospitalId(int hospitalId) { this.hospitalId = hospitalId; }
    
    public int getReportId() { return reportId; }
    public void setReportId(int reportId) { this.reportId = reportId; }
}