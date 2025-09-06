package org.example;

import java.util.*;

public class PublicTransitOptimizer {
    private Map<String, Node> nodes;
    private List<Road> roads;
    private Map<String, Map<String, Integer>> trafficFlow;
    private Map<String, List<String>> transitLines;

    public PublicTransitOptimizer(Map<String, Node> nodes, List<Road> roads, Map<String, Map<String, Integer>> trafficFlow, Map<String, List<String>> transitLines) {
        this.nodes = nodes;
        this.roads = roads;
        this.trafficFlow = trafficFlow;
        this.transitLines = transitLines;
    }

    public TransitSchedule optimizeSchedule(String lineName, String lineType, List<String> stops, String timeOfDay, int availableVehicles, int dailyPassengers) {
        int n = stops.size();
        if (n < 2) {
            throw new IllegalArgumentException("The transit line must have at least 2 stops");
        }

        // Dynamic programming table: dp[i][v] = minimum travel time to reach stop i using v vehicles
        double[][] dp = new double[n][availableVehicles + 1];
        int[][] vehicleAssignment = new int[n][availableVehicles + 1];

        // Initialize with infinity, except dp[0][0] = 0
        for (double[] row : dp) Arrays.fill(row, Double.MAX_VALUE);
        dp[0][0] = 0;

        // Calculate travel times with realistic optimization
        List<Double> segmentTimes = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            String from = stops.get(i - 1);
            String to = stops.get(i);
            Road road = getRoad(from, to);
            if (road == null) {
                System.out.println("Warning: No road between " + from + " and " + to + ", using default 10 min.");
                segmentTimes.add(10.0);
            } else {
                String key = from + "-" + to;
                if (!trafficFlow.containsKey(key)) key = to + "-" + from;
                Map<String, Integer> traffic = trafficFlow.get(key);
                double baseTime = road.getDistance() / 50.0 * 60; // 50 km/h
                double congestion = calculateCongestion(road.getCapacity(), timeOfDay, traffic);
                double travelTime = baseTime * (1 + congestion);
                segmentTimes.add(travelTime > 0 ? travelTime : 5.0);
                System.out.println("Segment " + from + " to " + to + ": Base Time = " + baseTime + ", Congestion = " + congestion + ", Total = " + travelTime + " min");
            }
        }

        // Optimize with vehicle distribution
        for (int i = 1; i < n; i++) {
            double travelTime = segmentTimes.get(i - 1);
            for (int v = 1; v <= availableVehicles; v++) {
                for (int prevV = 0; prevV <= v; prevV++) {
                    if (dp[i - 1][prevV] == Double.MAX_VALUE) continue;
                    double newTime = dp[i - 1][prevV] + (travelTime / (1 + (v - prevV) * 0.1)); // 10% time reduction per additional vehicle
                    int vehiclesUsed = v - prevV;
                    if (newTime < dp[i][v]) {
                        dp[i][v] = newTime;
                        vehicleAssignment[i][v] = vehiclesUsed > 0 ? vehiclesUsed : 1;
                        System.out.println("dp[" + i + "][" + v + "] = " + newTime + " with " + vehiclesUsed + " vehicles");
                    }
                }
            }
        }

        // Backtrack
        List<Double> times = new ArrayList<>();
        List<Integer> vehicles = new ArrayList<>();
        double minTime = Double.MAX_VALUE;
        int bestV = 0;
        for (int v = 1; v <= availableVehicles; v++) {
            if (dp[n - 1][v] < minTime && dp[n - 1][v] != Double.MAX_VALUE) {
                minTime = dp[n - 1][v];
                bestV = v;
            }
        }

        if (minTime == Double.MAX_VALUE) {
            throw new IllegalStateException("No feasible schedule found");
        }

        int currentV = bestV;
        double currentTime = 0;
        times.add(0.0);
        for (int i = n - 1; i > 0; i--) {
            int vehiclesUsed = vehicleAssignment[i][currentV];
            vehicles.add(vehiclesUsed);
            currentV -= vehiclesUsed;
            currentTime += segmentTimes.get(i - 1) / (1 + (vehiclesUsed * 0.1));
            times.add(currentTime);
        }
        Collections.reverse(times);
        Collections.reverse(vehicles);

        int totalVehicles = vehicles.stream().mapToInt(Integer::intValue).sum();
        double frequency = totalVehicles > 0 ? 60.0 / totalVehicles : 15.0; // Default 15 min frequency

        return new TransitSchedule(lineName, lineType, stops, times, totalVehicles, frequency);
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
}