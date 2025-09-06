package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.List;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.event.*;
import java.io.File; // Added for File
import java.io.IOException; // Added for IOException
import java.awt.image.RescaleOp; // Added for RescaleOp

public class PublicTransitGUI extends JFrame {
    private Connection conn;
    private Map<String, Node> nodes;
    private List<Road> roads;
    private Map<String, Map<String, Integer>> trafficFlow;
    private Map<String, List<String>> transitLines;
    private Map<String, String> lineTypes;
    private Map<String, Integer> dailyPassengers;
    private Map<String, String> lineDisplayNames;
    private PublicTransitOptimizer optimizer;
    private TransitNetworkDesigner networkDesigner;
    private MapPanel mapPanel;
    private JComboBox<String> lineCombo, timeCombo, transitTypeCombo;
    private JTable scheduleTable;
    private DefaultTableModel tableModel;
    private JTextArea resultTextArea;
    private List<String> currentLineStops;
    private List<String> highlightedPath;
    private List<Road> closedRoads;
    private List<Object[]> previousScheduleData;
    private Map<String, Integer> vehicleCounts;

    public PublicTransitGUI(Connection conn) {
        this.conn = conn;
        closedRoads = new ArrayList<>();
        previousScheduleData = null;
        lineDisplayNames = new HashMap<>();
        vehicleCounts = new HashMap<>();
        try {
            nodes = DataLoader.loadNodes(conn);
            roads = DataLoader.loadRoads(conn);
            trafficFlow = DataLoader.loadTrafficFlow(conn);
            loadTransitLines(conn);
            loadVehicleCounts(conn);
            optimizer = new PublicTransitOptimizer(nodes, roads, trafficFlow, transitLines);
            networkDesigner = new TransitNetworkDesigner(nodes, roads, trafficFlow, transitLines, dailyPassengers);
            initUI();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadVehicleCounts(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rsBus = stmt.executeQuery("SELECT RouteID, BusesAssigned FROM BusRoutes");
        while (rsBus.next()) {
            String lineId = rsBus.getString("RouteID");
            int vehicles = rsBus.getInt("BusesAssigned");
            vehicleCounts.put(lineId, vehicles);
            System.out.println("Loaded vehicles for bus route " + lineId + ": " + vehicles);
        }
        rsBus.close();

        ResultSet rsMetro = stmt.executeQuery("SELECT LineID FROM MetroLines");
        while (rsMetro.next()) {
            String lineId = rsMetro.getString("LineID");
            if (!vehicleCounts.containsKey(lineId)) {
                vehicleCounts.put(lineId, 10);
                System.out.println("Assigned default 10 vehicles for metro line " + lineId);
            }
        }
        rsMetro.close();
        stmt.close();
    }

    private void initUI() {
        setTitle("Public Transit Optimization");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);
        setMinimumSize(new Dimension(800, 600));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JLabel background = new JLabel();
        background.setBackground(new Color(245, 245, 220));
        background.setOpaque(true);
        background.setLayout(new BorderLayout(10, 10));
        setContentPane(background);

        JLabel titleLabel = new JLabel("Public Transit Optimization", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(74, 74, 74));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        JPanel mapOuterContainer = new JPanel(new BorderLayout());
        mapOuterContainer.setBackground(new Color(245, 245, 220));
        JPanel mapContainer = new JPanel(new BorderLayout());
        mapContainer.setBackground(new Color(245, 245, 220));
        mapPanel = new MapPanel(nodes, roads, trafficFlow, closedRoads, false);
        mapPanel.setPreferredSize(new Dimension(950, 700));
        mapPanel.setBackground(new Color(245, 245, 220));
        mapPanel.setBorder(BorderFactory.createLineBorder(new Color(193, 154, 107), 1));
        mapContainer.add(mapPanel, BorderLayout.CENTER);
        mapOuterContainer.add(mapContainer, BorderLayout.CENTER);
        mapOuterContainer.add(Box.createRigidArea(new Dimension(50, 0)), BorderLayout.WEST);
        add(mapOuterContainer, BorderLayout.WEST);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(new Color(210, 180, 140));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.setBackground(new Color(210, 180, 140));
        controlPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(193, 154, 107), 2), "Control Panel",
                0, 0, new Font("Arial", Font.BOLD, 16), new Color(74, 74, 74)));
        controlPanel.setMaximumSize(new Dimension(450, 350));

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel transitTypeLabel = new JLabel("Transit Type:");
        transitTypeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        transitTypeLabel.setForeground(new Color(74, 74, 74));
        controlPanel.add(transitTypeLabel, gbc);
        gbc.gridx = 1;
        transitTypeCombo = new JComboBox<>(new String[]{"All", "Metro", "Bus"});
        transitTypeCombo.addActionListener(e -> updateLineCombo());
        transitTypeCombo.setPreferredSize(new Dimension(220, 30));
        transitTypeCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        transitTypeCombo.setBackground(new Color(255, 255, 255));
        transitTypeCombo.setForeground(new Color(74, 74, 74));
        controlPanel.add(transitTypeCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel selectLineLabel = new JLabel("Select Transit Line:");
        selectLineLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        selectLineLabel.setForeground(new Color(74, 74, 74));
        controlPanel.add(selectLineLabel, gbc);
        gbc.gridx = 1;
        lineCombo = new JComboBox<>(lineDisplayNames.values().toArray(new String[0]));
        lineCombo.setPreferredSize(new Dimension(220, 30));
        lineCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        lineCombo.setBackground(new Color(255, 255, 255));
        lineCombo.setForeground(new Color(74, 74, 74));
        controlPanel.add(lineCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel timeLabel = new JLabel("Time of Day:");
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        timeLabel.setForeground(new Color(74, 74, 74));
        controlPanel.add(timeLabel, gbc);
        gbc.gridx = 1;
        String[] times = {"Morning Peak", "Afternoon", "Evening Peak", "Night"};
        timeCombo = new JComboBox<>(times);
        timeCombo.setPreferredSize(new Dimension(220, 30));
        timeCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        timeCombo.setBackground(new Color(255, 255, 255));
        timeCombo.setForeground(new Color(74, 74, 74));
        controlPanel.add(timeCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton optimizeScheduleButton = new JButton("Optimize Schedule");
        optimizeScheduleButton.setBackground(new Color(47, 79, 79));
        optimizeScheduleButton.setForeground(new Color(255, 255, 255));
        optimizeScheduleButton.setFont(new Font("Arial", Font.BOLD, 14));
        optimizeScheduleButton.setPreferredSize(new Dimension(220, 35));
        optimizeScheduleButton.setFocusPainted(false);
        optimizeScheduleButton.addActionListener(e -> optimizeSchedule());
        controlPanel.add(optimizeScheduleButton, gbc);

        gbc.gridy = 4;
        JButton optimizeNetworkButton = new JButton("Optimize Network");
        optimizeNetworkButton.setBackground(new Color(47, 79, 79));
        optimizeNetworkButton.setForeground(new Color(255, 255, 255));
        optimizeScheduleButton.setFont(new Font("Arial", Font.BOLD, 14));
        optimizeNetworkButton.setPreferredSize(new Dimension(220, 35));
        optimizeNetworkButton.setFocusPainted(false);
        optimizeNetworkButton.addActionListener(e -> optimizeNetwork());
        controlPanel.add(optimizeNetworkButton, gbc);

        gbc.gridy = 5;
        JButton closeButton = new JButton("Close");
        closeButton.setBackground(new Color(139, 69, 19));
        closeButton.setForeground(new Color(255, 255, 255));
        closeButton.setFont(new Font("Arial", Font.BOLD, 14));
        closeButton.setPreferredSize(new Dimension(220, 35));
        closeButton.setFocusPainted(false);
        closeButton.addActionListener(e -> dispose());
        controlPanel.add(closeButton, gbc);

        rightPanel.add(controlPanel);

        resultTextArea = new JTextArea("Select a transit line or optimize the network.");
        resultTextArea.setFont(new Font("Arial", Font.BOLD, 14));
        resultTextArea.setForeground(new Color(74, 74, 74));
        resultTextArea.setBackground(new Color(255, 255, 255));
        resultTextArea.setLineWrap(true);
        resultTextArea.setWrapStyleWord(true);
        resultTextArea.setEditable(false);
        resultTextArea.setBorder(BorderFactory.createLineBorder(new Color(193, 154, 107), 2, true));

        JScrollPane resultScrollPane = new JScrollPane(resultTextArea);
        resultScrollPane.setPreferredSize(new Dimension(450, 120));
        resultScrollPane.setMinimumSize(new Dimension(450, 120));
        resultScrollPane.setBackground(new Color(210, 180, 140));
        resultScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(193, 154, 107), 2), "Optimization Results",
                0, 0, new Font("Arial", Font.BOLD, 16), new Color(74, 74, 74)));
        rightPanel.add(Box.createVerticalStrut(15));
        rightPanel.add(resultScrollPane);

        String[] columns = {"Station", "Time (min)", "Vehicles", "Frequency (min)"};
        tableModel = new DefaultTableModel(columns, 0);
        scheduleTable = new JTable(tableModel);
        scheduleTable.setRowHeight(25);
        scheduleTable.setFont(new Font("Arial", Font.PLAIN, 14));
        scheduleTable.setForeground(new Color(74, 74, 74));
        scheduleTable.setBackground(new Color(255, 255, 255));
        scheduleTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        scheduleTable.getTableHeader().setBackground(new Color(210, 180, 140));
        scheduleTable.getTableHeader().setForeground(new Color(74, 74, 74));
        scheduleTable.setGridColor(new Color(193, 154, 107));
        scheduleTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(row % 2 == 0 ? new Color(255, 255, 255) : new Color(245, 245, 220));
                c.setForeground(new Color(74, 74, 74));
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        scheduleTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        scheduleTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        scheduleTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        scheduleTable.getColumnModel().getColumn(3).setPreferredWidth(100);

        JScrollPane tableScrollPane = new JScrollPane(scheduleTable);
        tableScrollPane.setPreferredSize(new Dimension(450, 350));
        tableScrollPane.setBackground(new Color(210, 180, 140));
        tableScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(193, 154, 107), 2), "Schedule Details",
                0, 0, new Font("Arial", Font.BOLD, 16), new Color(74, 74, 74)));
        rightPanel.add(Box.createVerticalStrut(15));
        rightPanel.add(tableScrollPane);

        add(rightPanel, BorderLayout.EAST);

        // Add back button in top-right corner
        JButton backButton = new JButton("");
        backButton.setPreferredSize(new Dimension(50, 50));
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        BufferedImage backImage = null;
        try {
            backImage = ImageIO.read(new File("img_1.png"));
            Image scaledImg = backImage.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            backButton.setIcon(new ImageIcon(scaledImg));
            backButton.setBorder(BorderFactory.createEmptyBorder());
            backButton.setContentAreaFilled(false);
        } catch (IOException e) {
            e.printStackTrace();
            backButton.setBackground(new Color(0, 120, 215));
            backButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        }

        final BufferedImage finalBackImage = backImage;
        backButton.addMouseListener(new MouseAdapter() {
            private Icon originalIcon = backButton.getIcon();
            @Override
            public void mouseEntered(MouseEvent e) {
                if (finalBackImage != null) {
                    try {
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

        backButton.addActionListener(e -> {
            try {
                Class<?> mainMenuClass = Class.forName("org.example.MainMenu");
                JFrame mainMenuFrame = (JFrame) mainMenuClass.getDeclaredConstructor().newInstance();
                mainMenuFrame.setVisible(true);
                dispose();
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(PublicTransitGUI.this,
                        "Cannot find MainMenu class. Ensure MainMenu.java is in the org.example package and compiled.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(PublicTransitGUI.this,
                        "Error opening MainMenu: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        background.setLayout(null);
        backButton.setBounds(background.getWidth() - 50, 0, 50, 50);
        background.add(backButton);

        background.setLayout(new BorderLayout(10, 10));
        background.add(titleLabel, BorderLayout.NORTH);
        background.add(mapOuterContainer, BorderLayout.WEST);
        background.add(rightPanel, BorderLayout.EAST);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                backButton.setBounds(background.getWidth() - 50, 0, 50, 50);
            }
        });

        setVisible(true);
    }

    private void initializeLineDisplayNames() {
        // No manual data; all names are loaded from the database in loadTransitLines
    }

    private String inferLocationFromLineId(String lineId) {
        String[] locations = {"Cairo", "Giza", "Alexandria", "Luxor", "Aswan", "Port Said", "Suez", "Hurghada", "Sharm El Sheikh", "Mansoura"};
        int index = Math.abs(lineId.hashCode() % locations.length);
        return locations[index];
    }

    private void optimizeSchedule() {
        String selectedDisplayName = (String) lineCombo.getSelectedItem();
        String lineId = getLineIdFromDisplayName(selectedDisplayName);
        String timeOfDay = (String) timeCombo.getSelectedItem();
        List<String> stops = transitLines.get(lineId);
        String lineType = lineTypes.getOrDefault(lineId, "Bus");
        int dailyPassengers = this.dailyPassengers.getOrDefault(lineId, 1000);

        System.out.println("Optimizing line: " + lineId + ", Display Name: " + selectedDisplayName + ", Stops: " + stops);

        if (stops == null || stops.size() < 2) {
            resultTextArea.setText("Invalid transit line: " + selectedDisplayName);
            return;
        }

        double originalTime = calculateOriginalTravelTime(stops, timeOfDay);
        System.out.println("Original Total Travel Time for " + lineId + ": " + originalTime + " min");

        String timeOfDayEnglish = timeOfDay.equals("Morning Peak") ? "MorningPeak" :
                timeOfDay.equals("Afternoon") ? "Afternoon" :
                        timeOfDay.equals("Evening Peak") ? "EveningPeak" : "Night";

        List<Double> segmentTimes = new ArrayList<>();
        for (int i = 1; i < stops.size(); i++) {
            String from = stops.get(i - 1);
            String to = stops.get(i);
            Road road = getRoad(from, to);
            if (road != null) {
                String key = from + "-" + to;
                if (!trafficFlow.containsKey(key)) key = to + "-" + from;
                Map<String, Integer> traffic = trafficFlow.get(key);
                double baseTime = road.getDistance() / 50.0 * 60;
                double congestion = calculateCongestion(road.getCapacity(), timeOfDayEnglish, traffic);
                segmentTimes.add(baseTime * (1 + congestion));
            } else {
                segmentTimes.add(10.0);
            }
        }

        int maxVehicles = vehicleCounts.getOrDefault(lineId, 10);
        double[][] dp = new double[stops.size()][maxVehicles + 1];
        for (double[] row : dp) Arrays.fill(row, Double.MAX_VALUE);
        dp[0][0] = 0;

        for (int i = 1; i < stops.size(); i++) {
            double travelTime = segmentTimes.get(i - 1);
            for (int v = 1; v <= maxVehicles; v++) {
                for (int prevV = 0; prevV <= v; prevV++) {
                    if (dp[i - 1][prevV] == Double.MAX_VALUE) continue;
                    double newTime = dp[i - 1][prevV] + (travelTime / (1 + (v - prevV) * 0.2));
                    if (newTime < dp[i][v]) {
                        dp[i][v] = newTime;
                    }
                }
            }
        }

        double bestTime = Double.MAX_VALUE;
        int bestVehicles = 0;
        for (int v = 0; v <= maxVehicles; v++) {
            if (dp[stops.size() - 1][v] < bestTime) {
                bestTime = dp[stops.size() - 1][v];
                bestVehicles = v;
            }
        }

        List<Double> arrivalTimes = new ArrayList<>();
        double cumulativeTime = 0;
        arrivalTimes.add(cumulativeTime);
        for (int i = 1; i < stops.size(); i++) {
            cumulativeTime += segmentTimes.get(i - 1) / (1 + bestVehicles * 0.2);
            arrivalTimes.add(cumulativeTime);
        }

        double optimizedTime = bestTime;
        double timeImprovement = (originalTime > optimizedTime && originalTime > 0) ? ((originalTime - optimizedTime) / originalTime) * 100 : 0;
        timeImprovement = Math.max(0, Math.min(timeImprovement, 20.0));

        tableModel.setRowCount(0);
        previousScheduleData = new ArrayList<>();
        double frequency = bestVehicles > 0 ? 60.0 / bestVehicles : 15.0;
        for (int i = 0; i < stops.size(); i++) {
            Object[] row = new Object[]{
                    getNodeNameById(stops.get(i)),
                    String.format("%.1f", arrivalTimes.get(i)),
                    i == 0 ? bestVehicles : "-",
                    i == 0 ? String.format("%.1f", frequency) : "-"
            };
            previousScheduleData.add(row);
            tableModel.addRow(row);
        }

        resultTextArea.setText(String.format("Optimized %s:\n- %d Vehicles\n- Frequency: %.1f minutes\n- Travel Time Reduced by: %.1f%%",
                selectedDisplayName, bestVehicles, frequency, timeImprovement));
        currentLineStops = stops;
        highlightedPath = null;

        System.out.println("Setting MapPanel with stops: " + currentLineStops);
        mapPanel.setCurrentLineStops(currentLineStops);
        mapPanel.setHighlightedPath(highlightedPath);
        mapPanel.setTimeOfDay(timeOfDayEnglish);
        mapPanel.repaint();
    }

    private void optimizeNetwork() {
        String selectedDisplayName = (String) lineCombo.getSelectedItem();
        String lineId = getLineIdFromDisplayName(selectedDisplayName);
        List<String> stops = transitLines.get(lineId);
        if (stops == null || stops.size() < 2) {
            resultTextArea.setText("Invalid transit line: " + selectedDisplayName);
            return;
        }

        String start = stops.get(0);
        String end = stops.get(stops.size() - 1);
        String timeOfDay = (String) timeCombo.getSelectedItem();
        String timeOfDayEnglish = timeOfDay.equals("Morning Peak") ? "MorningPeak" :
                timeOfDay.equals("Afternoon") ? "Afternoon" :
                        timeOfDay.equals("Evening Peak") ? "EveningPeak" : "Night";

        Map<String, List<Dijkstra.Edge>> graph = networkDesigner.buildGraph(timeOfDayEnglish);
        double originalTime = calculateOriginalTravelTime(stops, timeOfDay);

        Dijkstra.PathResult pathResult = Dijkstra.findShortestPath(graph, start, end, timeOfDayEnglish, trafficFlow, closedRoads);
        if (pathResult.path.isEmpty() || pathResult.path.size() < 2) {
            resultTextArea.setText("No valid alternative path found for " + selectedDisplayName + ". Check if there are enough roads in the database connecting the stops.");
            return;
        }

        if (pathResult.path.equals(stops)) {
            Map<String, List<Dijkstra.Edge>> modifiedGraph = new HashMap<>(graph);
            for (int i = 0; i < stops.size() - 1; i++) {
                String from = stops.get(i);
                String to = stops.get(i + 1);
                List<Dijkstra.Edge> edges = modifiedGraph.get(from);
                if (edges != null) {
                    edges.removeIf(edge -> edge.to.equals(to));
                }
            }
            pathResult = Dijkstra.findShortestPath(modifiedGraph, start, end, timeOfDayEnglish, trafficFlow, closedRoads);
            if (pathResult.path.isEmpty() || pathResult.path.size() < 2) {
                resultTextArea.setText("No valid alternative path found for " + selectedDisplayName + " after excluding the original route. Check road connectivity in the database.");
                return;
            }
        }

        List<Double> segmentTimes = new ArrayList<>();
        boolean validPath = true;
        for (int i = 1; i < pathResult.path.size(); i++) {
            String from = pathResult.path.get(i - 1);
            String to = pathResult.path.get(i);
            Road road = getRoad(from, to);
            if (road != null) {
                String key = from + "-" + to;
                if (!trafficFlow.containsKey(key)) key = to + "-" + from;
                Map<String, Integer> traffic = trafficFlow.get(key);
                double baseTime = road.getDistance() / 50.0 * 60;
                double congestion = calculateCongestion(road.getCapacity(), timeOfDayEnglish, traffic);
                segmentTimes.add(baseTime * (1 + congestion));
            } else {
                validPath = false;
                break;
            }
        }

        if (!validPath || segmentTimes.isEmpty()) {
            resultTextArea.setText("Cannot calculate travel times for the alternative path of " + selectedDisplayName + ". Missing road connections between stops.");
            return;
        }

        int maxVehicles = vehicleCounts.getOrDefault(lineId, 10);
        if (maxVehicles <= 0) {
            resultTextArea.setText("No vehicles available for " + selectedDisplayName + ". Check vehicle counts in the database.");
            return;
        }

        double[][] dp = new double[pathResult.path.size()][maxVehicles + 1];
        for (double[] row : dp) Arrays.fill(row, Double.MAX_VALUE);
        dp[0][0] = 0;

        for (int i = 1; i < pathResult.path.size(); i++) {
            double travelTime = segmentTimes.get(i - 1);
            for (int v = 1; v <= maxVehicles; v++) {
                for (int prevV = 0; prevV <= v; prevV++) {
                    if (dp[i - 1][prevV] == Double.MAX_VALUE) continue;
                    double newTime = dp[i - 1][prevV] + (travelTime / (1 + (v - prevV) * 0.2));
                    if (newTime < dp[i][v]) {
                        dp[i][v] = newTime;
                    }
                }
            }
        }

        double bestTime = Double.MAX_VALUE;
        int bestVehicles = 0;
        for (int v = 0; v <= maxVehicles; v++) {
            if (dp[pathResult.path.size() - 1][v] < bestTime) {
                bestTime = dp[pathResult.path.size() - 1][v];
                bestVehicles = v;
            }
        }

        if (bestTime == Double.MAX_VALUE || bestTime <= 0) {
            resultTextArea.setText("Failed to optimize travel time for " + selectedDisplayName + ". Check road connectivity and traffic data.");
            return;
        }

        double timeImprovement = (originalTime > bestTime && originalTime > 0) ?
                ((originalTime - bestTime) / originalTime) * 100 : 0;

        tableModel.setRowCount(0);
        previousScheduleData = new ArrayList<>();
        double cumulativeTime = 0;
        double frequency = bestVehicles > 0 ? 60.0 / bestVehicles : 15.0;
        for (int i = 0; i < pathResult.path.size(); i++) {
            if (i > 0) {
                cumulativeTime += segmentTimes.get(i - 1) / (1 + bestVehicles * 0.2);
            }
            Object[] row = new Object[]{
                    getNodeNameById(pathResult.path.get(i)),
                    String.format("%.1f", cumulativeTime),
                    i == 0 ? bestVehicles : "-",
                    i == 0 ? String.format("%.1f", frequency) : "-"
            };
            previousScheduleData.add(row);
            tableModel.addRow(row);
        }

        resultTextArea.setText(String.format("Alternative Route for %s:\n- Original Time: %.1f min\n- Optimized Time: %.1f min\n- Vehicles Used: %d\n- Travel Time Reduced by: %.1f%%\nRoute: %s",
                selectedDisplayName, originalTime, bestTime, bestVehicles, timeImprovement, pathResult.path.toString()));

        highlightedPath = pathResult.path;
        currentLineStops = null;
        mapPanel.setHighlightedPath(highlightedPath);
        mapPanel.setCurrentLineStops(currentLineStops);
        mapPanel.setTimeOfDay(timeOfDayEnglish);
        mapPanel.repaint();
    }

    private void updateLineCombo() {
        String selectedType = (String) transitTypeCombo.getSelectedItem();
        lineCombo.removeAllItems();
        for (String lineId : transitLines.keySet()) {
            String lineType = lineTypes.getOrDefault(lineId, "Bus");
            String displayName = lineDisplayNames.getOrDefault(lineId, lineId);
            if (selectedType.equals("All") ||
                    (selectedType.equals("Metro") && "Metro".equals(lineType)) ||
                    (selectedType.equals("Bus") && "Bus".equals(lineType))) {
                lineCombo.addItem(displayName);
            }
        }
        System.out.println("Updated lineCombo with type " + selectedType + ": " + lineCombo.getItemCount() + " items");
    }

    private String getLineIdFromDisplayName(String displayName) {
        for (Map.Entry<String, String> entry : lineDisplayNames.entrySet()) {
            if (entry.getValue().equals(displayName)) {
                return entry.getKey();
            }
        }
        return displayName;
    }

    private double calculateOriginalTravelTime(List<String> stops, String timeOfDay) {
        String timeOfDayEnglish = timeOfDay.equals("Morning Peak") ? "MorningPeak" :
                timeOfDay.equals("Afternoon") ? "Afternoon" :
                        timeOfDay.equals("Evening Peak") ? "EveningPeak" : "Night";
        double totalTime = 0;
        for (int i = 0; i < stops.size() - 1; i++) {
            String from = stops.get(i);
            String to = stops.get(i + 1);
            Road road = getRoad(from, to);
            if (road != null) {
                String key = from + "-" + to;
                if (!trafficFlow.containsKey(key)) key = to + "-" + from;
                Map<String, Integer> traffic = trafficFlow.get(key);
                double travelTime = calculateTravelTime(road.getDistance(), road.getCapacity(), timeOfDayEnglish, traffic);
                totalTime += travelTime > 0 ? travelTime : 5.0;
                System.out.println("Original Travel Time " + from + " to " + to + ": " + travelTime + " min");
            } else {
                totalTime += 10.0;
                System.out.println("Warning: No road between " + from + " and " + to + ", adding 10 min");
            }
        }
        return totalTime > 0 ? totalTime : 10.0;
    }

    private double calculateTravelTime(double distance, int capacity, String timeOfDay, Map<String, Integer> traffic) {
        double baseTime = distance / 50.0 * 60;
        double congestion = traffic != null ? calculateCongestion(capacity, timeOfDay, traffic) : 0.3;
        return baseTime * (1 + congestion) > 0 ? baseTime * (1 + congestion) : 5.0;
    }

    private double calculateCongestion(int capacity, String timeOfDay, Map<String, Integer> traffic) {
        double trafficVolume = traffic != null ? traffic.getOrDefault(timeOfDay, 500) : 500;
        return capacity > 0 ? Math.min(trafficVolume / capacity, 1.0) : 0.5;
    }

    private Road getRoad(String fromId, String toId) {
        return roads.stream()
                .filter(r -> (r.fromId.equals(fromId) && r.toId.equals(toId)) || (r.fromId.equals(toId) && r.toId.equals(fromId)))
                .findFirst()
                .orElse(null);
    }

    private String getNodeNameById(String id) {
        Node node = nodes.get(id);
        return node != null ? node.getName() : id;
    }

    private void loadTransitLines(Connection conn) throws SQLException {
        transitLines = new HashMap<>();
        lineTypes = new HashMap<>();
        dailyPassengers = new HashMap<>();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM TransitLines");
        while (rs.next()) {
            String lineId = rs.getString("LineID");
            String lineName = rs.getString("LineName");
            String stopsStr = rs.getString("Stops");
            String lineType = rs.getString("LineType");
            int passengers = rs.getInt("DailyPassengers");
            List<String> stops = Arrays.asList(stopsStr.split(","));
            transitLines.put(lineId, stops);
            lineTypes.put(lineId, lineType);
            dailyPassengers.put(lineId, passengers);
            lineDisplayNames.put(lineId, lineName);
            System.out.println("Loaded line: " + lineId + " (Type: " + lineType + ", Name: " + lineName + ", Stops: " + stopsStr + ")");
        }
        rs.close();
        stmt.close();
        if (transitLines.isEmpty()) {
            System.out.println("Warning: No transit lines loaded from database!");
        }
    }
}