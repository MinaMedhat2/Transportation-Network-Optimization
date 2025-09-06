package org.example;

import java.util.*;

public class Dijkstra {

    public static class PathResult {
        public double distance;
        public double time;
        public List<String> path;

        public PathResult(double distance, double time, List<String> path) {
            this.distance = distance;
            this.time = time;
            this.path = path;
        }
    }

    public static class Edge {
        String to;
        double distance;
        int capacity;

        public Edge(String to, double distance, int capacity) {
            this.to = to;
            this.distance = distance;
            this.capacity = capacity;
        }
    }

    public static PathResult findShortestPath(Map<String, List<Edge>> graph, String start, String end,
                                              String timeOfDay, Map<String, Map<String, Integer>> trafficFlow,
                                           List<Road> closedRoads) {
        Map<String, Double> times = new HashMap<>();//اقل وقت
        Map<String, Double> distances = new HashMap<>();//اقل مسافه
        Map<String, String> prev = new HashMap<>();// برجع الطريق
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(times::get));//بختار النقطه اللي الوقت بتعها اقل اولويه
        Set<String> visited = new HashSet<>();//علشان مرجعش ل نفس النقطه مرتين

        for (String node : graph.keySet()) {
            times.put(node, Double.POSITIVE_INFINITY);
            distances.put(node, Double.POSITIVE_INFINITY);
        }
        times.put(start, 0.0);
        distances.put(start, 0.0);
        pq.add(start);

        while (!pq.isEmpty()) {
            String current = pq.poll();
            if (visited.contains(current)) continue;
            visited.add(current);

            if (current.equals(end)) break;

            for (Edge edge : graph.getOrDefault(current, new ArrayList<>())) {
                Road road = closedRoads.stream()
                        .filter(r -> r.fromId.equals(current) && r.toId.equals(edge.to))
                        .findFirst()
                        .orElse(null);
                if (road != null) continue;

                String key = current + "-" + edge.to;
                Map<String, Integer> traffic = trafficFlow.get(key);
                if (traffic == null) {
                    key = edge.to + "-" + current;
                    traffic = trafficFlow.get(key);
                }
                if (traffic == null) continue;

                double travelTime = calculateTravelTime(edge.distance, edge.capacity, timeOfDay, traffic);
                double newTime = times.get(current) + travelTime;
                double newDist = distances.get(current) + edge.distance;

                if (newTime < times.getOrDefault(edge.to, Double.POSITIVE_INFINITY)) {
                    times.put(edge.to, newTime);
                    distances.put(edge.to, newDist);
                    prev.put(edge.to, current);
                    pq.add(edge.to);
                }
            }
        }

        List<String> path = new ArrayList<>();
        String step = end;
        while (step != null) {
            path.add(step);
            step = prev.get(step);
        }
        Collections.reverse(path);

        double totalTime = times.getOrDefault(end, Double.POSITIVE_INFINITY);
        double totalDistance = distances.getOrDefault(end, Double.POSITIVE_INFINITY);
        return new PathResult(totalDistance, totalTime, path);
    }

    static double calculateTravelTime(double distance, int capacity, String timeOfDay, Map<String, Integer> traffic) {
        double baseTime = distance / 60.0 * 60;
        double congestionFactor = calculateCongestion(capacity, timeOfDay, traffic);
        return baseTime * (1 + congestionFactor);
    }

    private static double calculateCongestion(int capacity, String timeOfDay, Map<String, Integer> traffic) {
        double trafficVolume;
        switch (timeOfDay) {
            case "Morning Peak":
                trafficVolume = traffic.get("MorningPeak");
                break;
            case "Afternoon":
                trafficVolume = traffic.get("Afternoon");
                break;
            case "Evening Peak":
                trafficVolume = traffic.get("EveningPeak");
                break;
            default:
                trafficVolume = traffic.get("Night");
                break;
        }
        double congestion = capacity > 0 ? trafficVolume / capacity : 1.0;
        return Math.min(congestion, 1.0);
    }


}