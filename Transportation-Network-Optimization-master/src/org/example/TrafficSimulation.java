package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.List;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.image.RescaleOp;

public class TrafficSimulation extends JFrame {
    private Connection conn;
    private Map<String, Node> nodes;
    private List<Road> roads;
    private Map<String, Map<String, Integer>> trafficFlow;
    private JComboBox<String> lineCombo, timeCombo, transitTypeCombo, startCombo, endCombo;
    private JTextArea resultTextArea;
    private JTable routeTable;
    private DefaultTableModel tableModel;
    private Map<String, List<String>> transitLines;
    private Map<String, String> lineTypes;
    private MapPanel mapPanel;
    private List<String> currentLineStops;
    private List<String> highlightedPath;
    private JCheckBox highlightRouteCheckBox;
    private List<Road> closedRoads;

    public TrafficSimulation(Connection conn) {
        this.conn = conn;
        closedRoads = new ArrayList<>();
        try {
            nodes = DataLoader.loadNodes(conn);
            roads = DataLoader.loadRoads(conn);
            trafficFlow = DataLoader.loadTrafficFlow(conn);
            transitLines = loadTransitLines(conn);
            initUI();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initUI() {
        setTitle("Traffic Flow Optimization");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true); // Remove window borders
        setMinimumSize(new Dimension(800, 600));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JLabel background = new JLabel();
        background.setBackground(new Color(245, 245, 220)); // Light beige background
        background.setOpaque(true);
        background.setLayout(new BorderLayout(10, 10));
        setContentPane(background);

        JLabel titleLabel = new JLabel("Traffic Flow Optimization", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(74, 74, 74)); // Dark gray for text
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        // Add a panel to shift the map to the right
        JPanel mapOuterContainer = new JPanel(new BorderLayout());
        mapOuterContainer.setBackground(new Color(245, 245, 220));
        JPanel mapContainer = new JPanel(new BorderLayout());
        mapContainer.setBackground(new Color(245, 245, 220));
        mapPanel = new MapPanel(nodes, roads, trafficFlow, closedRoads, highlightRouteCheckBox != null && highlightRouteCheckBox.isSelected());
        mapPanel.setPreferredSize(new Dimension(950, 700)); // Slightly larger than the original 900x650
        mapPanel.setBackground(new Color(245, 245, 220)); // Same light beige for map
        mapPanel.setBorder(BorderFactory.createLineBorder(new Color(193, 154, 107), 1)); // Darker beige border
        mapContainer.add(mapPanel, BorderLayout.CENTER); // Directly add mapPanel without JScrollPane

        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        legendPanel.setBackground(new Color(245, 245, 220));
        legendPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(193, 154, 107)), "Congestion Legend", 0, 0, null, new Color(74, 74, 74)));

        JPanel lowCongestion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lowCongestion.setBackground(new Color(245, 245, 220));
        JLabel lowDot = new JLabel("●");
        lowDot.setForeground(new Color(107, 142, 35)); // Muted green (same as roads)
        lowCongestion.add(lowDot);
        JLabel lowLabel = new JLabel("Low (< 50%)");
        lowLabel.setForeground(new Color(74, 74, 74));
        lowCongestion.add(lowLabel);
        legendPanel.add(lowCongestion);

        JPanel mediumCongestion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        mediumCongestion.setBackground(new Color(245, 245, 220));
        JLabel mediumDot = new JLabel("●");
        mediumDot.setForeground(new Color(255, 215, 0)); // Keeping softer yellow for medium congestion
        mediumCongestion.add(mediumDot);
        JLabel mediumLabel = new JLabel("Medium (50% - 80%)");
        mediumLabel.setForeground(new Color(74, 74, 74));
        mediumCongestion.add(mediumLabel);
        legendPanel.add(mediumCongestion);

        JPanel highCongestion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        highCongestion.setBackground(new Color(245, 245, 220));
        JLabel highDot = new JLabel("●");
        highDot.setForeground(new Color(139, 69, 19)); // Dark brown (same as high population nodes)
        highCongestion.add(highDot);
        JLabel highLabel = new JLabel("High (> 80%)");
        highLabel.setForeground(new Color(74, 74, 74));
        highCongestion.add(highLabel);
        legendPanel.add(highCongestion);

        mapContainer.add(legendPanel, BorderLayout.SOUTH);
        mapOuterContainer.add(mapContainer, BorderLayout.CENTER);
        mapOuterContainer.add(Box.createRigidArea(new Dimension(50, 0)), BorderLayout.WEST);
        add(mapOuterContainer, BorderLayout.WEST);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(new Color(210, 180, 140)); // Darker beige control panel
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.setBackground(new Color(210, 180, 140));
        controlPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(193, 154, 107)), "Controls", 0, 0, null, new Color(74, 74, 74)));
        controlPanel.setMaximumSize(new Dimension(400, 450));

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel transitTypeLabel = new JLabel("Transit Type:");
        transitTypeLabel.setForeground(new Color(74, 74, 74));
        controlPanel.add(transitTypeLabel, gbc);
        gbc.gridx = 1;
        transitTypeCombo = new JComboBox<>(new String[]{"All", "Metro", "Bus"});
        transitTypeCombo.addActionListener(e -> updateLineCombo());
        transitTypeCombo.setPreferredSize(new Dimension(200, 30));
        transitTypeCombo.setBackground(new Color(255, 255, 255));
        transitTypeCombo.setForeground(new Color(74, 74, 74));
        controlPanel.add(transitTypeCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel selectLineLabel = new JLabel("Select Transit Line:");
        selectLineLabel.setForeground(new Color(74, 74, 74));
        controlPanel.add(selectLineLabel, gbc);
        gbc.gridx = 1;
        lineCombo = new JComboBox<>(transitLines.keySet().toArray(new String[0]));
        lineCombo.setPreferredSize(new Dimension(200, 30));
        lineCombo.setBackground(new Color(255, 255, 255));
        lineCombo.setForeground(new Color(74, 74, 74));
        controlPanel.add(lineCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel startPointLabel = new JLabel("Start Point (for new path):");
        startPointLabel.setForeground(new Color(74, 74, 74));
        controlPanel.add(startPointLabel, gbc);
        gbc.gridx = 1;
        startCombo = new JComboBox<>(nodes.values().stream().map(Node::getName).toArray(String[]::new));
        startCombo.setPreferredSize(new Dimension(200, 30));
        startCombo.setBackground(new Color(255, 255, 255));
        startCombo.setForeground(new Color(74, 74, 74));
        controlPanel.add(startCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel endPointLabel = new JLabel("End Point (for new path):");
        endPointLabel.setForeground(new Color(74, 74, 74));
        controlPanel.add(endPointLabel, gbc);
        gbc.gridx = 1;
        endCombo = new JComboBox<>(nodes.values().stream().map(Node::getName).toArray(String[]::new));
        endCombo.setPreferredSize(new Dimension(200, 30));
        endCombo.setBackground(new Color(255, 255, 255));
        endCombo.setForeground(new Color(74, 74, 74));
        controlPanel.add(endCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        JLabel timeLabel = new JLabel("Time of Day:");
        timeLabel.setForeground(new Color(74, 74, 74));
        controlPanel.add(timeLabel, gbc);
        gbc.gridx = 1;
        String[] times = {"Morning Peak", "Afternoon", "Evening Peak", "Night"};
        timeCombo = new JComboBox<>(times);
        timeCombo.setPreferredSize(new Dimension(200, 30));
        timeCombo.setBackground(new Color(255, 255, 255));
        timeCombo.setForeground(new Color(74, 74, 74));
        controlPanel.add(timeCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        highlightRouteCheckBox = new JCheckBox("Highlight Selected Route Only");
        highlightRouteCheckBox.setForeground(new Color(74, 74, 74));
        highlightRouteCheckBox.setBackground(new Color(210, 180, 140));
        highlightRouteCheckBox.addActionListener(e -> {
            mapPanel.setHighlightRouteOnly(highlightRouteCheckBox.isSelected());
            mapPanel.repaint();
        });
        controlPanel.add(highlightRouteCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton analyzeButton = new JButton("Analyze Transit Line");
        analyzeButton.setBackground(new Color(47, 79, 79)); // Dark green
        analyzeButton.setForeground(new Color(255, 255, 255));
        analyzeButton.setPreferredSize(new Dimension(200, 35));
        analyzeButton.addActionListener(e -> analyzeTransitLine());
        controlPanel.add(analyzeButton, gbc);

        gbc.gridy = 7;
        JButton optimizeButton = new JButton("Optimize Transit Line");
        optimizeButton.setBackground(new Color(47, 79, 79)); // Dark green
        optimizeButton.setForeground(new Color(255, 255, 255));
        optimizeButton.setPreferredSize(new Dimension(200, 35));
        optimizeButton.addActionListener(e -> optimizeTransitLine());
        controlPanel.add(optimizeButton, gbc);

        gbc.gridy = 8;
        JButton findPathButton = new JButton("Find Optimal Path");
        findPathButton.setBackground(new Color(47, 79, 79)); // Dark green
        findPathButton.setForeground(new Color(255, 255, 255));
        findPathButton.setPreferredSize(new Dimension(200, 35));
        findPathButton.addActionListener(e -> findOptimalPath());
        controlPanel.add(findPathButton, gbc);

        gbc.gridy = 9;
        JButton closeRoadButton = new JButton("Close a Road (Test)");
        closeRoadButton.setBackground(new Color(139, 69, 19)); // Dark brown
        closeRoadButton.setForeground(new Color(255, 255, 255));
        closeRoadButton.setPreferredSize(new Dimension(200, 35));
        closeRoadButton.addActionListener(e -> closeRoadForTest());
        controlPanel.add(closeRoadButton, gbc);

        rightPanel.add(controlPanel);

        resultTextArea = new JTextArea("Select a transit line or points to analyze.");
        resultTextArea.setFont(new Font("Arial", Font.BOLD, 14));
        resultTextArea.setForeground(new Color(74, 74, 74));
        resultTextArea.setBackground(new Color(255, 255, 255));
        resultTextArea.setLineWrap(true);
        resultTextArea.setWrapStyleWord(true);
        resultTextArea.setEditable(false);
        resultTextArea.setBorder(BorderFactory.createLineBorder(new Color(193, 154, 107), 1, true));

        JScrollPane resultScrollPane = new JScrollPane(resultTextArea);
        resultScrollPane.setPreferredSize(new Dimension(400, 60));
        resultScrollPane.setMinimumSize(new Dimension(400, 60));
        resultScrollPane.setBackground(new Color(210, 180, 140));
        resultScrollPane.setBorder(BorderFactory.createLineBorder(new Color(193, 154, 107)));
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(resultScrollPane);

        String[] columns = {"From", "To", "Distance (km)", "Time (min)", "Congestion"};
        tableModel = new DefaultTableModel(columns, 0);
        routeTable = new JTable(tableModel);
        routeTable.setRowHeight(25);
        routeTable.setFont(new Font("Arial", Font.PLAIN, 14));
        routeTable.setForeground(new Color(74, 74, 74));
        routeTable.setBackground(new Color(255, 255, 255));
        routeTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        routeTable.getTableHeader().setBackground(new Color(210, 180, 140));
        routeTable.getTableHeader().setForeground(new Color(74, 74, 74));
        routeTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(row % 2 == 0 ? new Color(255, 255, 255) : new Color(245, 245, 220));
                c.setForeground(new Color(74, 74, 74));
                return c;
            }
        });
        JScrollPane tableScrollPane = new JScrollPane(routeTable);
        tableScrollPane.setPreferredSize(new Dimension(400, 400));
        tableScrollPane.setBackground(new Color(210, 180, 140));
        tableScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(193, 154, 107)), "Route Details", 0, 0, null, new Color(74, 74, 74)));
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(tableScrollPane);

        add(rightPanel, BorderLayout.EAST);

        // Add back button in top-right corner
        JButton backButton = new JButton("");
        backButton.setPreferredSize(new Dimension(50, 50)); // 50x50 pixels
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        BufferedImage backImage = null;
        try {
            // Load and scale img.png to button size
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
                JOptionPane.showMessageDialog(TrafficSimulation.this,
                        "Cannot find MainMenu class. Ensure MainMenu.java is in the org.example package and compiled.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(TrafficSimulation.this,
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

    private void updateLineCombo() {
        String selectedType = (String) transitTypeCombo.getSelectedItem();
        lineCombo.removeAllItems();
        for (String lineName : transitLines.keySet()) {
            String lineType = lineTypes.get(lineName);
            if (selectedType.equals("All") ||
                    (selectedType.equals("Metro") && "Metro".equals(lineType)) ||
                    (selectedType.equals("Bus") && "Bus".equals(lineType))) {
                lineCombo.addItem(lineName);
            }
        }
    }

    private void analyzeTransitLine() {
        String selectedLine = (String) lineCombo.getSelectedItem();
        String timeOfDay = (String) timeCombo.getSelectedItem();
        List<String> lineStops = transitLines.get(selectedLine);

        if (lineStops == null || lineStops.size() < 2) {
            resultTextArea.setText("Invalid transit line selected.");
            return;
        }

        currentLineStops = lineStops;
        highlightedPath = null; // Clear optimal path highlighting
        double totalDistance = 0;
        double totalTime = 0;
        double maxCongestion = 0;

        tableModel.setRowCount(0);
        for (int i = 0; i < lineStops.size() - 1; i++) {
            String from = lineStops.get(i);
            String to = lineStops.get(i + 1);
            Road road = getRoad(from, to);
            if (road == null) continue;

            String key = from + "-" + to;
            if (!trafficFlow.containsKey(key)) key = to + "-" + from;
            Map<String, Integer> traffic = trafficFlow.get(key);
            if (traffic == null) continue;

            double congestion = calculateCongestion(road.getCapacity(), timeOfDay, traffic);
            double travelTime = calculateTravelTime(road.getDistance(), road.getCapacity(), timeOfDay, traffic);

            totalDistance += road.getDistance();
            totalTime += travelTime;
            maxCongestion = Math.max(maxCongestion, congestion);

            tableModel.addRow(new Object[]{
                    getNodeNameById(from),
                    getNodeNameById(to),
                    String.format("%.1f", road.getDistance()),
                    String.format("%.1f", travelTime),
                    String.format("%.0f%%", congestion * 100)
            });
        }

        resultTextArea.setText(String.format("Line %s: %.1f km, %.1f min, %.0f%% Congested",
                selectedLine, totalDistance, totalTime, maxCongestion * 100));
        mapPanel.setCurrentLineStops(currentLineStops);
        mapPanel.setHighlightedPath(highlightedPath);
        mapPanel.setTimeOfDay(timeOfDay);
        mapPanel.repaint();
    }

    private void optimizeTransitLine() {
        String selectedLine = (String) lineCombo.getSelectedItem();
        String timeOfDay = (String) timeCombo.getSelectedItem();
        List<String> lineStops = transitLines.get(selectedLine);

        if (lineStops == null || lineStops.size() < 2) {
            resultTextArea.setText("Invalid transit line selected.");
            return;
        }

        double totalCongestionBefore = 0;
        double totalCongestionAfter = 0;
        Map<String, Double> newTraffic = new HashMap<>();
        List<List<String>> alternativePaths = new ArrayList<>(); // Store alternative paths
        StringBuilder message = new StringBuilder("Optimized " + selectedLine + ":\n");
        Map<String, List<Dijkstra.Edge>> graph = buildGraph();
        int reroutedSegments = 0;

        for (int i = 0; i < lineStops.size() - 1; i++) {
            String from = lineStops.get(i);
            String to = lineStops.get(i + 1);
            Road road = getRoad(from, to);
            if (road == null) continue;

            String key = from + "-" + to;
            if (!trafficFlow.containsKey(key)) key = to + "-" + from;
            Map<String, Integer> traffic = trafficFlow.get(key);
            if (traffic == null) continue;

            double congestion = calculateCongestion(road.getCapacity(), timeOfDay, traffic);
            totalCongestionBefore += congestion;

            if (congestion > 0.8) { // Check for high congestion
                // Temporarily close the congested road
                List<Road> tempClosedRoads = new ArrayList<>(closedRoads);
                tempClosedRoads.add(road);

                // Find alternative path
                Dijkstra.PathResult altPath = Dijkstra.findShortestPath(graph, from, to, timeOfDay, trafficFlow, tempClosedRoads);
                if (!altPath.path.isEmpty() && altPath.time != Double.POSITIVE_INFINITY) {
                    // Reroute 30% of traffic
                    double currentTraffic = getTrafficForTime(traffic, timeOfDay);
                    double reroutedTraffic = currentTraffic * 0.3;
                    double remainingTraffic = currentTraffic * 0.7;
                    newTraffic.put(key, remainingTraffic);

                    // Distribute rerouted traffic to alternative path
                    for (int j = 0; j < altPath.path.size() - 1; j++) {
                        String altFrom = altPath.path.get(j);
                        String altTo = altPath.path.get(j + 1);
                        String altKey = altFrom + "-" + altTo;
                        if (!trafficFlow.containsKey(altKey)) altKey = altTo + "-" + altFrom;
                        Map<String, Integer> altTraffic = trafficFlow.get(altKey);
                        if (altTraffic != null) {
                            double altCurrentTraffic = getTrafficForTime(altTraffic, timeOfDay);
                            newTraffic.put(altKey, altCurrentTraffic + reroutedTraffic);
                        }
                    }

                    // Store alternative path
                    alternativePaths.add(altPath.path);

                    // Add to message
                    String pathStr = String.join(" -> ", altPath.path.stream().map(this::getNodeNameById).toList());
                    message.append(String.format("- Rerouted 30%% of traffic from %s -> %s (%.0f%% congestion) to alternative path: %s\n",
                            getNodeNameById(from), getNodeNameById(to), congestion * 100, pathStr));
                    reroutedSegments++;
                } else {
                    newTraffic.put(key, getTrafficForTime(traffic, timeOfDay));
                }
            } else {
                newTraffic.put(key, getTrafficForTime(traffic, timeOfDay));
            }
        }

        // Calculate congestion after rerouting
        for (int i = 0; i < lineStops.size() - 1; i++) {
            String from = lineStops.get(i);
            String to = lineStops.get(i + 1);
            Road road = getRoad(from, to);
            if (road == null) continue;

            String key = from + "-" + to;
            if (!trafficFlow.containsKey(key)) key = to + "-" + from;
            double trafficVolume = newTraffic.getOrDefault(key, getTrafficForTime(trafficFlow.get(key), timeOfDay));
            double congestion = trafficVolume / road.getCapacity();
            if (congestion > 1) congestion = 1;
            totalCongestionAfter += congestion;
        }

        // Calculate congestion reduction
        double avgCongestionBefore = totalCongestionBefore / (lineStops.size() - 1);
        double avgCongestionAfter = totalCongestionAfter / (lineStops.size() - 1);
        double reduction = avgCongestionBefore > 0 ? (avgCongestionBefore - avgCongestionAfter) / avgCongestionBefore * 100 : 0;

        // Finalize message
        if (reroutedSegments == 0) {
            message.append("No roads required rerouting (all congestion levels below 80%).");
        } else {
            message.append(String.format("- Total congestion reduced by %.0f%%.", reduction));
        }

        // Update UI
        resultTextArea.setText(message.toString());
        updateTableWithNewCongestion(newTraffic, timeOfDay);

        // Combine alternative paths for display
        highlightedPath = new ArrayList<>();
        Set<String> seenEdges = new HashSet<>();
        for (List<String> path : alternativePaths) {
            for (int i = 0; i < path.size() - 1; i++) {
                String edge = path.get(i) + "-" + path.get(i + 1);
                if (!seenEdges.contains(edge)) {
                    seenEdges.add(edge);
                    if (!highlightedPath.contains(path.get(i))) {
                        highlightedPath.add(path.get(i));
                    }
                    if (!highlightedPath.contains(path.get(i + 1))) {
                        highlightedPath.add(path.get(i + 1));
                    }
                }
            }
        }

        currentLineStops = lineStops; // Keep original line highlighted
        mapPanel.setCurrentLineStops(currentLineStops);
        mapPanel.setHighlightedPath(highlightedPath);
        mapPanel.setTimeOfDay(timeOfDay);
        mapPanel.repaint();
    }

    private void findOptimalPath() {
        String startName = (String) startCombo.getSelectedItem();
        String endName = (String) endCombo.getSelectedItem();
        String timeOfDay = (String) timeCombo.getSelectedItem();

        String start = getNodeIdByName(startName);
        String end = getNodeIdByName(endName);

        if (start == null || end == null) {
            resultTextArea.setText("Invalid start or end point selected.");
            return;
        }

        // Find transit lines that pass through both Start and End
        List<String> matchingLines = findTransitLinesBetweenPoints(start, end);
        StringBuilder comparisonResult = new StringBuilder();

        if (!matchingLines.isEmpty()) {
            comparisonResult.append("Transit lines passing through both points ")
                    .append(startName).append(" and ").append(endName).append(": ")
                    .append(String.join(", ", matchingLines));
        } else {
            comparisonResult.append("No transit lines pass through both points ")
                    .append(startName).append(" and ").append(endName).append(".");
        }

        // Calculate the optimal path using Dijkstra
        Map<String, List<Dijkstra.Edge>> graph = buildGraph();
        Dijkstra.PathResult optimalPath = Dijkstra.findShortestPath(graph, start, end, timeOfDay, trafficFlow, closedRoads);

        if (optimalPath.path.isEmpty() || optimalPath.time == Double.POSITIVE_INFINITY) {
            resultTextArea.setText("No path found between " + startName + " and " + endName + ".");
            return;
        }

        // Display the optimal path
        comparisonResult.append("\nOptimal Path: ").append(String.format("%.1f km, %.1f min", optimalPath.distance, optimalPath.time));
        resultTextArea.setText(comparisonResult.toString());
        highlightedPath = optimalPath.path;
        currentLineStops = null; // Clear transit line highlighting
        displayPathInTable(optimalPath.path, timeOfDay);
        mapPanel.setCurrentLineStops(currentLineStops);
        mapPanel.setHighlightedPath(highlightedPath);
        mapPanel.setTimeOfDay(timeOfDay);
        mapPanel.repaint();
    }

    private List<String> findTransitLinesBetweenPoints(String start, String end) {
        List<String> matchingLines = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : transitLines.entrySet()) {
            String lineName = entry.getKey();
            List<String> stops = entry.getValue();
            if (stops.contains(start) && stops.contains(end)) {
                int startIndex = stops.indexOf(start);
                int endIndex = stops.indexOf(end);
                if (startIndex < endIndex) { // Ensure the path direction is correct
                    matchingLines.add(lineName);
                }
            }
        }
        return matchingLines;
    }

    private void closeRoadForTest() {
        Road roadToClose = roads.stream()
                .filter(r -> (r.fromId.equals("1") && r.toId.equals("3")) || (r.fromId.equals("3") && r.toId.equals("1")))
                .findFirst()
                .orElse(null);

        if (roadToClose != null && !closedRoads.contains(roadToClose)) {
            closedRoads.add(roadToClose);
            resultTextArea.setText("Road between Maadi and Downtown Cairo closed.");
            mapPanel.repaint();
        } else {
            resultTextArea.setText("Road already closed or not found.");
        }
    }

    private Map<String, List<Dijkstra.Edge>> buildGraph() {
        Map<String, List<Dijkstra.Edge>> graph = new HashMap<>();
        for (Road road : roads) {
            if (!road.isExisting()) continue; // Skip non-existing roads
            Dijkstra.Edge edge = new Dijkstra.Edge(road.getToId(), road.getDistance(), road.getCapacity());
            graph.computeIfAbsent(road.getFromId(), k -> new ArrayList<>()).add(edge);
            // Since roads are bidirectional, add the reverse edge
            Dijkstra.Edge reverseEdge = new Dijkstra.Edge(road.getFromId(), road.getDistance(), road.getCapacity());
            graph.computeIfAbsent(road.getToId(), k -> new ArrayList<>()).add(reverseEdge);
        }
        return graph;
    }

    private void displayPathInTable(List<String> path, String timeOfDay) {
        tableModel.setRowCount(0);
        double totalDistance = 0;
        double totalTime = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to = path.get(i + 1);
            Road road = getRoad(from, to);
            if (road == null) continue;

            String key = from + "-" + to;
            if (!trafficFlow.containsKey(key)) key = to + "-" + from;
            Map<String, Integer> traffic = trafficFlow.get(key);
            double congestion = traffic != null ? calculateCongestion(road.getCapacity(), timeOfDay, traffic) : 0;
            double travelTime = calculateTravelTime(road.getDistance(), road.getCapacity(), timeOfDay, traffic);

            totalDistance += road.getDistance();
            totalTime += travelTime;

            tableModel.addRow(new Object[]{
                    getNodeNameById(from),
                    getNodeNameById(to),
                    String.format("%.1f", road.getDistance()),
                    String.format("%.1f", travelTime),
                    String.format("%.0f%%", congestion * 100)
            });
        }
    }

    private double calculateCongestion(int capacity, String timeOfDay, Map<String, Integer> traffic) {
        double trafficVolume = getTrafficForTime(traffic, timeOfDay);
        double congestion = capacity > 0 ? trafficVolume / capacity : 1.0;
        return Math.min(congestion, 1.0);
    }

    private double getTrafficForTime(Map<String, Integer> traffic, String timeOfDay) {
        if (traffic == null) return 0;
        switch (timeOfDay) {
            case "Morning Peak":
                return traffic.getOrDefault("MorningPeak", 0);
            case "Afternoon":
                return traffic.getOrDefault("Afternoon", 0);
            case "Evening Peak":
                return traffic.getOrDefault("EveningPeak", 0);
            case "Night":
                return traffic.getOrDefault("Night", 0);
            default:
                return 0;
        }
    }

    private double calculateTravelTime(double distance, int capacity, String timeOfDay, Map<String, Integer> traffic) {
        double baseTime = distance / 60.0 * 60;
        double congestionFactor = traffic != null ? calculateCongestion(capacity, timeOfDay, traffic) : 0;
        return baseTime * (1 + congestionFactor);
    }

    private void updateTableWithNewCongestion(Map<String, Double> newTraffic, String timeOfDay) {
        String selectedLine = (String) lineCombo.getSelectedItem();
        List<String> lineStops = transitLines.get(selectedLine);

        tableModel.setRowCount(0);
        for (int i = 0; i < lineStops.size() - 1; i++) {
            String from = lineStops.get(i);
            String to = lineStops.get(i + 1);
            Road road = getRoad(from, to);
            if (road == null) continue;

            String key = from + "-" + to;
            if (!trafficFlow.containsKey(key)) key = to + "-" + from;
            double trafficVolume = newTraffic.getOrDefault(key, getTrafficForTime(trafficFlow.get(key), timeOfDay));
            double congestion = trafficVolume / road.getCapacity();
            if (congestion > 1) congestion = 1;
            double travelTime = road.getDistance() / 60.0 * 60 * (1 + congestion);

            tableModel.addRow(new Object[]{
                    getNodeNameById(from),
                    getNodeNameById(to),
                    String.format("%.1f", road.getDistance()),
                    String.format("%.1f", travelTime),
                    String.format("%.0f%%", congestion * 100)
            });
        }
    }

    private Road getRoad(String fromId, String toId) {
        return roads.stream()
                .filter(r -> (r.fromId.equals(fromId) && r.toId.equals(toId)) || (r.fromId.equals(toId) && r.toId.equals(fromId)))
                .findFirst()
                .orElse(null);
    }

    private String getNodeIdByName(String name) {
        return nodes.entrySet().stream()
                .filter(entry -> entry.getValue().getName().equals(name))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private String getNodeNameById(String id) {
        Node node = nodes.get(id);
        return node != null ? node.getName() : id;
    }

    private Map<String, List<String>> loadTransitLines(Connection conn) throws SQLException {
        Map<String, List<String>> lines = new HashMap<>();
        Map<String, String> lineTypes = new HashMap<>();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM TransitLines");
        while (rs.next()) {
            String lineName = rs.getString("LineName");
            String stopsStr = rs.getString("Stops");
            String lineType = rs.getString("LineType");
            List<String> stops = Arrays.asList(stopsStr.split(","));
            lines.put(lineName, stops);
            lineTypes.put(lineName, lineType);
        }
        rs.close();
        stmt.close();
        this.lineTypes = lineTypes;
        System.out.println("Total transit lines loaded: " + lines.size());
        return lines;
    }
}