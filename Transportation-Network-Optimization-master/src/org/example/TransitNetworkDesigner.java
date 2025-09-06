package org.example;

import java.util.*;

public class TransitNetworkDesigner {
    private Map<String, Node> nodes;
    private List<Road> roads;
    private Map<String, Map<String, Integer>> trafficFlow;
    private Map<String, List<String>> transitLines;
    private Map<String, Integer> dailyPassengers;

    public TransitNetworkDesigner(Map<String, Node> nodes, List<Road> roads, Map<String, Map<String, Integer>> trafficFlow,
                                  Map<String, List<String>> transitLines, Map<String, Integer> dailyPassengers) {
        this.nodes = nodes;
        this.roads = roads;
        this.trafficFlow = trafficFlow;
        this.transitLines = transitLines;
        this.dailyPassengers = dailyPassengers;
    }

    /**
     * تحسين نقاط النقل بناءً على السكان، الاتصال، والازدحام.
     */
    public List<String> optimizeTransferPoints(String timeOfDay) {
        Map<String, Double> transferScores = new HashMap<>();
        for (String nodeId : nodes.keySet()) {
            Node node = nodes.get(nodeId);
            double score = 0;

            // عامل السكان
            if (node.getPopulation() != null) {
                score += node.getPopulation() / 1000000.0; // تطبيع السكان
            }

            // عامل الاتصال (عدد الطرق)
            long roadCount = roads.stream()
                    .filter(r -> r.fromId.equals(nodeId) || r.toId.equals(nodeId))
                    .count();
            score += roadCount * 0.5;

            // عامل خطوط النقل (عدد الخطوط التي تمر بالعقدة)
            long lineCount = transitLines.values().stream()
                    .filter(stops -> stops.contains(nodeId))
                    .count();
            score += lineCount * 0.3;

            // عامل الطلب (عدد الركاب اليومي للخطوط التي تمر بالعقدة)
            double passengerFactor = 0;
            for (Map.Entry<String, List<String>> entry : transitLines.entrySet()) {
                if (entry.getValue().contains(nodeId)) {
                    passengerFactor += dailyPassengers.getOrDefault(entry.getKey(), 0) / 1000000.0;
                }
            }
            score += passengerFactor;

            // عامل الازدحام (متوسط ازدحام الطرق المتصلة)
            double totalCongestion = 0;
            int connectedRoads = 0;
            for (Road road : roads) {
                if (road.fromId.equals(nodeId) || road.toId.equals(nodeId)) {
                    String key = road.fromId + "-" + road.toId;
                    if (!trafficFlow.containsKey(key)) key = road.toId + "-" + road.fromId;
                    Map<String, Integer> traffic = trafficFlow.get(key);
                    if (traffic != null) {
                        totalCongestion += calculateCongestion(road.getCapacity(), timeOfDay, traffic);
                        connectedRoads++;
                    }
                }
            }
            if (connectedRoads > 0) {
                score -= totalCongestion / connectedRoads; // تقليل الدرجة للازدحام العالي
            }

            transferScores.put(nodeId, score);
        }

        // اختيار أعلى 5 عقد كنقاط نقل
        return transferScores.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * بناء شبكة مدمجة عن طريق ربط نقاط النقل.
     */
    public Map<String, List<String>> buildIntegratedNetwork(List<String> transferPoints, String timeOfDay) {
        Map<String, List<String>> network = new HashMap<>();
        Map<String, List<Dijkstra.Edge>> graph = buildGraph(timeOfDay);

        // ربط كل نقطة نقل بالأخرى
        for (String point1 : transferPoints) {
            List<String> connections = new ArrayList<>();
            for (String point2 : transferPoints) {
                if (!point1.equals(point2)) {
                    Dijkstra.PathResult path = Dijkstra.findShortestPath(graph, point1, point2, timeOfDay, trafficFlow, new ArrayList<>());
                    if (!path.path.isEmpty()) {
                        connections.add(point2);
                    }
                }
            }
            network.put(point1, connections);
        }

        return network;
    }

    /**
     * حساب تحسين التغطية بناءً على نقاط النقل.
     */
    public double calculateCoverageImprovement(List<String> transferPoints) {
        Set<String> originalCoveredNodes = new HashSet<>();
        for (List<String> stops : transitLines.values()) {
            originalCoveredNodes.addAll(stops);
        }

        Set<String> newCoveredNodes = new HashSet<>(originalCoveredNodes);
        newCoveredNodes.addAll(transferPoints);
        for (String point : transferPoints) {
            for (Road road : roads) {
                if (road.fromId.equals(point) || road.toId.equals(point)) {
                    newCoveredNodes.add(road.fromId);
                    newCoveredNodes.add(road.toId);
                }
            }
        }

        double originalCoverage = (double) originalCoveredNodes.size() / nodes.size();
        double newCoverage = (double) newCoveredNodes.size() / nodes.size();
        return (newCoverage - originalCoverage) / originalCoverage * 100;
    }

    /**
     * بناء الـ Graph من الداتا بيز.
     */
    public Map<String, List<Dijkstra.Edge>> buildGraph(String timeOfDay) {
        Map<String, List<Dijkstra.Edge>> graph = new HashMap<>();
        for (Road road : roads) {
            if (!road.isExisting()) continue;
            String key = road.fromId + "-" + road.toId;
            if (!trafficFlow.containsKey(key)) key = road.toId + "-" + road.fromId;
            Map<String, Integer> traffic = trafficFlow.get(key);
            double travelTime = calculateTravelTime(road.getDistance(), road.getCapacity(), timeOfDay, traffic);
            Dijkstra.Edge edge = new Dijkstra.Edge(road.toId, travelTime, road.getCapacity());
            graph.computeIfAbsent(road.fromId, k -> new ArrayList<>()).add(edge);
            Dijkstra.Edge reverseEdge = new Dijkstra.Edge(road.fromId, travelTime, road.getCapacity());
            graph.computeIfAbsent(road.toId, k -> new ArrayList<>()).add(reverseEdge);
        }
        return graph;
    }

    private double calculateTravelTime(double distance, int capacity, String timeOfDay, Map<String, Integer> traffic) {
        double baseTime = distance / 60.0 * 60;
        double congestionFactor = traffic != null ? calculateCongestion(capacity, timeOfDay, traffic) : 0;
        return baseTime * (1 + congestionFactor);
    }

    private double calculateCongestion(int capacity, String timeOfDay, Map<String, Integer> traffic) {
        double trafficVolume = getTrafficForTime(traffic, timeOfDay);
        double congestion = capacity > 0 ? trafficVolume / capacity : 1.0;
        return Math.min(congestion, 1.0);
    }

    private double getTrafficForTime(Map<String, Integer> traffic, String timeOfDay) {
        if (traffic == null) return 0;
        switch (timeOfDay) {
            case "Morning Peak": return traffic.getOrDefault("MorningPeak", 0);
            case "Afternoon": return traffic.getOrDefault("Afternoon", 0);
            case "Evening Peak": return traffic.getOrDefault("EveningPeak", 0);
            case "Night": return traffic.getOrDefault("Night", 0);
            default: return 0;
        }
    }
}