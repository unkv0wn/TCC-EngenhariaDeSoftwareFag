package com.routewise.service.osm;

import com.routewise.algorithm.HaversineUtil;
import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.access.OsmInputException;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.pbf.seq.PbfReader;
import de.topobyte.osm4j.xml.dynsax.OsmXmlReader;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OSM Graph Service — loads and parses OpenStreetMap data using osm4j.
 *
 * <h2>Role in the System</h2>
 * This service provides a local road-network graph built from OSM data.
 * It is used to:
 * <ol>
 *   <li>Snap user-placed waypoints to the nearest OSM road node.</li>
 *   <li>Expose graph statistics (number of nodes, edges) for the TCC UI.</li>
 *   <li>Serve as a fallback data source when OSRM is unavailable (future).</li>
 * </ol>
 *
 * <h2>Configuration</h2>
 * Configure {@code routewise.osm.data-path} in {@code application.properties}.
 * If left empty, the service initialises in disabled mode and no graph is loaded.
 * Supported file formats: {@code .osm} (XML) and {@code .pbf} (Protocol Buffers).
 *
 * <h2>Memory</h2>
 * Only highway nodes (nodes referenced by at least one OSM way tagged
 * {@code highway=*}) are stored. This keeps memory usage bounded.
 */
@Service
public class OsmGraphService {

  private static final Logger log = LoggerFactory.getLogger(OsmGraphService.class);

  @Value("${routewise.osm.data-path:}")
  private String osmDataPath;

  // ── In-memory graph ───────────────────────────────────────────────────────

  /** All OSM nodes on or adjacent to roads: nodeId → NodeData */
  private final Map<Long, NodeData> nodes = new HashMap<>();

  /** Adjacency list: nodeId → list of (neighbourId, distanceMetres) */
  private final Map<Long, List<Edge>> adjacencyList = new HashMap<>();

  private boolean loaded = false;
  private int     edgeCount = 0;

  // ─────────────────────────────────────────────────────────────────────────
  // Lifecycle
  // ─────────────────────────────────────────────────────────────────────────

  /** Loads the OSM graph at application startup (optional feature). */
  @PostConstruct
  public void loadGraph() {
    if (osmDataPath == null || osmDataPath.isBlank()) {
      log.info("OSM graph service is DISABLED (routewise.osm.data-path not configured).");
      return;
    }

    File file = new File(osmDataPath);
    if (!file.exists() || !file.isFile()) {
      log.warn("OSM data file not found at '{}'; graph service is DISABLED.", osmDataPath);
      return;
    }

    log.info("Loading OSM graph from '{}' …", osmDataPath);
    long start = System.currentTimeMillis();

    try {
      parseOsmFile(file);
      loaded = true;
      long elapsed = System.currentTimeMillis() - start;
      log.info(
        "OSM graph loaded in {}ms — nodes={}, edges={}, file={}",
        elapsed, nodes.size(), edgeCount, file.getName()
      );
    } catch (IOException | OsmInputException ex) {
      log.error("Failed to load OSM graph from '{}': {}", osmDataPath, ex.getMessage(), ex);
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Public API
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Finds the nearest road-network node to the given coordinates.
   *
   * @param lat target latitude
   * @param lng target longitude
   * @return nearest {@link NodeData}, or empty if the graph is not loaded
   */
  public Optional<NodeData> findNearestNode(double lat, double lng) {
    if (!loaded || nodes.isEmpty()) {
      return Optional.empty();
    }

    NodeData nearest = null;
    double   minDist = Double.MAX_VALUE;

    for (NodeData node : nodes.values()) {
      double dist = HaversineUtil.distanceKm(lat, lng, node.lat(), node.lng());
      if (dist < minDist) {
        minDist = dist;
        nearest = node;
      }
    }

    return Optional.ofNullable(nearest);
  }

  /**
   * Returns basic statistics about the loaded graph.
   *
   * @return {@link GraphStats} with counts and enabled flag
   */
  public GraphStats getStats() {
    return new GraphStats(loaded, nodes.size(), edgeCount, osmDataPath);
  }

  /** @return {@code true} if the OSM graph was successfully loaded */
  public boolean isLoaded() {
    return loaded;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // OSM parsing (osm4j)
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Two-pass parsing strategy:
   * <ol>
   *   <li>Pass 1 — collect all node IDs referenced by highway ways.</li>
   *   <li>Pass 2 — load lat/lng for those node IDs and build the graph.</li>
   * </ol>
   * A single-pass approach is simpler but loads ALL nodes into memory.
   * For a TCC, the single-pass approach is acceptable on regional extracts.
   */
  private void parseOsmFile(File file) throws IOException, OsmInputException {
    // ── Temporary structures for two-phase parsing ────────────────────────
    Set<Long>              highwayNodeIds = new HashSet<>();
    List<long[]>           wayNodeSequences = new ArrayList<>();
    Map<Long, double[]>    allNodes = new HashMap<>();

    // ── Single-pass handler collecting both nodes and ways ────────────────
    OsmHandler handler = new OsmHandler() {

      @Override
      public void handle(OsmBounds bounds) throws IOException {
        // Not used
      }

      @Override
      public void handle(OsmNode node) throws IOException {
        allNodes.put(node.getId(), new double[]{node.getLatitude(), node.getLongitude()});
      }

      @Override
      public void handle(OsmWay way) throws IOException {
        if (!isHighway(way)) return;

        int    numNodes = way.getNumberOfNodes();
        long[] nodeIds  = new long[numNodes];
        for (int i = 0; i < numNodes; i++) {
          nodeIds[i] = way.getNodeId(i);
          highwayNodeIds.add(nodeIds[i]);
        }
        wayNodeSequences.add(nodeIds);
      }

      @Override
      public void handle(OsmRelation relation) throws IOException {
        // Not used in phase 1
      }

      @Override
      public void complete() throws IOException {
        // Processing done after all entities are read
      }
    };

    // ── Choose parser based on file extension ────────────────────────────
    try (InputStream stream = new FileInputStream(file)) {
      if (file.getName().endsWith(".pbf")) {
        PbfReader reader = new PbfReader(stream, false);
        reader.setHandler(handler);
        reader.read();
      } else {
        // Default: OSM XML (.osm)
        OsmXmlReader reader = new OsmXmlReader(stream, false);
        reader.setHandler(handler);
        reader.read();
      }
    }

    // ── Build graph from collected data ───────────────────────────────────
    for (long nodeId : highwayNodeIds) {
      double[] coords = allNodes.get(nodeId);
      if (coords != null) {
        nodes.put(nodeId, new NodeData(nodeId, coords[0], coords[1]));
        adjacencyList.put(nodeId, new ArrayList<>());
      }
    }

    AtomicInteger edges = new AtomicInteger(0);
    for (long[] sequence : wayNodeSequences) {
      for (int i = 0; i < sequence.length - 1; i++) {
        long fromId = sequence[i];
        long toId   = sequence[i + 1];

        NodeData from = nodes.get(fromId);
        NodeData to   = nodes.get(toId);
        if (from == null || to == null) continue;

        double dist = HaversineUtil.toMetres(
          HaversineUtil.distanceKm(from.lat(), from.lng(), to.lat(), to.lng())
        );

        // Bidirectional edge (most OSM roads are two-way)
        adjacencyList.computeIfAbsent(fromId, k -> new ArrayList<>())
          .add(new Edge(toId, dist));
        adjacencyList.computeIfAbsent(toId, k -> new ArrayList<>())
          .add(new Edge(fromId, dist));

        edges.addAndGet(2);
      }
    }
    edgeCount = edges.get();
  }

  /** Returns {@code true} if the given OSM way has a {@code highway} tag. */
  private boolean isHighway(OsmWay way) {
    for (int i = 0; i < way.getNumberOfTags(); i++) {
      if ("highway".equals(way.getTag(i).getKey())) {
        return true;
      }
    }
    return false;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Nested types (graph data model)
  // ─────────────────────────────────────────────────────────────────────────

  /** An OSM road-network node with its geographic coordinates. */
  public record NodeData(long id, double lat, double lng) {}

  /** A directed edge from one node to another with a distance in metres. */
  public record Edge(long targetId, double distanceMetres) {}

  /** Diagnostic statistics for the loaded graph. */
  public record GraphStats(boolean enabled, int nodeCount, int edgeCount, String dataPath) {}
}
