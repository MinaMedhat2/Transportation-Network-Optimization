package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Road {
    public String fromId;
    public String toId;
    public Double distance;
    public Integer capacity;
    public Integer condition;
    public boolean isExisting;
    public Double cost;
    public String fromID;
    public String toID;
    public double intersectionDelay;
    public Road(String fromId, String toId, Double distance, Integer capacity, Integer condition,
                boolean isExisting, Double cost ) {
        this.fromId = fromId;
        this.toId = toId;
        this.distance = distance;
        this.capacity = capacity;
        this.condition = condition;
        this.isExisting = isExisting;
        this.cost = cost;

    }

    public String getFromId() { return fromId; }
    public String getToId() { return toId; }
    public double getDistance() { return distance != null ? distance : 0.0; }
    public int getCapacity() { return capacity != null ? capacity : 0; }
    public Integer getCondition() { return condition; }
    public boolean isExisting() { return isExisting; }
    public double getCost() { return cost != null ? cost : 0.0; }


}
