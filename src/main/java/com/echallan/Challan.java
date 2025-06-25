package com.echallan;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Challan {
    private String challanId;
    private String vehicleNumber;
    private String violation;
    private double fine;
    private String status;
    private String issueDate;
    private String dueDate;
    private String location;

    public Challan(String challanId, String vehicleNumber, String violation, double fine) {
        this.challanId = challanId;
        this.vehicleNumber = vehicleNumber;
        this.violation = violation;
        this.fine = fine;
        this.status = "PENDING";

        // Set issue date to current date
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        this.issueDate = now.format(formatter);

        // Set due date to 30 days from issue date
        this.dueDate = now.plusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        this.location = "Not Specified";
    }

    // Constructor with location
    public Challan(String challanId, String vehicleNumber, String violation, double fine, String location) {
        this(challanId, vehicleNumber, violation, fine);
        this.location = location != null ? location : "Not Specified";
    }

    // Getters
    public String getChallanId() { return challanId; }
    public String getVehicleNumber() { return vehicleNumber; }
    public String getViolation() { return violation; }
    public double getFine() { return fine; }
    public String getStatus() { return status; }
    public String getIssueDate() { return issueDate; }
    public String getDueDate() { return dueDate; }
    public String getLocation() { return location; }

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public void setLocation(String location) { this.location = location; }

    // Check if challan is overdue
    public boolean isOverdue() {
        if ("PAID".equals(status)) return false;
        try {
            LocalDateTime due = LocalDateTime.parse(dueDate + " 23:59",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return LocalDateTime.now().isAfter(due);
        } catch (Exception e) {
            return false;
        }
    }

    // Get penalty amount for overdue challans
    public double getPenaltyAmount() {
        return isOverdue() ? fine * 0.1 : 0; // 10% penalty
    }

    // Get total amount including penalty
    public double getTotalAmount() {
        return fine + getPenaltyAmount();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(challanId)
                .append(" | Vehicle: ").append(vehicleNumber)
                .append(" | Violation: ").append(violation)
                .append(" | Fine: ₹").append(fine);

        if (getPenaltyAmount() > 0) {
            sb.append(" | Penalty: ₹").append(String.format("%.2f", getPenaltyAmount()));
            sb.append(" | Total: ₹").append(String.format("%.2f", getTotalAmount()));
        }

        sb.append(" | Status: ").append(status)
                .append(" | Date: ").append(issueDate)
                .append(" | Due: ").append(dueDate)
                .append(" | Location: ").append(location);

        if (isOverdue() && !"PAID".equals(status)) {
            sb.append(" [OVERDUE]");
        }

        return sb.toString();
    }
}