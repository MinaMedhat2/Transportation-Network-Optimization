package org.example;

import javax.swing.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InfrastructureNetworkDesigner {
    private Map<String, Node> nodes;
    private List<Road> roads;

    public InfrastructureNetworkDesigner(Map<String, Node> nodes, List<Road> roads) {
        this.nodes = nodes;
        this.roads = roads;
    }

    public InfrastructureNetworkDesigner(Connection conn) throws SQLException {
        this.nodes = DataLoader.loadNodes(conn);
        this.roads = DataLoader.loadRoads(conn);
    }

    public List<Road> buildOptimalRoadNetwork() {
        List<Road> selectedRoads = new ArrayList<>();

        UnionFind uf = new UnionFind(nodes.keySet());
        List<Road> remainingRoads = new ArrayList<>();

        for (Road road : roads) {
            String from = road.getFromId();
            String to = road.getToId();
            if (road.isExisting() && !uf.connected(from, to)) {
                uf.union(from, to);
                selectedRoads.add(road);
            } else if (!road.isExisting()) {
                remainingRoads.add(road);
            }
        }

        remainingRoads.sort((road1, road2) -> {
            double weight1 = calculateWeightedCost(road1);
            double weight2 = calculateWeightedCost(road2);
            return Double.compare(weight1, weight2);
        });

        for (Road road : remainingRoads) {
            String from = road.getFromId();
            String to = road.getToId();
            if (!uf.connected(from, to)) {
                uf.union(from, to);
                selectedRoads.add(road);
            }
        }

        return selectedRoads;
    }

    private double calculateWeightedCost(Road road) {
        Node fromNode = nodes.get(road.getFromId());
        Node toNode = nodes.get(road.getToId());
        double cost = road.getCost();

        boolean highPopulationFrom = fromNode.getPopulation() != null && fromNode.getPopulation() > 200000;
        boolean highPopulationTo = toNode.getPopulation() != null && toNode.getPopulation() > 200000;
        boolean isFacilityFrom = "Facility".equals(fromNode.getType());
        boolean isFacilityTo = "Facility".equals(toNode.getType());

        double priorityFactor = 1.0;
        if (highPopulationFrom || highPopulationTo || isFacilityFrom || isFacilityTo) {
            priorityFactor = 0.5;
        }

        return cost * priorityFactor;
    }

    public double calculateTotalConstructionCost(List<Road> selectedRoads) {
        double totalCost = 0.0;
        for (Road road : selectedRoads) {
            if (!road.isExisting()) {
                totalCost += road.getCost();
            }
        }
        return totalCost;
    }

    public static void main(String[] args) {
        Connection conn = DBConnection.connect();
        if (conn == null) {
            System.err.println("Failed to connect to the database.");
            return;
        }

        try {
            Map<String, Node> nodes = DataLoader.loadNodes(conn);
            List<Road> roads = DataLoader.loadRoads(conn);

            InfrastructureNetworkDesigner designer = new InfrastructureNetworkDesigner(nodes, roads);
            List<Road> mstRoads = designer.buildOptimalRoadNetwork();

            System.out.println("\nSelected Roads in Optimal Network:");
            for (Road road : mstRoads) {
                System.out.println(road.getFromId() + " -> " + road.getToId() +
                        (road.isExisting() ? " (Existing Road)" : " (New Road, Cost: " + road.getCost() + "M EGP)"));
            }

            double totalCost = designer.calculateTotalConstructionCost(mstRoads);
            System.out.println("\nTotal Construction Cost for New Roads: " + totalCost + " Million EGP");

            System.out.println("\nTime Complexity: O(E log E)");
            System.out.println("Space Complexity: O(V)");

            SwingUtilities.invokeLater(() -> {
                NetworkDesignerGUI gui = new NetworkDesignerGUI(conn);
                gui.setVisible(true);
            });
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error loading data: " + e.getMessage());
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

class UnionFind {
    private Map<String, String> parent;

    public UnionFind(Set<String> nodes) {
        parent = new HashMap<>();
        for (String node : nodes) {
            parent.put(node, node);
        }
    }

    public String find(String node) {
        if (!parent.get(node).equals(node)) {
            parent.put(node, find(parent.get(node)));
        }
        return parent.get(node);
    }

    public void union(String node1, String node2) {
        String root1 = find(node1);
        String root2 = find(node2);
        if (!root1.equals(root2)) {
            parent.put(root1, root2);
        }
    }

    public boolean connected(String node1, String node2) {
        return find(node1).equals(find(node2));
    }
}