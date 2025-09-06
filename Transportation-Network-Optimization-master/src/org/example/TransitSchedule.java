package org.example;

import java.util.List;

public class TransitSchedule {
    private String lineName;
    private String lineType; // "Metro" or "Bus"
    private List<String> stops; // Node IDs
    private List<Double> times; // Departure times at each stop (in minutes)
    private int vehicleCount; // Number of vehicles assigned
    private double frequency; // Frequency in minutes

    public TransitSchedule(String lineName, String lineType, List<String> stops, List<Double> times, int vehicleCount, double frequency) {
        this.lineName = lineName;
        this.lineType = lineType;
        this.stops = stops;
        this.times = times;
        this.vehicleCount = vehicleCount;
        this.frequency = frequency;
    }

    // Getters
    public String getLineName() { return lineName; }
    public String getLineType() { return lineType; }
    public List<String> getStops() { return stops; }
    public List<Double> getTimes() { return times; }
    public int getVehicleCount() { return vehicleCount; }
    public double getFrequency() { return frequency; }

    // Setters
    public void setTimes(List<Double> times) { this.times = times; }
    public void setVehicleCount(int vehicleCount) { this.vehicleCount = vehicleCount; }
    public void setFrequency(double frequency) { this.frequency = frequency; }
}