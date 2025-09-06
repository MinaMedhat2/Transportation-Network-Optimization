package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MapPanel extends JPanel {
    private Map<String, Node> nodes;
    private List<Road> roads;
    private Map<String, Map<String, Integer>> trafficFlow;
    private List<String> currentLineStops;
    private List<String> highlightedPath;
    private String timeOfDay = "Morning Peak";
    private List<Road> closedRoads;
    private boolean highlightRouteOnly;
    private String hoveredNode = null;
    private String vehicleType;
    private boolean blinkState = true;
    private float animationProgress = 0.0f;
    private Timer animationTimer;

    public MapPanel(Map<String, Node> nodes, List<Road> roads, Map<String, Map<String, Integer>> trafficFlow,
                    List<Road> closedRoads, boolean highlightRouteOnly) {
        this.nodes = nodes;
        this.roads = roads;
        this.trafficFlow = trafficFlow;
        this.closedRoads = closedRoads;
        this.highlightRouteOnly = highlightRouteOnly;

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                double minX = nodes.values().stream().mapToDouble(Node::getX).min().orElse(0);
                double maxX = nodes.values().stream().mapToDouble(Node::getX).max().orElse(1);
                double minY = nodes.values().stream().mapToDouble(Node::getY).min().orElse(0);
                double maxY = nodes.values().stream().mapToDouble(Node::getY).max().orElse(1);

                int mouseX = e.getX();
                int mouseY = e.getY();
                String closestNode = null;
                double minDistance = Double.MAX_VALUE;

                for (Node node : nodes.values()) {
                    int x = (int) ((node.getX() - minX) / (maxX - minX) * (getWidth() - 50) + 25);
                    int y = (int) ((node.getY() - minY) / (maxY - minY) * (getHeight() - 50) + 25);
                    double distance = Math.sqrt(Math.pow(mouseX - x, 2) + Math.pow(mouseY - y, 2));
                    if (distance < 10 && distance < minDistance) {
                        closestNode = node.getId();
                        minDistance = distance;
                    }
                }

                if (closestNode != null && !closestNode.equals(hoveredNode)) {
                    hoveredNode = closestNode;
                    repaint();
                } else if (closestNode == null && hoveredNode != null) {
                    hoveredNode = null;
                    repaint();
                }
            }
        });
    }

    public void setCurrentLineStops(List<String> currentLineStops) {
        this.currentLineStops = currentLineStops;
    }

    public void setHighlightedPath(List<String> highlightedPath) {
        this.highlightedPath = highlightedPath;
        if (highlightedPath != null && vehicleType != null && (vehicleType.equals("Ambulance") || vehicleType.equals("Fire Truck"))) {
            startAnimation();
        } else {
            stopAnimation();
        }
        repaint();
    }

    public void setTimeOfDay(String timeOfDay) {
        this.timeOfDay = timeOfDay != null ? timeOfDay : "Morning Peak";
    }

    public void setHighlightRouteOnly(boolean highlightRouteOnly) {
        this.highlightRouteOnly = highlightRouteOnly;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
        if (vehicleType == null || !vehicleType.equals("Ambulance") && !vehicleType.equals("Fire Truck")) {
            stopAnimation();
        }
        repaint();
    }

    public void setBlinkState(boolean state) {
        this.blinkState = state;
        repaint();
    }

    private void startAnimation() {
        if (animationTimer != null) {
            animationTimer.cancel();
        }
        animationTimer = new Timer();
        animationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                animationProgress += 0.01f;
                if (animationProgress >= 1.0f) {
                    animationProgress = 0.0f;
                }
                repaint();
            }
        }, 0, 50); // Update every 50ms
    }

    private void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.cancel();
            animationTimer = null;
            animationProgress = 0.0f;
            repaint();
        }
    }

    private double calculateCongestion(int capacity, String timeOfDay, Map<String, Integer> traffic) {
        double trafficVolume = getTrafficForTime(traffic, timeOfDay != null ? timeOfDay : "Morning Peak");
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double minX = nodes.values().stream().mapToDouble(Node::getX).min().orElse(0);
        double maxX = nodes.values().stream().mapToDouble(Node::getX).max().orElse(1);
        double minY = nodes.values().stream().mapToDouble(Node::getY).min().orElse(0);
        double maxY = nodes.values().stream().mapToDouble(Node::getY).max().orElse(1);

        // Draw all roads
        if (!highlightRouteOnly) {
            for (Road road : roads) {
                if (closedRoads.contains(road)) continue;

                Node fromNode = nodes.get(road.fromId);
                Node toNode = nodes.get(road.toId);
                if (fromNode == null || toNode == null) continue;

                int x1 = (int) ((fromNode.getX() - minX) / (maxX - minX) * (getWidth() - 50) + 25);
                int y1 = (int) ((fromNode.getY() - minY) / (maxY - minY) * (getHeight() - 50) + 25);
                int x2 = (int) ((toNode.getX() - minX) / (maxX - minX) * (getWidth() - 50) + 25);
                int y2 = (int) ((toNode.getY() - minY) / (maxY - minY) * (getHeight() - 50) + 25);

                String key = road.fromId + "-" + road.toId;
                if (!trafficFlow.containsKey(key)) key = road.toId + "-" + road.fromId;
                Map<String, Integer> traffic = trafficFlow.get(key);

                if (road.isExisting()) {
                    if (traffic != null) {
                        double congestion = calculateCongestion(road.getCapacity(), timeOfDay, traffic);
                        Color baseColor = congestion > 0.8 ? new Color(139, 69, 19) :
                                congestion > 0.5 ? new Color(255, 215, 0) :
                                        new Color(107, 142, 35);
                        g2d.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(),
                                (currentLineStops == null && highlightedPath == null) ? 255 : 50));
                    } else {
                        g2d.setColor(new Color(107, 142, 35, 50));
                    }
                    g2d.setStroke(new BasicStroke((currentLineStops == null && highlightedPath == null) ? 1 : 0.5f));
                } else {
                    g2d.setColor(new Color(128, 128, 128, 100));
                    g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
                }
                g2d.drawLine(x1, y1, x2, y2);
            }
        }

        // Draw current transit line
        if (currentLineStops != null && currentLineStops.size() > 1) {
            g2d.setColor(new Color(107, 142, 35));
            g2d.setStroke(new BasicStroke(3));
            for (int i = 0; i < currentLineStops.size() - 1; i++) {
                Node fromNode = nodes.get(currentLineStops.get(i));
                Node toNode = nodes.get(currentLineStops.get(i + 1));
                if (fromNode == null || toNode == null) continue;

                int x1 = (int) ((fromNode.getX() - minX) / (maxX - minX) * (getWidth() - 50) + 25);
                int y1 = (int) ((fromNode.getY() - minY) / (maxY - minY) * (getHeight() - 50) + 25);
                int x2 = (int) ((toNode.getX() - minX) / (maxX - minX) * (getWidth() - 50) + 25);
                int y2 = (int) ((toNode.getY() - minY) / (maxY - minY) * (getHeight() - 50) + 25);
                g2d.drawLine(x1, y1, x2, y2);
            }
        }

        // Draw highlighted path
        if (highlightedPath != null && highlightedPath.size() > 1) {
            g2d.setStroke(new BasicStroke(3));
            if (vehicleType != null && (vehicleType.equals("Ambulance") || vehicleType.equals("Fire Truck"))) {
                g2d.setColor(blinkState ? Color.RED : Color.YELLOW);
            } else {
                g2d.setColor(new Color(139, 69, 19)); // Original color for Normal Car
            }

            for (int i = 0; i < highlightedPath.size() - 1; i++) {
                Node fromNode = nodes.get(highlightedPath.get(i));
                Node toNode = nodes.get(highlightedPath.get(i + 1));
                if (fromNode == null || toNode == null) continue;
                int x1 = (int) ((fromNode.getX() - minX) / (maxX - minX) * (getWidth() - 50) + 25);
                int y1 = (int) ((fromNode.getY() - minY) / (maxY - minY) * (getHeight() - 50) + 25);
                int x2 = (int) ((toNode.getX() - minX) / (maxX - minX) * (getWidth() - 50) + 25);
                int y2 = (int) ((toNode.getY() - minY) / (maxY - minY) * (getHeight() - 50) + 25);
                g2d.drawLine(x1, y1, x2, y2);
            }

            // Draw moving box for emergency vehicles
            if (vehicleType != null && (vehicleType.equals("Ambulance") || vehicleType.equals("Fire Truck")) &&
                    highlightedPath.size() >= 2) {
                int segmentIndex = (int) (animationProgress * (highlightedPath.size() - 1));
                if (segmentIndex >= highlightedPath.size() - 1) segmentIndex = highlightedPath.size() - 2;

                Node fromNode = nodes.get(highlightedPath.get(segmentIndex));
                Node toNode = nodes.get(highlightedPath.get(segmentIndex + 1));
                if (fromNode != null && toNode != null) {
                    float segmentProgress = (animationProgress * (highlightedPath.size() - 1)) - segmentIndex;
                    int x1 = (int) ((fromNode.getX() - minX) / (maxX - minX) * (getWidth() - 50) + 25);
                    int y1 = (int) ((fromNode.getY() - minY) / (maxY - minY) * (getHeight() - 50) + 25);
                    int x2 = (int) ((toNode.getX() - minX) / (maxX - minX) * (getWidth() - 50) + 25);
                    int y2 = (int) ((toNode.getY() - minY) / (maxY - minY) * (getHeight() - 50) + 25);

                    int boxX = (int) (x1 + (x2 - x1) * segmentProgress);
                    int boxY = (int) (y1 + (y2 - y1) * segmentProgress);

                    g2d.setColor(blinkState ? Color.RED : Color.YELLOW);
                    g2d.fillRect(boxX - 10, boxY - 10, 20, 20);

                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.BOLD, 10));
                    FontMetrics fm = g2d.getFontMetrics();
                    String label = vehicleType.equals("Ambulance") ? "AMB" : "FT";
                    int textWidth = fm.stringWidth(label);
                    g2d.drawString(label, boxX - textWidth / 2, boxY + fm.getAscent() / 2);
                }
            }
        }

        // Draw nodes
        int nodesDrawn = 0;
        for (Node node : nodes.values()) {
            if (node.getPopulation() == null) {
                System.out.println("Node with null population: " + node.getId() + ", Name: " + node.getName());
            }
            int x = (int) ((node.getX() - minX) / (maxX - minX) * (getWidth() - 50) + 25);
            int y = (int) ((node.getY() - minY) / (maxY - minY) * (getHeight() - 50) + 25);
            g2d.setColor((node.getPopulation() != null && node.getPopulation() > 500000) ?
                    new Color(139, 69, 19) : new Color(245, 222, 179));
            g2d.fillOval(x - 5, y - 5, 10, 10);
            nodesDrawn++;
            System.out.println("Drawing node: " + node.getId() + " (" + node.getName() + ") at (" + x + ", " + y + ")");

            if (node.getId().equals(hoveredNode)) {
                g2d.setColor(new Color(74, 74, 74));
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                g2d.drawString(node.getName(), x + 10, y);
            }
        }
        System.out.println("Total nodes drawn: " + nodesDrawn);

        // Draw stop numbers for transit lines
        if (currentLineStops != null && currentLineStops.size() > 1) {
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.setColor(new Color(74, 74, 74));
            for (int i = 0; i < currentLineStops.size(); i++) {
                Node node = nodes.get(currentLineStops.get(i));
                if (node == null) continue;
                int x = (int) ((node.getX() - minX) / (maxX - minX) * (getWidth() - 50) + 25);
                int y = (int) ((node.getY() - minY) / (maxY - minY) * (getHeight() - 50) + 25);
                g2d.drawString(String.valueOf(i + 1), x - 15, y + 5);
            }
        }

        // Draw stop numbers for highlighted path
        if (highlightedPath != null && highlightedPath.size() > 1) {
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.setColor(new Color(74, 74, 74));
            for (int i = 0; i < highlightedPath.size(); i++) {
                Node node = nodes.get(highlightedPath.get(i));
                if (node == null) continue;
                int x = (int) ((node.getX() - minX) / (maxX - minX) * (getWidth() - 50) + 25);
                int y = (int) ((node.getY() - minY) / (maxY - minY) * (getHeight() - 50) + 25);
                g2d.drawString(String.valueOf(i + 1), x - 15, y + 5);
            }
        }
    }
}