package com.echallan;

import java.sql.*;
import java.util.*;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:echallan.db";

    public Database() {
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("SQLite JDBC driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found! Make sure sqlite-jdbc is in your Maven dependencies.");
            e.printStackTrace();
        }
        createTable();
    }

    private void createTable() {
        // First, check if table exists and drop it if structure is wrong
        if (tableNeedsUpdate()) {
            dropTable();
        }

        String sql = """
            CREATE TABLE IF NOT EXISTS challans (
                challan_id TEXT PRIMARY KEY,
                vehicle_number TEXT NOT NULL,
                violation TEXT NOT NULL,
                fine REAL NOT NULL,
                status TEXT DEFAULT 'PENDING',
                issue_date TEXT,
                due_date TEXT,
                location TEXT DEFAULT 'Not Specified'
            )
        """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Database table created/verified successfully");
        } catch (SQLException e) {
            System.err.println("Error creating database table:");
            e.printStackTrace();
        }
    }

    private boolean tableNeedsUpdate() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Check if the table exists and has the right columns
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, "challans", null);

            boolean hasIssueDate = false;
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                if ("issue_date".equals(columnName)) {
                    hasIssueDate = true;
                    break;
                }
            }

            if (!hasIssueDate) {
                System.out.println("Table needs update - missing issue_date column");
                return true;
            }

            return false;
        } catch (SQLException e) {
            System.out.println("Could not check table structure, will recreate");
            return true;
        }
    }

    private void dropTable() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS challans");
            System.out.println("Old table dropped - will recreate with correct structure");
        } catch (SQLException e) {
            System.err.println("Error dropping table: " + e.getMessage());
        }
    }

    public boolean addChallan(Challan challan) {
        String sql = "INSERT INTO challans (challan_id, vehicle_number, violation, fine, status, issue_date, due_date, location) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        System.out.println("Attempting to add challan: " + challan.getChallanId());

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, challan.getChallanId());
            pstmt.setString(2, challan.getVehicleNumber());
            pstmt.setString(3, challan.getViolation());
            pstmt.setDouble(4, challan.getFine());
            pstmt.setString(5, challan.getStatus());
            pstmt.setString(6, challan.getIssueDate());
            pstmt.setString(7, challan.getDueDate());
            pstmt.setString(8, challan.getLocation());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Challan added successfully to database");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("SQL Error while adding challan: " + e.getMessage());
            return false;
        }
    }

    public List<Challan> getAllChallans() {
        List<Challan> challans = new ArrayList<>();
        String sql = "SELECT * FROM challans ORDER BY issue_date DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Challan challan = new Challan(
                        rs.getString("challan_id"),
                        rs.getString("vehicle_number"),
                        rs.getString("violation"),
                        rs.getDouble("fine"),
                        rs.getString("location")
                );
                challan.setStatus(rs.getString("status"));
                challan.setIssueDate(rs.getString("issue_date"));
                challan.setDueDate(rs.getString("due_date"));
                challans.add(challan);
            }
            System.out.println("Retrieved " + challans.size() + " challans from database");
        } catch (SQLException e) {
            System.err.println("Error retrieving challans: " + e.getMessage());
        }
        return challans;
    }

    public boolean payChallan(String challanId) {
        String sql = "UPDATE challans SET status = 'PAID' WHERE challan_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, challanId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Challan " + challanId + " marked as paid");
                return true;
            } else {
                System.err.println("Challan " + challanId + " not found for payment");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error paying challan: " + e.getMessage());
            return false;
        }
    }

    public List<Challan> searchByVehicle(String vehicleNumber) {
        List<Challan> challans = new ArrayList<>();
        String sql = "SELECT * FROM challans WHERE vehicle_number LIKE ? ORDER BY issue_date DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + vehicleNumber + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Challan challan = new Challan(
                        rs.getString("challan_id"),
                        rs.getString("vehicle_number"),
                        rs.getString("violation"),
                        rs.getDouble("fine"),
                        rs.getString("location")
                );
                challan.setStatus(rs.getString("status"));
                challan.setIssueDate(rs.getString("issue_date"));
                challan.setDueDate(rs.getString("due_date"));
                challans.add(challan);
            }
            System.out.println("Search for '" + vehicleNumber + "' returned " + challans.size() + " results");
        } catch (SQLException e) {
            System.err.println("Error searching challans: " + e.getMessage());
        }
        return challans;
    }

    // NEW: Get pending challans only
    public List<Challan> getPendingChallans() {
        List<Challan> challans = new ArrayList<>();
        String sql = "SELECT * FROM challans WHERE status = 'PENDING' ORDER BY issue_date DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Challan challan = new Challan(
                        rs.getString("challan_id"),
                        rs.getString("vehicle_number"),
                        rs.getString("violation"),
                        rs.getDouble("fine"),
                        rs.getString("location")
                );
                challan.setStatus(rs.getString("status"));
                challan.setIssueDate(rs.getString("issue_date"));
                challan.setDueDate(rs.getString("due_date"));
                challans.add(challan);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving pending challans: " + e.getMessage());
        }
        return challans;
    }

    // NEW: Get overdue challans
    public List<Challan> getOverdueChallans() {
        List<Challan> allChallans = getPendingChallans();
        List<Challan> overdueChallans = new ArrayList<>();

        for (Challan challan : allChallans) {
            if (challan.isOverdue()) {
                overdueChallans.add(challan);
            }
        }
        return overdueChallans;
    }

    // NEW: Get statistics
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        String sql = """
            SELECT 
                COUNT(*) as total,
                COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending,
                COUNT(CASE WHEN status = 'PAID' THEN 1 END) as paid,
                SUM(CASE WHEN status = 'PENDING' THEN fine ELSE 0 END) as pending_amount,
                SUM(CASE WHEN status = 'PAID' THEN fine ELSE 0 END) as collected_amount
            FROM challans
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                stats.put("total", rs.getInt("total"));
                stats.put("pending", rs.getInt("pending"));
                stats.put("paid", rs.getInt("paid"));
                stats.put("pending_amount", rs.getDouble("pending_amount"));
                stats.put("collected_amount", rs.getDouble("collected_amount"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting statistics: " + e.getMessage());
        }
        return stats;
    }

    // NEW: Delete a challan (for admin use)
    public boolean deleteChallan(String challanId) {
        String sql = "DELETE FROM challans WHERE challan_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, challanId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Challan " + challanId + " deleted successfully");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error deleting challan: " + e.getMessage());
            return false;
        }
    }

    public boolean testConnection() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            System.out.println("Database connection test: SUCCESS");
            return true;
        } catch (SQLException e) {
            System.err.println("Database connection test: FAILED");
            e.printStackTrace();
            return false;
        }
    }
}