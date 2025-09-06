package org.example;

import java.util.List;

public class TransitLine {
    private String lineId;
    private String lineName;
    private List<String> stops;
    private String lineType;
    private int dailyPassengers;
    private List<Double> schedule;
    private int vehicleCount;
    private double avgWaitTime;

    public TransitLine(String lineId, String lineName, List<String> stops, String lineType, int dailyPassengers,
                       List<Double> schedule, int vehicleCount, double avgWaitTime) {
        this.lineId = lineId;
        this.lineName = lineName;
        this.stops = stops;
        this.lineType = lineType;
        this.dailyPassengers = dailyPassengers;
        this.schedule = schedule;
        this.vehicleCount = vehicleCount;
        this.avgWaitTime = avgWaitTime;
    }

    public String getLineId() {
        return lineId;
    }

    public String getLineName() {
        return lineName;
    }

    public List<String> getStops() {
        return stops;
    }

    public String getLineType() {
        return lineType;
    }

    public int getDailyPassengers() {
        return dailyPassengers;
    }

    public List<Double> getSchedule() {
        return schedule;
    }

    public int getVehicleCount() {
        return vehicleCount;
    }

    public double getAvgWaitTime() {
        return avgWaitTime;
    }
}