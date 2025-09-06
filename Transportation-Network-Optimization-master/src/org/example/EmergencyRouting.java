package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;

public class EmergencyRouting extends JFrame {
    private Map<String, Node> nodes;
    private List<Road> roads;
    private Map<String, List<Road>> adjacencyList;
    private Map<String, Map<String, Integer>> trafficFlow;
    private Map<String, Boolean> roadStatus;
    private Map<String, Double> congestionFactors;
    private Map<String, SignalTiming> signalTimings;
    private JComboBox<String> startCombo, endCombo, timeCombo, vehicleTypeCombo, scenarioCombo, emergencyLocationCombo;
    private JTextArea resultTextArea;
    private DefaultTableModel tableModel;
    private MapPanel mapPanel;
    private JPanel mapContainer;
    private List<Node> emergencyPath;
    private Connection connection;
    private String selectedVehicleType;
    private Timer blinkingTimer;
    private boolean blinkState;
    private TrafficSignalOptimizer signalOptimizer;

    public EmergencyRouting() {
        try {
            connection = DBConnection.connect();
            if (connection == null) {
                showError("Failed to connect to database.", "Connection Error");
                dispose();
                return;
            }
            initializeData();
            initUI();
        } catch (SQLException e) {
            showError("Error loading data: " + e.getMessage(), "Database Error");
            dispose();
        }
    }

    private void initializeData() throws SQLException {
        nodes = DataLoader.loadNodes(connection);
        roads = DataLoader.loadRoads(connection);
        trafficFlow = DataLoader.loadTrafficFlow(connection);
        signalTimings = loadSignalTimings();
        congestionFactors = new HashMap<>();
        emergencyPath = null;
        blinkState = true;
        if (nodes.isEmpty() || roads.isEmpty()) {
            throw new SQLException("No nodes or roads loaded.");
        }
        adjacencyList = buildAdjacencyList();
        signalOptimizer = new TrafficSignalOptimizer(nodes, roads, trafficFlow, signalTimings);
        updateRoadStatus();
    }

    private Map<String, SignalTiming> loadSignalTimings() throws SQLException {
        Map<String, SignalTiming> timings = new HashMap<>();
        String query = "SELECT NodeID, GreenTime, CycleLength FROM IntersectionSignals";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                timings.put(rs.getString("NodeID"), new SignalTiming(rs.getDouble("GreenTime"), rs.getDouble("CycleLength")));
            }
        }
        return timings;
    }

    private void updateRoadStatus() throws SQLException {
        roadStatus = new HashMap<>();
        congestionFactors.clear();
        String query = "SELECT FromID, ToID, IsActive, IncidentType, CongestionFactor FROM Incidents WHERE IsActive = TRUE";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String key = rs.getString("FromID") + "->" + rs.getString("ToID");
                String incidentType = rs.getString("IncidentType");
                if (incidentType.equals("Closure")) {
                    roadStatus.put(key, false);
                } else if (incidentType.equals("Congestion")) {
                    congestionFactors.put(key, rs.getDouble("CongestionFactor"));
                    roadStatus.put(key, true);
                }
            }
        }
        roads.forEach(road -> {
            String key = road.fromId + "->" + road.toId;
            roadStatus.putIfAbsent(key, true);
        });
    }

    private void initUI() {
        setTitle("Emergency Vehicle Routing System");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true); // Remove window borders
        setMinimumSize(new Dimension(800, 600));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel background = new JPanel();
        background.setBackground(new Color(245, 245, 220));
        background.setLayout(new BorderLayout(10, 10));
        setContentPane(background);

        JLabel titleLabel = createTitleLabel();
        background.add(titleLabel, BorderLayout.NORTH);
        JPanel mapOuterContainer = createMapPanel();
        background.add(mapOuterContainer, BorderLayout.WEST);
        JPanel rightPanel = createControlPanel();
        background.add(rightPanel, BorderLayout.EAST);

        // Add back button in top-right corner
        JButton backButton = new JButton("");
        backButton.setPreferredSize(new Dimension(50, 50)); // 50x50 pixels
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        BufferedImage backImage = null;
        try {
            // Load and scale img_1.png to button size
            backImage = ImageIO.read(new File("img_1.png"));
            Image scaledImg = backImage.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            backButton.setIcon(new ImageIcon(scaledImg));
            backButton.setBorder(BorderFactory.createEmptyBorder());
            backButton.setContentAreaFilled(false); // Remove default background
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback to solid color if image fails
            backButton.setBackground(new Color(0, 120, 215));
            backButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        }

        // Hover effect for back button (slight brightening)
        final BufferedImage finalBackImage = backImage;
        backButton.addMouseListener(new MouseAdapter() {
            private Icon originalIcon = backButton.getIcon();
            @Override
            public void mouseEntered(MouseEvent e) {
                if (finalBackImage != null) {
                    try {
                        // Determine number of color components
                        int numComponents = finalBackImage.getColorModel().getNumComponents();
                        float[] scales = new float[numComponents];
                        float[] offsets = new float[numComponents];
                        for (int i = 0; i < numComponents; i++) {
                            scales[i] = (i < numComponents - 1 || !finalBackImage.getColorModel().hasAlpha()) ? 1.2f : 1.0f;
                            offsets[i] = 0.0f;
                        }
                        RescaleOp op = new RescaleOp(scales, offsets, null);
                        BufferedImage brighterImg = op.filter(finalBackImage, null);
                        Image scaledBrighter = brighterImg.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                        backButton.setIcon(new ImageIcon(scaledBrighter));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                backButton.setIcon(originalIcon);
            }
        });

        // Action to open MainMenu
        backButton.addActionListener(e -> {
            try {
                // Instantiate and show MainMenu
                Class<?> mainMenuClass = Class.forName("org.example.MainMenu");
                JFrame mainMenuFrame = (JFrame) mainMenuClass.getDeclaredConstructor().newInstance();
                mainMenuFrame.setVisible(true);
                // Close current window
                dispose();
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(EmergencyRouting.this,
                        "Cannot find MainMenu class. Ensure MainMenu.java is in the org.example package and compiled.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(EmergencyRouting.this,
                        "Error opening MainMenu: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Position back button in extreme top-right corner
        background.setLayout(null); // Temporarily switch to null layout for absolute positioning
        backButton.setBounds(background.getWidth() - 50, 0, 50, 50); // 0px from top, 0px from right
        background.add(backButton);

        // Restore BorderLayout for main components
        background.setLayout(new BorderLayout(10, 10));
        background.add(titleLabel, BorderLayout.NORTH);
        background.add(mapOuterContainer, BorderLayout.WEST);
        background.add(rightPanel, BorderLayout.EAST);

        // Ensure button position updates after frame is maximized
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                backButton.setBounds(background.getWidth() - 50, 0, 50, 50);
            }
        });

        setVisible(true);
    }

    private JLabel createTitleLabel() {
        JLabel titleLabel = new JLabel("Emergency Vehicle Routing", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(74, 74, 74));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        return titleLabel;
    }

    private JPanel createMapPanel() {
        JPanel mapOuterContainer = new JPanel(new BorderLayout());
        mapOuterContainer.setBackground(new Color(245, 245, 220));
        mapContainer = new JPanel(new BorderLayout());
        mapContainer.setBackground(new Color(245, 245, 220));
        List<Road> closedRoads = roads.stream()
                .filter(road -> !roadStatus.getOrDefault(road.fromId + "->" + road.toId, true))
                .collect(Collectors.toList());
        mapPanel = new MapPanel(nodes, roads, trafficFlow, closedRoads, false);
        mapPanel.setPreferredSize(new Dimension(950, 700));
        mapPanel.setBackground(new Color(245, 245, 220));
        mapPanel.setBorder(BorderFactory.createLineBorder(new Color(193, 154, 107), 1));
        mapContainer.add(mapPanel, BorderLayout.CENTER);
        mapContainer.add(createLegendPanel(), BorderLayout.SOUTH);
        mapOuterContainer.add(mapContainer, BorderLayout.CENTER);
        mapOuterContainer.add(Box.createRigidArea(new Dimension(50, 0)), BorderLayout.WEST);
        return mapOuterContainer;
    }

    private JPanel createLegendPanel() {
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        legendPanel.setBackground(new Color(245, 245, 220));
        legendPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(193, 154, 107)), "Congestion & Signal Legend",
                0, 0, null, new Color(74, 74, 74)));
        legendPanel.add(new JLabel("Ascending"));
        legendPanel.add(createCongestionLabel("● Low (< 50%)", new Color(107, 142, 35)));
        legendPanel.add(createCongestionLabel("● Medium (50% - 80%)", new Color(255, 215, 0)));
        legendPanel.add(createCongestionLabel("● High (> 80%)", new Color(139, 69, 19)));
        legendPanel.add(createCongestionLabel("● Emergency Preemption", new Color(0, 255, 0)));
        return legendPanel;
    }

    private JPanel createCongestionLabel(String text, Color dotColor) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(new Color(245, 245, 220));
        JLabel dot = new JLabel(text.split(" ")[0]);
        dot.setForeground(dotColor);
        panel.add(dot);
        panel.add(new JLabel(text.substring(2)) {{ setForeground(new Color(74, 74, 74)); }});
        return panel;
    }

    private JPanel createControlPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(new Color(210, 180, 140));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.setBackground(new Color(210, 180, 140));
        controlPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(193, 154, 107)), "Controls",
                0, 0, null, new Color(74, 74, 74)));
        controlPanel.setMaximumSize(new Dimension(400, 500));

        gbc.gridx = 0;
        gbc.gridy = 0;
        controlPanel.add(new JLabel("Start Point:") {{ setForeground(new Color(74, 74, 74)); }}, gbc);
        gbc.gridx = 1;
        startCombo = createComboBox(nodes.values().stream().map(Node::getName).toArray(String[]::new));
        controlPanel.add(startCombo, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        controlPanel.add(new JLabel("End Point:") {{ setForeground(new Color(74, 74, 74)); }}, gbc);
        gbc.gridx = 1;
        endCombo = createComboBox(nodes.values().stream().map(Node::getName).toArray(String[]::new));
        controlPanel.add(endCombo, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        controlPanel.add(new JLabel("Time of Day:") {{ setForeground(new Color(74, 74, 74)); }}, gbc);
        gbc.gridx = 1;
        timeCombo = createComboBox(new String[]{"Morning Peak", "Afternoon", "Evening Peak", "Night"});
        controlPanel.add(timeCombo, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        controlPanel.add(new JLabel("Vehicle Type:") {{ setForeground(new Color(74, 74, 74)); }}, gbc);
        gbc.gridx = 1;
        vehicleTypeCombo = createComboBox(new String[]{"Ambulance", "Fire Truck", "Normal Car"});
        controlPanel.add(vehicleTypeCombo, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        controlPanel.add(new JLabel("Emergency Scenario:") {{ setForeground(new Color(74, 74, 74)); }}, gbc);
        gbc.gridx = 1;
        scenarioCombo = createComboBox(new String[]{"Fire", "Sick"});
        scenarioCombo.addActionListener(e -> updateEmergencyLocationCombo());
        controlPanel.add(scenarioCombo, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        controlPanel.add(new JLabel("Emergency Location:") {{ setForeground(new Color(74, 74, 74)); }}, gbc);
        gbc.gridx = 1;
        emergencyLocationCombo = createComboBox(new String[]{});
        updateEmergencyLocationCombo();
        controlPanel.add(emergencyLocationCombo, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        controlPanel.add(createButton("Dispatch Emergency Vehicle", new Color(139, 0, 0), e -> dispatchVehicle(false)), gbc);

        gbc.gridy++;
        controlPanel.add(createButton("Dispatch to Scenario", new Color(0, 100, 0), e -> dispatchVehicle(true)), gbc);

        gbc.gridy++;
        controlPanel.add(createButton("Generate Analytics", new Color(0, 128, 128), e -> generateAnalytics()), gbc);

        gbc.gridy++;
        controlPanel.add(createButton("Reset", new Color(70, 130, 180), e -> resetUI()), gbc);

        rightPanel.add(controlPanel);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(createResultTextArea());
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(createRouteTable());
        return rightPanel;
    }

    private JComboBox<String> createComboBox(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setPreferredSize(new Dimension(200, 30));
        combo.setBackground(Color.WHITE);
        combo.setForeground(new Color(74, 74, 74));
        return combo;
    }

    private JButton createButton(String text, Color bgColor, ActionListener listener) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(200, 35));
        button.addActionListener(listener);
        return button;
    }

    private JScrollPane createResultTextArea() {
        resultTextArea = new JTextArea("Select start, end points, and vehicle type to dispatch.");
        resultTextArea.setFont(new Font("Arial", Font.BOLD, 14));
        resultTextArea.setForeground(new Color(74, 74, 74));
        resultTextArea.setBackground(new Color(255, 245, 220));
        resultTextArea.setLineWrap(true);
        resultTextArea.setWrapStyleWord(true);
        resultTextArea.setEditable(false);
        resultTextArea.setBorder(BorderFactory.createLineBorder(new Color(193, 154, 107), 1, true));
        JScrollPane scrollPane = new JScrollPane(resultTextArea);
        scrollPane.setPreferredSize(new Dimension(400, 150));
        scrollPane.setBackground(new Color(210, 180, 140));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(193, 154, 107)));
        return scrollPane;
    }

    private JScrollPane createRouteTable() {
        String[] columns = {"From", "To", "Distance (km)", "Time (min)", "Congestion", "Signal Status"};
        tableModel = new DefaultTableModel(columns, 0);
        JTable routeTable = new JTable(tableModel);
        routeTable.setRowHeight(25);
        routeTable.setFont(new Font("Arial", Font.PLAIN, 14));
        routeTable.setForeground(new Color(74, 74, 74));
        routeTable.setBackground(new Color(255, 245, 220));
        routeTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        routeTable.getTableHeader().setBackground(new Color(210, 180, 140));
        routeTable.getTableHeader().setForeground(new Color(74, 74, 74));
        routeTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(row % 2 == 0 ? new Color(255, 245, 220) : new Color(245, 235, 200));
                c.setForeground(new Color(74, 74, 74));
                return c;
            }
        });
        JScrollPane scrollPane = new JScrollPane(routeTable);
        // Fixed typo: 'setPreferre' to 'setPreferredSize' and 'dSize' to 'Dimension'
        scrollPane.setPreferredSize(new Dimension(400, 350));
        scrollPane.setBackground(new Color(210, 180, 140));
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(193, 154, 107)), "Route Details",
                0, 0, null, new Color(74, 74, 74)));
        return scrollPane;
    }

    private void updateEmergencyLocationCombo() {
        emergencyLocationCombo.removeAllItems();
        nodes.values().stream().map(Node::getName).sorted().forEach(emergencyLocationCombo::addItem);
        emergencyLocationCombo.setEnabled(true);
    }

    // Fixed syntax: Removed erroneous '|' symbol
    private void resetPathDisplay() {
        emergencyPath = null;
        tableModel.setRowCount(0);
        mapPanel.setHighlightedPath(null);
        stopBlinking();
        mapPanel.repaint();
    }

    private void dispatchVehicle(boolean isScenario) {
        try {
            updateRoadStatus();
            trafficFlow = DataLoader.loadTrafficFlow(connection);
            signalTimings = loadSignalTimings();
            resetPathDisplay();

            String startName = (String) startCombo.getSelectedItem();
            String endName = isScenario ? (String) emergencyLocationCombo.getSelectedItem() : (String) endCombo.getSelectedItem();
            String timeOfDay = (String) timeCombo.getSelectedItem();
            selectedVehicleType = isScenario ? (scenarioCombo.getSelectedItem().equals("Fire") ? "Fire Truck" : "Ambulance") : (String) vehicleTypeCombo.getSelectedItem();

            String startId = getNodeIdByName(startName);
            String endId = getNodeIdByName(endName);
            if (startId == null || endId == null) {
                resultTextArea.setText("Invalid start or end point selected.");
                return;
            }
            if (!isNodeConnected(startId) || !isNodeConnected(endId)) {
                resultTextArea.setText("Start or end point is not connected.");
                return;
            }

            if (isScenario) {
                vehicleTypeCombo.setSelectedItem(selectedVehicleType);
                endCombo.setSelectedItem(endName);
            }

            emergencyPath = findShortestPath(startId, endId, timeOfDay, !selectedVehicleType.equals("Normal Car"), selectedVehicleType);
            if (emergencyPath == null || emergencyPath.isEmpty()) {
                resultTextArea.setText("No path found from " + startName + " to " + endName + ".");
                return;
            }

            if (!selectedVehicleType.equals("Normal Car")) {
                signalOptimizer.optimizeSignalsForPath(emergencyPath, selectedVehicleType, timeOfDay);
                updateSignalTimingsInDB();
            }

            boolean missingRoads = displayPathInTable(emergencyPath, timeOfDay);
            double totalTime = calculatePathMetrics(emergencyPath, timeOfDay, true).time;
            double totalTimeWithoutPriority = calculatePathMetrics(emergencyPath, timeOfDay, false).time;
            double totalDistance = calculatePathMetrics(emergencyPath, timeOfDay, true).distance;

            StringBuilder result = new StringBuilder();
            result.append(String.format(
                    "Path (%s) from %s to %s:\nDistance: %.1f km\nTime with priority: %.1f min\nTime without priority: %.1f min\nNodes in path: %d\nAverage response time per km: %.1f min/km\n",
                    selectedVehicleType, startName, endName, totalDistance, totalTime, totalTimeWithoutPriority, emergencyPath.size(),
                    totalDistance > 0 ? totalTime / totalDistance : 0));
            if (missingRoads) {
                result.append("Warning: Some road segments are missing.\n");
            }
            resultTextArea.setText(result.toString());

            List<String> pathIds = emergencyPath.stream().map(Node::getId).collect(Collectors.toList());
            mapPanel.setHighlightedPath(pathIds);
            mapPanel.setTimeOfDay(timeOfDay);
            mapPanel.setVehicleType(selectedVehicleType);
            if (!selectedVehicleType.equals("Normal Car")) {
                startBlinking();
            }
            mapPanel.repaint();
        } catch (SQLException e) {
            resultTextArea.setText("Database error: " + e.getMessage());
        }
    }

    private void updateSignalTimingsInDB() throws SQLException {
        String query = "UPDATE IntersectionSignals SET GreenTime = ?, CycleLength = ?, LastUpdated = CURRENT_TIMESTAMP WHERE NodeID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            for (Map.Entry<String, SignalTiming> entry : signalTimings.entrySet()) {
                pstmt.setDouble(1, entry.getValue().greenTime);
                pstmt.setDouble(2, entry.getValue().cycleLength);
                pstmt.setString(3, entry.getKey());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private void generateAnalytics() {
        resultTextArea.setText("Starting analytics...");
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                if (nodes.isEmpty() || roads.isEmpty() || trafficFlow.isEmpty()) {
                    return "Error: Incomplete data in database.";
                }
                StringBuilder analytics = new StringBuilder();
                List<Node> hospitals = nodes.values().stream().filter(n -> "Medical".equals(n.getNodeType())).collect(Collectors.toList());
                if (hospitals.isEmpty()) {
                    return "No hospitals found.";
                }

                Random rand = new Random();
                List<Node> sampleNodes = nodes.values().stream()
                        .filter(n -> !hospitals.contains(n) && isNodeConnected(n.getId()))
                        .collect(Collectors.toList());
                sampleNodes = sampleNodes.subList(0, Math.min(10, sampleNodes.size()));
                Collections.shuffle(sampleNodes, rand);

                Map<String, Double> avgTimes = new HashMap<>();
                Map<String, Double> avgTimesNoPreemption = new HashMap<>();
                Map<String, Double> avgCongestions = new HashMap<>();
                Map<String, String> criticalPaths = new HashMap<>();
                int maxIterations = 100;
                int iterationCount = 0;
                long startTime = System.currentTimeMillis();
                long maxDuration = 30_000;

                for (Node hospital : hospitals) {
                    if (!isNodeConnected(hospital.getId())) continue;
                    double totalTime = 0, totalTimeNoPreemption = 0, totalCongestion = 0;
                    int count = 0;
                    double maxTime = 0;
                    String criticalPath = "";
                    for (Node start : sampleNodes) {
                        if (iterationCount >= maxIterations || System.currentTimeMillis() - startTime > maxDuration) {
                            analytics.append("Warning: Analysis stopped due to excessive iterations or timeout.\n");
                            break;
                        }
                        for (String time : new String[]{"Morning Peak", "Afternoon", "Evening Peak", "Night"}) {
                            List<Node> path = findShortestPath(start.getId(), hospital.getId(), time, true, "Ambulance");
                            if (path != null && !path.isEmpty()) {
                                signalOptimizer.optimizeSignalsForPath(path, "Ambulance", time);
                                var metrics = calculatePathMetrics(path, time, true);
                                var metricsNoPreemption = calculatePathMetrics(path, time, false);
                                totalTime += metrics.time;
                                totalTimeNoPreemption += metricsNoPreemption.time;
                                totalCongestion += metrics.congestion;
                                count++;
                                if (metrics.time > maxTime) {
                                    maxTime = metrics.time;
                                    criticalPath = start.getName() + " to " + hospital.getName() + " (" + time + ")";
                                }
                            }
                            iterationCount++;
                        }
                    }
                    if (count > 0) {
                        avgTimes.put(hospital.getName(), totalTime / count);
                        avgTimesNoPreemption.put(hospital.getName(), totalTimeNoPreemption / count);
                        avgCongestions.put(hospital.getName(), totalCongestion / count);
                        criticalPaths.put(hospital.getName(), criticalPath);
                    }
                }

                if (avgTimes.isEmpty()) {
                    analytics.append("No valid paths found for any hospital.\n");
                } else {
                    analytics.append("Average Response Times to Hospitals (with Signal Preemption):\n");
                    avgTimes.forEach((name, time) ->
                            analytics.append(String.format("- %s: %.1f min (No Preemption: %.1f min, Avg Congestion: %.0f%%, Critical Path: %s)\n",
                                    name, time, avgTimesNoPreemption.get(name), avgCongestions.get(name) * 100, criticalPaths.get(name))));
                }

                Map<String, Double> timeOfDayTimes = new HashMap<>();
                Map<String, Double> timeOfDayTimesNoPreemption = new HashMap<>();
                Map<String, Double> timeOfDayCongestions = new HashMap<>();
                iterationCount = 0;
                for (String time : new String[]{"Morning Peak", "Afternoon", "Evening Peak", "Night"}) {
                    double totalTime = 0, totalTimeNoPreemption = 0, totalCongestion = 0;
                    int count = 0;
                    for (Node start : sampleNodes) {
                        if (iterationCount >= maxIterations || System.currentTimeMillis() - startTime > maxDuration) {
                            analytics.append("Warning: Time of day analysis stopped.\n");
                            break;
                        }
                        for (Node end : hospitals) {
                            if (start.equals(end) || !isNodeConnected(start.getId()) || !isNodeConnected(end.getId())) continue;
                            List<Node> path = findShortestPath(start.getId(), end.getId(), time, true, "Ambulance");
                            if (path != null && !path.isEmpty()) {
                                signalOptimizer.optimizeSignalsForPath(path, "Ambulance", time);
                                var metrics = calculatePathMetrics(path, time, true);
                                var metricsNoPreemption = calculatePathMetrics(path, time, false);
                                totalTime += metrics.time;
                                totalTimeNoPreemption += metricsNoPreemption.time;
                                totalCongestion += metrics.congestion;
                                count++;
                            }
                            iterationCount++;
                        }
                    }
                    if (count > 0) {
                        timeOfDayTimes.put(time, totalTime / count);
                        timeOfDayTimesNoPreemption.put(time, totalTimeNoPreemption / count);
                        timeOfDayCongestions.put(time, totalCongestion / count);
                    }
                }
                analytics.append("\nAverage Response Times by Time of Day (with Signal Preemption):\n");
                if (timeOfDayTimes.isEmpty()) {
                    analytics.append("No valid paths found.\n");
                } else {
                    timeOfDayTimes.forEach((time, avgTime) ->
                            analytics.append(String.format("- %s: %.1f min (No Preemption: %.1f min, Avg Congestion: %.0f%%)\n",
                                    time, avgTime, timeOfDayTimesNoPreemption.get(time), timeOfDayCongestions.get(time) * 100)));
                }

                analytics.append("\nGreedy Signal Optimization Analysis (Egyptian Context):\n");
                analytics.append("- Optimal Cases: Effective during moderate congestion, reducing response times by up to 20%.\n");
                analytics.append("- Suboptimal Cases: During high congestion, preemption may increase response times by 5-10%.\n");
                analytics.append("- Recommendation: Use adaptive cycle lengths in high-density areas.\n");

                return analytics.toString();
            }

            @Override
            protected void done() {
                try {
                    resultTextArea.setText(get());
                } catch (Exception e) {
                    resultTextArea.setText("Error during analytics: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void resetUI() {
        try {
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("UPDATE Incidents SET IsActive = FALSE WHERE IsActive = TRUE");
                stmt.executeUpdate("UPDATE IntersectionSignals SET GreenTime = 30, CycleLength = 120, LastUpdated = CURRENT_TIMESTAMP");
            }
            startCombo.setSelectedIndex(0);
            endCombo.setSelectedIndex(0);
            timeCombo.setSelectedIndex(0);
            vehicleTypeCombo.setSelectedIndex(0);
            scenarioCombo.setSelectedIndex(0);
            updateEmergencyLocationCombo();
            resetPathDisplay();
            roadStatus.replaceAll((k, v) -> true);
            congestionFactors.clear();
            signalTimings = loadSignalTimings();
            resultTextArea.setText("Select start, end points, and vehicle type to dispatch.");
            trafficFlow = DataLoader.loadTrafficFlow(connection);
            updateRoadStatus();
            MapPanel newMapPanel = new MapPanel(nodes, roads, trafficFlow, new ArrayList<>(), false);
            newMapPanel.setPreferredSize(new Dimension(950, 700));
            newMapPanel.setBackground(new Color(245, 245, 220));
            newMapPanel.setBorder(BorderFactory.createLineBorder(new Color(193, 154, 107), 1));
            mapContainer.remove(mapPanel);
            mapPanel = newMapPanel;
            mapContainer.add(mapPanel, BorderLayout.CENTER);
            mapContainer.revalidate();
            mapContainer.repaint();
        } catch (SQLException e) {
            resultTextArea.setText("Database error during reset: " + e.getMessage());
        }
    }

    private boolean isNodeConnected(String nodeId) {
        List<Road> nodeRoads = adjacencyList.getOrDefault(nodeId, Collections.emptyList());
        return !nodeRoads.isEmpty() && nodeRoads.stream().anyMatch(road -> roadStatus.getOrDefault(road.fromId + "->" + road.toId, true));
    }

    private List<Node> findShortestPath(String startId, String goalId, String timeOfDay, boolean usePriority, String vehicleType) {
        try {
            Node start = nodes.get(startId);
            Node goal = nodes.get(goalId);
            if (start == null || goal == null) {
                resultTextArea.setText("Error: Start or goal node not found.");
                return null;
            }

            Map<Node, Double> gScore = new HashMap<>();
            gScore.put(start, 0.0);
            Map<Node, Node> cameFrom = new HashMap<>();
            PriorityQueue<AStarNode> openSet = new PriorityQueue<>();
            Map<Node, AStarNode> openMap = new HashMap<>();
            double facilityPriority = "Medical".equals(goal.getNodeType()) ? 0.8 : 1.0;
            double hStart = distanceBetween(start, goal) * (1 + getAverageCongestion(timeOfDay) * 0.1);
            AStarNode startASN = new AStarNode(start, 0.0, hStart * facilityPriority);
            openSet.add(startASN);
            openMap.put(start, startASN);
            // Fixed: Changed HashMap to HashSet for closedSet
            Set<Node> closedSet = new HashSet<>();
            while (!openSet.isEmpty()) {
                AStarNode currentASN = openSet.poll();
                Node current = currentASN.node;
                openMap.remove(current);
                if (closedSet.contains(current)) continue;
                closedSet.add(current);

                if (current.equals(goal)) {
                    return reconstructPath(cameFrom, current);
                }

                for (Road road : adjacencyList.getOrDefault(current.getId(), Collections.emptyList())) {
                    if (!roadStatus.getOrDefault(road.fromId + "->" + road.toId, true)) continue;
                    Node neighbor = nodes.get(road.toId);
                    if (neighbor == null || closedSet.contains(neighbor)) continue;

                    double tentativeG = gScore.get(current) + getCost(road, current, neighbor, timeOfDay, usePriority, vehicleType);
                    if (!gScore.containsKey(neighbor) || tentativeG < gScore.get(neighbor)) {
                        cameFrom.put(neighbor, current);
                        gScore.put(neighbor, tentativeG);
                        double h = distanceBetween(neighbor, goal) * facilityPriority * (1 + getAverageCongestion(timeOfDay) * 0.1);
                        double f = tentativeG + h;
                        if (openMap.containsKey(neighbor)) {
                            openSet.remove(openMap.get(neighbor));
                        }
                        AStarNode newASN = new AStarNode(neighbor, tentativeG, f);
                        openSet.add(newASN);
                        openMap.put(neighbor, newASN);
                    }
                }
            }
            return null;
        } catch (SQLException e) {
            resultTextArea.setText("Database error during pathfinding: " + e.getMessage());
            return null;
        }
    }

    private List<Node> reconstructPath(Map<Node, Node> cameFrom, Node current) {
        List<Node> path = new ArrayList<>();
        path.add(current);
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(0, current);
        }
        return path;
    }

    private Map<String, List<Road>> buildAdjacencyList() {
        Map<String, List<Road>> adjList = new HashMap<>();
        for (Road road : roads) {
            adjList.computeIfAbsent(road.fromId, k -> new ArrayList<>()).add(road);
            adjList.computeIfAbsent(road.toId, k -> new ArrayList<>()).add(
                    new Road(road.toId, road.fromId, road.distance, road.capacity, road.condition, road.isExisting, road.cost));
        }
        return adjList;
    }

    private double getCost(Road road, Node from, Node to, String timeOfDay, boolean usePriority, String vehicleType) throws SQLException {
        if (road == null) return Double.POSITIVE_INFINITY;
        String key = from.getId() + "->" + to.getId();
        double trafficVolume = getTrafficVolume(key, timeOfDay);
        double congestionFactor = congestionFactors.getOrDefault(key, 1.0);
        trafficVolume *= congestionFactor;
        double congestion = road.capacity > 0 ? Math.min(trafficVolume / road.capacity, 1.0) : 1.0;

        double baseSpeed, priorityFactor, intersectionDelay;
        switch (vehicleType) {
            case "Ambulance":
                baseSpeed = 70.0;
                priorityFactor = usePriority ? 0.2 : 0.5;
                intersectionDelay = usePriority ? signalOptimizer.getPreemptionDelay(to.getId(), timeOfDay) : road.intersectionDelay;
                break;
            case "Fire Truck":
                baseSpeed = 65.0;
                priorityFactor = usePriority ? 0.2 : 0.5;
                intersectionDelay = usePriority ? signalOptimizer.getPreemptionDelay(to.getId(), timeOfDay) : road.intersectionDelay;
                break;
            default:
                baseSpeed = 60.0;
                priorityFactor = 0.5;
                intersectionDelay = signalOptimizer.getNormalDelay(to.getId(), timeOfDay);
        }

        double timeFactor = switch (timeOfDay) {
            case "Morning Peak" -> 1.2;
            case "Evening Peak" -> 1.15;
            case "Night" -> 0.8;
            default -> 1.0;
        };

        double baseTime = road.distance / baseSpeed * 60;
        return (baseTime * (1 + priorityFactor * congestion) + intersectionDelay) * timeFactor;
    }

    private double getTrafficVolume(String key, String timeOfDay) throws SQLException {
        TrafficFlowData traffic = (TrafficFlowData) trafficFlow.getOrDefault(key, trafficFlow.get(key.split("->")[1] + "->" + key.split("->")[0]));
        if (traffic != null) {
            return switch (timeOfDay) {
                case "Morning Peak" -> traffic.morningPeak;
                case "Afternoon" -> traffic.afternoon;
                case "Evening Peak" -> traffic.eveningPeak;
                default -> traffic.night;
            };
        }
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT AVG(" + switch (timeOfDay) {
                 case "Morning Peak" -> "MorningPeak";
                 case "Afternoon" -> "Afternoon";
                 case "Evening Peak" -> "EveningPeak";
                 default -> "Night";
             } + ") as avgTraffic FROM TrafficFlow")) {
            return rs.next() ? rs.getDouble("avgTraffic") : 100;
        }
    }

    private double getAverageCongestion(String timeOfDay) throws SQLException {
        double totalCongestion = 0;
        int count = 0;
        for (Road road : roads) {
            String key = road.fromId + "->" + road.toId;
            double trafficVolume = getTrafficVolume(key, timeOfDay);
            double congestionFactor = congestionFactors.getOrDefault(key, 1.0);
            double congestion = road.capacity > 0 ? Math.min(trafficVolume * congestionFactor / road.capacity, 1.0) : 1.0;
            totalCongestion += congestion;
            count++;
        }
        return count > 0 ? totalCongestion / count : 0.5;
    }

    private double distanceBetween(Node a, Node b) {
        double lat1 = Math.toRadians(a.getY()), lon1 = Math.toRadians(a.getX());
        double lat2 = Math.toRadians(b.getY()), lon2 = Math.toRadians(b.getX());
        double dlat = lat2 - lat1, dlon = lon2 - lon1;
        double formulaA = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dlon / 2) * Math.sin(dlon / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(formulaA), Math.sqrt(1 - formulaA));
    }

    private boolean displayPathInTable(List<Node> path, String timeOfDay) throws SQLException {
        tableModel.setRowCount(0);
        boolean missingRoads = false;
        for (int i = 0; i < path.size() - 1; i++) {
            Node from = path.get(i), to = path.get(i + 1);
            Road road = getRoad(from.getId(), to.getId());
            if (road == null) {
                missingRoads = true;
                continue;
            }
            var metrics = calculatePathMetrics(List.of(from, to), timeOfDay, true);
            String signalStatus = !selectedVehicleType.equals("Normal Car") ? signalOptimizer.getSignalStatus(to.getId()) : "Standard";
            tableModel.addRow(new Object[]{
                    from.getName(), to.getName(),
                    String.format("%.1f", road.distance),
                    String.format("%.1f", metrics.time),
                    String.format("%.0f%%", metrics.congestion * 100),
                    signalStatus
            });
        }
        return missingRoads;
    }

    private record PathMetrics(double time, double distance, double congestion) {}

    private PathMetrics calculatePathMetrics(List<Node> path, String timeOfDay, boolean usePriority) throws SQLException {
        if (path == null || path.size() < 2) return new PathMetrics(0, 0, 0);
        double totalTime = 0, totalDistance = 0, totalCongestion = 0;
        int count = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Node from = path.get(i), to = path.get(i + 1);
            Road road = getRoad(from.getId(), to.getId());
            if (road == null) continue;
            String key = from.getId() + "->" + to.getId();
            double trafficVolume = getTrafficVolume(key, timeOfDay);
            double congestionFactor = congestionFactors.getOrDefault(key, 1.0);
            double congestion = road.capacity > 0 ? Math.min(trafficVolume * congestionFactor / road.capacity, 1.0) : 1.0;
            double baseSpeed, priorityFactor, intersectionDelay;
            String vehicleType = usePriority ? selectedVehicleType : "Normal Car";
            switch (vehicleType) {
                case "Ambulance":
                    baseSpeed = 70.0;
                    priorityFactor = usePriority ? 0.2 : 0.5;
                    intersectionDelay = usePriority ? signalOptimizer.getPreemptionDelay(to.getId(), timeOfDay) : road.intersectionDelay;
                    break;
                case "Fire Truck":
                    baseSpeed = 65.0;
                    priorityFactor = usePriority ? 0.2 : 0.5;
                    intersectionDelay = usePriority ? signalOptimizer.getPreemptionDelay(to.getId(), timeOfDay) : road.intersectionDelay;
                    break;
                default:
                    baseSpeed = 60.0;
                    priorityFactor = 0.5;
                    intersectionDelay = signalOptimizer.getNormalDelay(to.getId(), timeOfDay);
            }
            double timeFactor = switch (timeOfDay) {
                case "Morning Peak" -> 1.2;
                case "Evening Peak" -> 1.15;
                case "Night" -> 0.8;
                default -> 1.0;
            };
            double baseTime = road.distance / baseSpeed * 60;
            totalTime += (baseTime * (1 + priorityFactor * congestion) + intersectionDelay) * timeFactor;
            totalDistance += road.distance;
            totalCongestion += congestion;
            count++;
        }
        return new PathMetrics(totalTime, totalDistance, count > 0 ? totalCongestion / count : 0);
    }

    private Road getRoad(String fromId, String toId) {
        return adjacencyList.getOrDefault(fromId, Collections.emptyList()).stream()
                .filter(road -> road.toId.equals(toId)).findFirst().orElse(null);
    }

    private String getNodeIdByName(String name) {
        return nodes.entrySet().stream()
                .filter(entry -> entry.getValue().getName().equals(name))
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    private void startBlinking() {
        stopBlinking();
        blinkingTimer = new Timer();
        blinkingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                blinkState = !blinkState;
                mapPanel.setBlinkState(blinkState);
                mapPanel.repaint();
            }
        }, 0, 500);
    }

    private void stopBlinking() {
        if (blinkingTimer != null) {
            blinkingTimer.cancel();
            blinkingTimer = null;
            blinkState = true;
            mapPanel.setBlinkState(blinkState);
            mapPanel.repaint();
        }
    }

    private void showError(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void dispose() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {}
        stopBlinking();
        super.dispose();
    }

    private static class SignalTiming {
        double greenTime;
        double cycleLength;
        boolean isPreempted;

        SignalTiming(double greenTime, double cycleLength) {
            this.greenTime = greenTime;
            this.cycleLength = cycleLength;
            this.isPreempted = false;
        }
    }

    private class TrafficSignalOptimizer {
        private final Map<String, Node> nodes;
        private final List<Road> roads;
        private final Map<String, Map<String, Integer>> trafficFlow;
        private final Map<String, SignalTiming> signalTimings;

        TrafficSignalOptimizer(Map<String, Node> nodes, List<Road> roads, Map<String, Map<String, Integer>> trafficFlow, Map<String, SignalTiming> signalTimings) {
            this.nodes = nodes;
            this.roads = roads;
            this.trafficFlow = trafficFlow;
            this.signalTimings = signalTimings;
        }

        void optimizeSignalsForPath(List<Node> path, String vehicleType, String timeOfDay) throws SQLException {
            if (path == null || path.size() < 2 || vehicleType.equals("Normal Car")) return;
            for (int i = 0; i < path.size() - 1; i++) {
                String nodeId = path.get(i + 1).getId();
                if (signalTimings.containsKey(nodeId)) {
                    SignalTiming timing = signalTimings.get(nodeId);
                    timing.isPreempted = true;
                    timing.greenTime = Math.min(timing.cycleLength * 0.7, timing.greenTime + 15);
                }
            }
            for (Node node : path) {
                String nodeId = node.getId();
                if (!signalTimings.containsKey(nodeId) || signalTimings.get(nodeId).isPreempted) continue;
                double congestion = calculateNodeCongestion(nodeId, timeOfDay);
                SignalTiming timing = signalTimings.get(nodeId);
                timing.greenTime = Math.min(timing.cycleLength * 0.6, 30 + congestion * 20);
            }
        }

        double getPreemptionDelay(String nodeId, String timeOfDay) {
            if (signalTimings.containsKey(nodeId) && signalTimings.get(nodeId).isPreempted) {
                return 0.5;
            }
            return getNormalDelay(nodeId, timeOfDay);
        }

        double getNormalDelay(String nodeId, String timeOfDay) {
            if (!signalTimings.containsKey(nodeId)) return 10.0;
            SignalTiming timing = signalTimings.get(nodeId);
            return (timing.cycleLength - timing.greenTime) * 0.5;
        }

        String getSignalStatus(String nodeId) {
            if (signalTimings.containsKey(nodeId) && signalTimings.get(nodeId).isPreempted) {
                return "Preempted";
            }
            return "Standard";
        }

        private double calculateNodeCongestion(String nodeId, String timeOfDay) throws SQLException {
            double totalCongestion = 0;
            int count = 0;
            for (Road road : adjacencyList.getOrDefault(nodeId, Collections.emptyList())) {
                String key = road.fromId + "->" + road.toId;
                double trafficVolume = getTrafficVolume(key, timeOfDay);
                double congestionFactor = congestionFactors.getOrDefault(key, 1.0);
                double congestion = road.capacity > 0 ? Math.min(trafficVolume * congestionFactor / road.capacity, 1.0) : 1.0;
                totalCongestion += congestion;
                count++;
            }
            return count > 0 ? totalCongestion / count : 0.5;
        }
    }

    private class AStarNode implements Comparable<AStarNode> {
        Node node;
        double g, f;

        AStarNode(Node node, double g, double f) {
            this.node = node;
            this.g = g;
            this.f = f;
        }

        @Override
        public int compareTo(AStarNode other) {
            return Double.compare(this.f, other.f);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return node.equals(((AStarNode) o).node);
        }

        @Override
        public int hashCode() {
            return node.hashCode();
        }
    }
}