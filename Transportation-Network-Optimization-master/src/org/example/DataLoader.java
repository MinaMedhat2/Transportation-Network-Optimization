package org.example;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataLoader {

    public static Map<String, Node> loadNodes(Connection conn) throws SQLException {
        Map<String, Node> nodes = new HashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Nodes")) {
            while (rs.next()) {
                String id = rs.getString("NodeID");
                String name = rs.getString("Name");
                Integer population = rs.getObject("Population") != null ? rs.getInt("Population") : null;
                String type = rs.getString("NodeType");
                double x = rs.getDouble("X");
                double y = rs.getDouble("Y");
                nodes.put(id, new Node(id, name, population, type, x, y, type));
            }
        }
        return nodes;
    }

    public static Map<String, Node> loadNodesForMap(Connection conn) throws SQLException {
        return loadNodes(conn);
    }

    public static List<Road> loadRoads(Connection conn) throws SQLException {
        List<Road> roads = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Roads")) {
            while (rs.next()) {
                String from = rs.getString("FromID");
                String to = rs.getString("ToID");
                Double distance = rs.getObject("Distance") != null ? rs.getDouble("Distance") : null;
                Integer capacity = rs.getObject("Capacity") != null ? rs.getInt("Capacity") : null;
                Integer condition = rs.getObject("RoadCondition") != null ? rs.getInt("RoadCondition") : null;
                boolean existing = rs.getBoolean("IsExisting");
                Double cost = rs.getObject("ConstructionCost") != null ? rs.getDouble("ConstructionCost") : null;
                roads.add(new Road(from, to, distance, capacity, condition, existing, cost));
            }
        }
        return roads;
    }

    public static List<Road> loadRoadsForMap(Connection conn) throws SQLException {
        return loadRoads(conn); // Reuse loadRoads to fetch from database
    }

    public static Map<String, Map<String, Integer>> loadTrafficFlow(Connection conn) throws SQLException {
        Map<String, Map<String, Integer>> trafficFlow = new HashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM TrafficFlow")) {
            while (rs.next()) {
                String from = rs.getString("FromID");
                String to = rs.getString("ToID");
                String key = from + "-" + to;
                Map<String, Integer> flow = new HashMap<>();
                flow.put("MorningPeak", rs.getInt("MorningPeak"));
                flow.put("Afternoon", rs.getInt("Afternoon"));
                flow.put("EveningPeak", rs.getInt("EveningPeak"));
                flow.put("Night", rs.getInt("Night"));
                trafficFlow.put(key, flow);
            }
        }
        return trafficFlow;
    }
}