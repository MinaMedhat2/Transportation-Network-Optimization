# Project Title: Smart City Transportation Network Optimization

This project presents a transportation management and optimization system designed for the Greater Cairo metropolitan area.  
It integrates various algorithmic techniques to solve real-world urban traffic challenges using advanced data structures and intelligent algorithms.

---

## Technologies and Tools Used

- **Java Swing**: For building the interactive Graphical User Interface (GUI).
- **MySQL Database**: Used for storing and retrieving transportation network data such as nodes (locations), roads, traffic flow, and facilities.
- **Data Structures**: Weighted graph representation and custom structures for temporal traffic data and simulation.

---

## Algorithms Implemented

### 1. Minimum Spanning Tree Algorithms
- Kruskal’s Algorithm
- Prim’s Algorithm

### 2. Shortest Path Algorithms
- Dijkstra’s Algorithm
- A* Search Algorithm
- Modified Shortest Path Algorithm for time-dependent traffic patterns

### 3. Dynamic Programming Algorithms
- Optimal public transport scheduling
- Resource allocation for road maintenance
- Memoization-enhanced path planning

### 4. Greedy Algorithms
- Real-time traffic signal optimization
- Emergency vehicle preemption system at congested intersections

---

## System Navigation & Features

Upon launching the application, the user is presented with the following main options:

1. **Network Design**  
   Opens a map interface where users can visualize and optimize the road network using Minimum Spanning Tree algorithms.  
   Helps design cost-effective connections between high-demand areas and critical facilities.

2. **Traffic Simulation**  
   Displays a dynamic simulation map that demonstrates traffic flow across the city.  
   Integrates shortest path algorithms with time-dependent traffic patterns and congestion scenarios.

3. **Emergency Response Planning**  
   Provides a map-based interface for routing emergency vehicles using A* and priority-based intersection handling.  
   Aims to minimize emergency response times.

4. **Public Transit Optimization**  
   Manages metro and bus schedules using dynamic programming to improve public transportation efficiency.

5. **Information About App**  
   Displays general details about the app, its objectives, features, and the algorithms used.

6. **Exit**  
   Safely closes the application.

---

## System Design Summary

Each module is implemented with a focus on **visual interactivity** and **algorithmic functionality**.  
The GUI is built using **Java Swing**, and operations are supported by a **MySQL database**, ensuring real-world accuracy.

The map-based interface enables users to:
- Visualize the transportation network
- Apply algorithms interactively
- Receive real-time feedback and analytical results
