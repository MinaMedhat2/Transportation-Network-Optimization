package org.example;

public class Node {
    public String id;
    public String name;
    public Integer population;
    public String type;
    public double x;
    public double y;
    private String nodeType;

    // Add nodeType as a constructor parameter
    public Node(String id, String name, Integer population, String type, double x, double y, String nodeType) {
        this.id = id;
        this.name = name;
        this.population = population;
        this.type = type;
        this.x = x;
        this.y = y;
        this.nodeType = nodeType;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Integer getPopulation() { return population; }
    public String getType() { return type; }
    public double getX() { return x; }
    public double getY() { return y; }

    // ✅ Add this getter to fix the error
    public String getNodeType() {
        return nodeType;
    }
}
