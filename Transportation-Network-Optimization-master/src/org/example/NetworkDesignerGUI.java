package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.image.RescaleOp;

public class NetworkDesignerGUI extends JFrame {
    private Map<String, Node> nodes;
    private List<Road> roads;
    private List<Road> selectedRoads;
    private InfrastructureNetworkDesigner designer;
    private GraphPanel graphPanel;
    private JTable roadTable;
    private JComboBox<String> priorityComboBox;
    private JComboBox<String> costDistanceComboBox;
    private JComboBox<String> roadTypeComboBox;

    public NetworkDesignerGUI(Connection conn) {
        try {
            this.nodes = DataLoader.loadNodes(conn);
            this.roads = removeDuplicateRoads(DataLoader.loadRoads(conn));
            this.designer = new InfrastructureNetworkDesigner(nodes, this.roads);
            this.selectedRoads = new ArrayList<>();
            initUI();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initUI() {
        setTitle("Road Optimizer");
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(800, 600));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JLabel background = new JLabel();
        background.setBackground(new Color(245, 245, 220));
        background.setOpaque(true);
        background.setLayout(new BorderLayout(10, 10));
        setContentPane(background);

        JLabel titleLabel = new JLabel("Road Optimizer", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(74, 74, 74));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        JPanel mapOuterContainer = new JPanel(new BorderLayout());
        mapOuterContainer.setBackground(new Color(245, 245, 220));
        JPanel mapContainer = new JPanel(new BorderLayout());
        mapContainer.setBackground(new Color(245, 245, 220));
        graphPanel = new GraphPanel();
        graphPanel.setPreferredSize(new Dimension(950, 700));
        graphPanel.setBackground(new Color(245, 245, 220));
        graphPanel.setBorder(BorderFactory.createLineBorder(new Color(193, 154, 107), 1));
        mapContainer.add(graphPanel, BorderLayout.CENTER);
        mapOuterContainer.add(mapContainer, BorderLayout.CENTER);
        mapOuterContainer.add(Box.createRigidArea(new Dimension(50, 0)), BorderLayout.WEST);
        add(mapOuterContainer, BorderLayout.WEST);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(new Color(210, 180, 140));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.setBackground(new Color(210, 180, 140));
        controlPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(193, 154, 107)),
                "Controls",
                0, 0, null, new Color(74, 74, 74)
        ));
        controlPanel.setMaximumSize(new Dimension(400, 350));

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel priorityLabel = new JLabel("Priority:");
        priorityLabel.setForeground(new Color(74, 74, 74));
        controlPanel.add(priorityLabel, gbc);
        gbc.gridx = 1;
        priorityComboBox = createStyledComboBox(new String[]{"HIGH POPULATION AREAS", "LOW POPULATION AREAS"});
        controlPanel.add(priorityComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel costDistanceLabel = new JLabel("Cost/Distance:");
        costDistanceLabel.setForeground(new Color(74, 74, 74));
        controlPanel.add(costDistanceLabel, gbc);
        gbc.gridx = 1;
        costDistanceComboBox = createStyledComboBox(new String[]{
                "LOW (< 500M EGP or < 8 KM)",
                "MEDIUM (500M-1000M EGP or 8-15 KM)",
                "HIGH (> 1000M EGP or > 15 KM)"
        });
        controlPanel.add(costDistanceComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel roadTypeLabel = new JLabel("Road Type:");
        roadTypeLabel.setForeground(new Color(74, 74, 74));
        controlPanel.add(roadTypeLabel, gbc);
        gbc.gridx = 1;
        roadTypeComboBox = createStyledComboBox(new String[]{"EXISTING ROADS", "NEW ROADS"});
        controlPanel.add(roadTypeComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton generateButton = createStyledButton("Generate", new Color(47, 79, 79));
        generateButton.addActionListener(e -> generateNetwork());
        controlPanel.add(generateButton, gbc);

        gbc.gridy = 4;
        JButton resetButton = createStyledButton("Reset", new Color(139, 69, 19));
        resetButton.addActionListener(e -> resetNetwork());
        controlPanel.add(resetButton, gbc);

        gbc.gridy = 5;
        JButton closeButton = createStyledButton("Close", new Color(139, 69, 19));
        closeButton.addActionListener(e -> dispose());
        controlPanel.add(closeButton, gbc);

        rightPanel.add(controlPanel);

        String[] columnNames = {"From", "To", "Distance (KM)", "Cost (M EGP)", "Status"};
        Object[][] emptyData = new Object[0][5];
        DefaultTableModel tableModel = new DefaultTableModel(emptyData, columnNames);
        roadTable = new JTable(tableModel);
        roadTable.setRowHeight(25);
        roadTable.setFont(new Font("Arial", Font.PLAIN, 12));
        roadTable.setForeground(new Color(74, 74, 74));
        roadTable.setBackground(new Color(255, 255, 255));
        roadTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        roadTable.getTableHeader().setBackground(new Color(210, 180, 140));
        roadTable.getTableHeader().setForeground(new Color(74, 74, 74));
        roadTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(row % 2 == 0 ? new Color(255, 255, 255) : new Color(245, 245, 220));
                c.setForeground(new Color(74, 74, 74));
                return c;
            }
        });
        JScrollPane tableScrollPane = new JScrollPane(roadTable);
        tableScrollPane.setPreferredSize(new Dimension(400, 400));
        tableScrollPane.setBackground(new Color(210, 180, 140));
        tableScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(193, 154, 107)),
                "Road Details",
                0, 0, null, new Color(74, 74, 74)
        ));
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(tableScrollPane);

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
                JOptionPane.showMessageDialog(NetworkDesignerGUI.this,
                        "Cannot find MainMenu class. Ensure MainMenu.java is in the org.example package and compiled.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(NetworkDesignerGUI.this,
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

        add(rightPanel, BorderLayout.EAST);
        setVisible(true);
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(new Color(255, 255, 255));
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(200, 35));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(193, 154, 107), 1));
        return button;
    }

    private JComboBox<String> createStyledComboBox(String[] items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setFont(new Font("Arial", Font.PLAIN, 12));
        comboBox.setBackground(new Color(255, 255, 255));
        comboBox.setForeground(new Color(74, 74, 74));
        comboBox.setPreferredSize(new Dimension(200, 30));
        comboBox.setBorder(BorderFactory.createLineBorder(new Color(193, 154, 107), 1));
        return comboBox;
    }

    private List<Road> removeDuplicateRoads(List<Road> roads) {
        Set<String> roadSet = new HashSet<>();
        List<Road> uniqueRoads = new ArrayList<>();
        for (Road road : roads) {
            String roadKey = road.getFromId() + "-" + road.getToId();
            String reverseKey = road.getToId() + "-" + road.getFromId();
            if (!roadSet.contains(roadKey) && !roadSet.contains(reverseKey)) {
                roadSet.add(roadKey);
                uniqueRoads.add(road);
            }
        }
        return uniqueRoads;
    }

    private void generateNetwork() {
        List<Road> filteredRoads = new ArrayList<>(roads);

        String priority = (String) priorityComboBox.getSelectedItem();
        if ("HIGH POPULATION AREAS".equals(priority)) {
            filteredRoads.removeIf(road -> {
                Node fromNode = nodes.get(road.getFromId());
                Node toNode = nodes.get(road.getToId());
                return fromNode == null || toNode == null ||
                        (fromNode.getPopulation() == null || fromNode.getPopulation() <= 200000) &&
                                (toNode.getPopulation() == null || toNode.getPopulation() <= 200000);
            });
        } else {
            filteredRoads.removeIf(road -> {
                Node fromNode = nodes.get(road.getFromId());
                Node toNode = nodes.get(road.getToId());
                return fromNode == null || toNode == null ||
                        (fromNode.getPopulation() != null && fromNode.getPopulation() > 200000) ||
                        (toNode.getPopulation() != null && toNode.getPopulation() > 200000);
            });
        }

        String costDistance = (String) costDistanceComboBox.getSelectedItem();
        filteredRoads.removeIf(road -> {
            double distance = road.getDistance();
            double cost = road.getCost();
            if ("LOW (< 500M EGP or < 8 KM)".equals(costDistance)) {
                return !(distance < 8 || (!road.isExisting() && cost < 500));
            } else if ("MEDIUM (500M-1000M EGP or 8-15 KM)".equals(costDistance)) {
                return !((distance >= 8 && distance <= 15) || (!road.isExisting() && cost >= 500 && cost <= 1000));
            } else {
                return !(distance > 15 || (!road.isExisting() && cost > 1000));
            }
        });

        String roadType = (String) roadTypeComboBox.getSelectedItem();
        if ("EXISTING ROADS".equals(roadType)) {
            filteredRoads.removeIf(road -> !road.isExisting());
        } else {
            filteredRoads.removeIf(Road::isExisting);
        }

        designer = new InfrastructureNetworkDesigner(nodes, filteredRoads);
        selectedRoads = designer.buildOptimalRoadNetwork();
        updateTable();
        graphPanel.repaint();
    }

    private void resetNetwork() {
        selectedRoads.clear();
        priorityComboBox.setSelectedIndex(0);
        costDistanceComboBox.setSelectedIndex(0);
        roadTypeComboBox.setSelectedIndex(0);
        roadTable.setModel(new DefaultTableModel(new Object[0][5], new String[]{"From", "To", "Distance (KM)", "Cost (M EGP)", "Status"}));
        graphPanel.repaint();
    }

    private void updateTable() {
        String[] columnNames = {"From", "To", "Distance (KM)", "Cost (M EGP)", "Status"};
        Object[][] data = new Object[selectedRoads.size()][5];
        for (int i = 0; i < selectedRoads.size(); i++) {
            Road road = selectedRoads.get(i);
            Node fromNode = nodes.get(road.getFromId());
            Node toNode = nodes.get(road.getToId());
            data[i][0] = fromNode != null ? fromNode.getName() : road.getFromId();
            data[i][1] = toNode != null ? toNode.getName() : road.getToId();
            data[i][2] = String.format("%.1f", road.getDistance());
            data[i][3] = road.isExisting() ? "N/A" : String.format("%.1f", road.getCost());
            data[i][4] = road.isExisting() ? "Existing" : "New";
        }
        roadTable.setModel(new DefaultTableModel(data, columnNames));
    }

    private class GraphPanel extends JPanel {
        private Node hoveredNode = null;
        private static final int NODE_RADIUS = 5;

        public GraphPanel() {
            setToolTipText("");
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    hoveredNode = null;
                    double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
                    double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
                    for (Node node : nodes.values()) {
                        minX = Math.min(minX, node.getX());
                        maxX = Math.max(maxX, node.getX());
                        minY = Math.min(minY, node.getY());
                        maxY = Math.max(maxY, node.getY());
                    }

                    double width = getWidth() - 50;
                    double height = getHeight() - 50;
                    double xScale = (maxX == minX) ? 1 : width / (maxX - minX);
                    double yScale = (maxY == minY) ? 1 : height / (maxY - minY);

                    for (Node node : nodes.values()) {
                        int x = (int) ((node.getX() - minX) * xScale) + 25;
                        int y = (int) ((node.getY() - minY) * yScale) + 25;
                        if (Math.sqrt(Math.pow(e.getX() - x, 2) + Math.pow(e.getY() - y, 2)) <= NODE_RADIUS + 5) {
                            hoveredNode = node;
                            break;
                        }
                    }
                    repaint();
                }
            });
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            if (hoveredNode != null) {
                return String.format("<html><b>%s</b><br>Population: %s<br>Type: %s</html>",
                        hoveredNode.getName(),
                        hoveredNode.getPopulation() != null ? hoveredNode.getPopulation() : "N/A",
                        hoveredNode.getType());
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
            double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
            for (Node node : nodes.values()) {
                minX = Math.min(minX, node.getX());
                maxX = Math.max(maxX, node.getX());
                minY = Math.min(minY, node.getY());
                maxY = Math.max(maxY, node.getY());
            }

            double width = getWidth() - 50;
            double height = getHeight() - 50;
            double xScale = (maxX == minX) ? 1 : width / (maxX - minX);
            double yScale = (maxY == minY) ? 1 : height / (maxY - minY);

            for (Road road : roads) {
                Node fromNode = nodes.get(road.getFromId());
                Node toNode = nodes.get(road.getToId());
                if (fromNode == null || toNode == null) continue;

                int x1 = (int) ((fromNode.getX() - minX) * xScale) + 25;
                int y1 = (int) ((fromNode.getY() - minY) * yScale) + 25;
                int x2 = (int) ((toNode.getX() - minX) * xScale) + 25;
                int y2 = (int) ((toNode.getY() - minY) * yScale) + 25;

                if (selectedRoads.contains(road)) {
                    g2d.setColor(new Color(139, 69, 19));
                    g2d.setStroke(new BasicStroke(3));
                } else {
                    g2d.setColor(new Color(107, 142, 35));
                    g2d.setStroke(new BasicStroke(1));
                }
                g2d.drawLine(x1, y1, x2, y2);
            }

            for (Node node : nodes.values()) {
                if (node.getPopulation() == null) {
                    System.out.println("Node with null population: " + node.getId() + ", Name: " + node.getName());
                }
                int x = (int) ((node.getX() - minX) * xScale) + 25;
                int y = (int) ((node.getY() - minY) * yScale) + 25;

                g2d.setColor((node.getPopulation() != null && node.getPopulation() > 200000) ?
                        new Color(139, 69, 19) : new Color(245, 222, 179));
                g2d.fillOval(x - NODE_RADIUS, y - NODE_RADIUS, 2 * NODE_RADIUS, 2 * NODE_RADIUS);

                if (node == hoveredNode) {
                    g2d.setColor(new Color(74, 74, 74));
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    g2d.drawString(node.getName(), x + 10, y);
                }
            }

            if (selectedRoads != null && !selectedRoads.isEmpty()) {
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                g2d.setColor(new Color(74, 74, 74));
                Set<String> drawnNodes = new HashSet<>();
                int stopNumber = 1;
                for (Road road : selectedRoads) {
                    Node fromNode = nodes.get(road.getFromId());
                    Node toNode = nodes.get(road.getToId());
                    if (fromNode == null || toNode == null) continue;

                    if (!drawnNodes.contains(road.getFromId())) {
                        int x = (int) ((fromNode.getX() - minX) * xScale) + 25;
                        int y = (int) ((fromNode.getY() - minY) * yScale) + 25;
                        g2d.drawString(String.valueOf(stopNumber++), x - 15, y + 5);
                        drawnNodes.add(road.getFromId());
                    }
                    if (!drawnNodes.contains(road.getToId())) {
                        int x = (int) ((toNode.getX() - minX) * xScale) + 25;
                        int y = (int) ((toNode.getY() - minY) * yScale) + 25;
                        g2d.drawString(String.valueOf(stopNumber++), x - 15, y + 5);
                        drawnNodes.add(road.getToId());
                    }
                }
            }
        }
    }
}