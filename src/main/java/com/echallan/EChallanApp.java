package com.echallan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;

public class EChallanApp extends JFrame {
    private Database database;
    private JTextField vehicleField, violationField, fineField, searchField, locationField;
    private JTextArea displayArea;
    private JLabel statsLabel;
    private String[] commonViolations = {
            "Over Speeding", "Signal Jump", "Wrong Lane", "No Helmet", "Mobile Usage",
            "No Seat Belt", "Parking Violation", "Document Missing"
    };
    private double[] fineAmounts = {1500, 1000, 500, 1000, 1000, 500, 300, 200};
    private JComboBox<String> violationCombo;
    private JButton issueButton, searchButton, viewAllButton, payButton,
            pendingButton, overdueButton, statsButton, deleteButton;
    private List<Challan> currentChallans;

    public EChallanApp() {
        try {
            database = new Database();
            if (database.testConnection()) {
                System.out.println("Database initialized successfully");
            } else {
                System.err.println("Database connection failed");
                JOptionPane.showMessageDialog(null,
                        "Database connection failed! Please check your SQLite setup.",
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            System.err.println("Error initializing database:");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Failed to initialize database: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        setupGUI();
        updateStatistics(); // Load initial statistics
        viewAllChallans(null); // Load all challans on startup
    }

    private void setupGUI() {
        setTitle("Enhanced E-Challan System v2.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Top panel for issuing challan
        JPanel issuePanel = createIssuePanel();
        mainPanel.add(issuePanel, BorderLayout.NORTH);

        // Middle panel for search and operations
        JPanel operationsPanel = createOperationsPanel();
        mainPanel.add(operationsPanel, BorderLayout.CENTER);

        // Bottom panel for display and statistics
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(createStatsPanel(), BorderLayout.NORTH);
        bottomPanel.add(createDisplayPanel(), BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createIssuePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Issue New Challan"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Vehicle Number
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Vehicle Number:"), gbc);
        gbc.gridx = 1;
        vehicleField = new JTextField(15);
        panel.add(vehicleField, gbc);

        // Location
        gbc.gridx = 2; gbc.gridy = 0;
        panel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 3;
        locationField = new JTextField(15);
        panel.add(locationField, gbc);

        // Violation dropdown
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Violation:"), gbc);
        gbc.gridx = 1;
        violationCombo = new JComboBox<>(commonViolations);
        violationCombo.addActionListener(e -> {
            int index = violationCombo.getSelectedIndex();
            fineField.setText(String.valueOf(fineAmounts[index]));
        });
        panel.add(violationCombo, gbc);

        // Fine Amount
        gbc.gridx = 2; gbc.gridy = 1;
        panel.add(new JLabel("Fine Amount:"), gbc);
        gbc.gridx = 3;
        fineField = new JTextField("1500", 15);
        panel.add(fineField, gbc);

        // Issue Button
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 2;
        issueButton = new JButton("Issue Challan");
        issueButton.setBackground(new Color(76, 175, 80));
        issueButton.setForeground(Color.WHITE);
        issueButton.setFont(new Font("Arial", Font.BOLD, 12));
        issueButton.addActionListener(this::issueChallan);
        panel.add(issueButton, gbc);

        return panel;
    }

    private JPanel createOperationsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout());
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Challans"));

        searchPanel.add(new JLabel("Vehicle Number:"));
        searchField = new JTextField(15);
        searchPanel.add(searchField);

        searchButton = new JButton("Search");
        searchButton.addActionListener(this::searchChallans);
        searchPanel.add(searchButton);

        viewAllButton = new JButton("View All");
        viewAllButton.addActionListener(this::viewAllChallans);
        searchPanel.add(viewAllButton);

        // Action panel
        JPanel actionPanel = new JPanel(new FlowLayout());
        actionPanel.setBorder(BorderFactory.createTitledBorder("Actions"));

        pendingButton = new JButton("Pending Only");
        pendingButton.setBackground(new Color(255, 193, 7));
        pendingButton.setFont(new Font("Arial", Font.BOLD, 11));
        pendingButton.addActionListener(this::viewPendingChallans);
        actionPanel.add(pendingButton);

        overdueButton = new JButton("Overdue");
        overdueButton.setBackground(new Color(220, 53, 69));
        overdueButton.setForeground(Color.WHITE);
        overdueButton.setFont(new Font("Arial", Font.BOLD, 11));
        overdueButton.addActionListener(this::viewOverdueChallans);
        actionPanel.add(overdueButton);

        payButton = new JButton("Pay Challan");
        payButton.setBackground(new Color(40, 167, 69));
        payButton.setForeground(Color.WHITE);
        payButton.setFont(new Font("Arial", Font.BOLD, 11));
        payButton.addActionListener(this::payChallan);
        // Don't disable by default - let users know they need to select challans first
        actionPanel.add(payButton);

        deleteButton = new JButton("Delete Challan");
        deleteButton.setBackground(new Color(220, 53, 69));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFont(new Font("Arial", Font.BOLD, 11));
        deleteButton.addActionListener(this::deleteChallan);
        // Don't disable by default - let users know they need to select challans first
        actionPanel.add(deleteButton);

        statsButton = new JButton("Refresh Stats");
        statsButton.setBackground(new Color(23, 162, 184));
        statsButton.setForeground(Color.WHITE);
        statsButton.setFont(new Font("Arial", Font.BOLD, 11));
        statsButton.addActionListener(e -> {
            updateStatistics();
            viewAllChallans(e);
        });
        actionPanel.add(statsButton);

        panel.add(searchPanel);
        panel.add(actionPanel);
        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Statistics"));

        statsLabel = new JLabel("Loading statistics...");
        statsLabel.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(statsLabel);

        return panel;
    }

    private JPanel createDisplayPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Challan Details"));

        displayArea = new JTextArea(15, 60);
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        displayArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(displayArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // Event handlers
    private void issueChallan(ActionEvent e) {
        System.out.println("=== Issue Challan Button Clicked ===");

        String vehicle = vehicleField.getText().trim().toUpperCase();
        String violation = (String) violationCombo.getSelectedItem();
        String fineText = fineField.getText().trim();
        String location = locationField.getText().trim();

        if (vehicle.isEmpty() || fineText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill vehicle number and fine amount!");
            return;
        }

        try {
            double fine = Double.parseDouble(fineText);
            String challanId = "CH" + System.currentTimeMillis();

            Challan challan = new Challan(challanId, vehicle, violation, fine, location);

            boolean success = database.addChallan(challan);

            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Challan issued successfully!\nChallan ID: " + challanId +
                                "\nDue Date: " + challan.getDueDate(),
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                clearIssueFields();
                updateStatistics();
                viewAllChallans(e);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Failed to issue challan! Check console for details.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid fine amount!");
        }
    }

    private void searchChallans(ActionEvent e) {
        String vehicle = searchField.getText().trim().toUpperCase();
        if (vehicle.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter vehicle number!");
            return;
        }

        currentChallans = database.searchByVehicle(vehicle);
        displayChallans(currentChallans, "Search Results for: " + vehicle);
    }

    private void viewAllChallans(ActionEvent e) {
        currentChallans = database.getAllChallans();
        displayChallans(currentChallans, "All Challans");
    }

    private void viewPendingChallans(ActionEvent e) {
        currentChallans = database.getPendingChallans();
        displayChallans(currentChallans, "Pending Challans");
    }

    private void viewOverdueChallans(ActionEvent e) {
        currentChallans = database.getOverdueChallans();
        displayChallans(currentChallans, "Overdue Challans (with Penalty)");
    }

    private void payChallan(ActionEvent e) {
        if (currentChallans == null || currentChallans.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No challans loaded! Please use 'View All' or 'Search' to load challans first.",
                    "No Challans", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Show available challans for payment
        StringBuilder availableChallans = new StringBuilder();
        availableChallans.append("Available Challans for Payment:\n\n");
        boolean hasPendingChallans = false;

        for (Challan c : currentChallans) {
            if ("PENDING".equals(c.getStatus())) {
                availableChallans.append("ID: ").append(c.getChallanId())
                        .append(" | Vehicle: ").append(c.getVehicleNumber())
                        .append(" | Amount: ₹").append(String.format("%.2f", c.getTotalAmount()))
                        .append("\n");
                hasPendingChallans = true;
            }
        }

        if (!hasPendingChallans) {
            JOptionPane.showMessageDialog(this, "No pending challans available for payment!");
            return;
        }

        String challanId = JOptionPane.showInputDialog(this,
                availableChallans.toString() + "\nEnter Challan ID to pay:");

        if (challanId != null && !challanId.trim().isEmpty()) {
            // Find the challan to show payment details
            Challan challanToPay = null;
            for (Challan c : currentChallans) {
                if (c.getChallanId().equals(challanId.trim())) {
                    challanToPay = c;
                    break;
                }
            }

            if (challanToPay != null && "PENDING".equals(challanToPay.getStatus())) {
                double totalAmount = challanToPay.getTotalAmount();
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Payment Details:\n" +
                                "Challan ID: " + challanToPay.getChallanId() + "\n" +
                                "Vehicle: " + challanToPay.getVehicleNumber() + "\n" +
                                "Fine: ₹" + challanToPay.getFine() + "\n" +
                                "Penalty: ₹" + String.format("%.2f", challanToPay.getPenaltyAmount()) + "\n" +
                                "Total Amount: ₹" + String.format("%.2f", totalAmount) + "\n\n" +
                                "Proceed with payment?",
                        "Confirm Payment", JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    if (database.payChallan(challanId.trim())) {
                        JOptionPane.showMessageDialog(this,
                                "Payment successful!\nAmount Paid: ₹" + String.format("%.2f", totalAmount));
                        updateStatistics();
                        viewAllChallans(e);
                    } else {
                        JOptionPane.showMessageDialog(this, "Payment failed!");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Challan not found or already paid!");
            }
        }
    }

    private void deleteChallan(ActionEvent e) {
        if (currentChallans == null || currentChallans.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No challans loaded! Please use 'View All' or 'Search' to load challans first.",
                    "No Challans", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Show available challans for deletion
        StringBuilder availableChallans = new StringBuilder();
        availableChallans.append("Available Challans:\n\n");

        for (Challan c : currentChallans) {
            availableChallans.append("ID: ").append(c.getChallanId())
                    .append(" | Vehicle: ").append(c.getVehicleNumber())
                    .append(" | Status: ").append(c.getStatus())
                    .append("\n");
        }

        String challanId = JOptionPane.showInputDialog(this,
                availableChallans.toString() + "\nEnter Challan ID to delete:\n(Warning: This action cannot be undone!)");

        if (challanId != null && !challanId.trim().isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete challan: " + challanId.trim() + "?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                if (database.deleteChallan(challanId.trim())) {
                    JOptionPane.showMessageDialog(this, "Challan deleted successfully!");
                    updateStatistics();
                    viewAllChallans(e);
                } else {
                    JOptionPane.showMessageDialog(this, "Challan not found!");
                }
            }
        }
    }

    private void displayChallans(List<Challan> challans, String title) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");
        sb.append("=".repeat(80)).append("\n\n");

        if (challans.isEmpty()) {
            sb.append("No challans found.\n");
        } else {
            for (Challan challan : challans) {
                sb.append(challan.toString()).append("\n");
                sb.append("-".repeat(80)).append("\n");
            }
        }
        sb.append("\nTotal Challans: ").append(challans.size());

        // Calculate totals for current view
        double totalFines = challans.stream().mapToDouble(Challan::getFine).sum();
        double totalPenalties = challans.stream().mapToDouble(Challan::getPenaltyAmount).sum();
        long overdueCount = challans.stream().filter(Challan::isOverdue).count();

        sb.append(" | Total Fine Amount: ₹").append(String.format("%.2f", totalFines));
        if (totalPenalties > 0) {
            sb.append(" | Total Penalties: ₹").append(String.format("%.2f", totalPenalties));
        }
        if (overdueCount > 0) {
            sb.append(" | Overdue: ").append(overdueCount);
        }

        displayArea.setText(sb.toString());
    }

    private void updateStatistics() {
        Map<String, Object> stats = database.getStatistics();

        StringBuilder sb = new StringBuilder();
        sb.append("Total: ").append(stats.get("total"))
                .append(" | Pending: ").append(stats.get("pending"))
                .append(" | Paid: ").append(stats.get("paid"))
                .append(" | Pending Amount: ₹").append(String.format("%.2f", (Double)stats.get("pending_amount")))
                .append(" | Collected: ₹").append(String.format("%.2f", (Double)stats.get("collected_amount")));

        statsLabel.setText(sb.toString());
    }

    private void clearIssueFields() {
        vehicleField.setText("");
        locationField.setText("");
        fineField.setText("1500");
        violationCombo.setSelectedIndex(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new EChallanApp().setVisible(true);
        });
    }
}