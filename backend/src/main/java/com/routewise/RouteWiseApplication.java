package com.routewise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RouteWise — Urban Route Optimizer
 *
 * <p>Academic full-stack application (TCC) that computes optimal multi-waypoint
 * routes using:
 * <ul>
 *   <li>A* algorithm for waypoint ordering (minimizes OSRM travel duration)</li>
 *   <li>OSRM Open Source Routing Machine for real road costs and geometry</li>
 *   <li>osm4j for local OpenStreetMap data parsing</li>
 *   <li>Java 21 Virtual Threads for concurrent request handling</li>
 *   <li>SSE (Server-Sent Events) for real-time computation progress</li>
 * </ul>
 */
@SpringBootApplication
public class RouteWiseApplication {

  public static void main(String[] args) {
    SpringApplication.run(RouteWiseApplication.class, args);
  }
}
