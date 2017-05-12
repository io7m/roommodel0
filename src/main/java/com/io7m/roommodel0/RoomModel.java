package com.io7m.roommodel0;

import com.io7m.jaffirm.core.Invariants;
import com.io7m.jfunctional.Pair;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jregions.core.unparameterized.areas.AreaL;
import com.io7m.jregions.core.unparameterized.areas.AreasL;
import com.io7m.jspatial.api.quadtrees.QuadTreeConfigurationL;
import com.io7m.jspatial.api.quadtrees.QuadTreeLType;
import com.io7m.jspatial.api.quadtrees.QuadTreeReadableLType;
import com.io7m.jspatial.implementation.QuadTreeL;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2D;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jgrapht.graph.SimpleGraph;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.io7m.jfunctional.Unit.unit;

public final class RoomModel implements RoomModelType
{
  private final SimpleGraph<Vertex, Edge> vertex_connectivity;
  private final Reference2ReferenceOpenHashMap<Polygon, Unit> polygons;
  private final Set<RoomPolygonType> polygons_view;
  private final QuadTreeLType<Polygon> polygons_tree;
  private final AreaL bounds;
  private long vertex_ids;
  private long poly_ids;

  @SuppressWarnings("unchecked")
  private static <A, B> QuadTreeReadableLType<B> cast(
    final QuadTreeReadableLType<A> q)
  {
    return (QuadTreeReadableLType<B>) q;
  }

  public static RoomModelType create(
    final AreaL bounds)
  {
    return new RoomModel(bounds);
  }

  private RoomModel(
    final AreaL in_bounds)
  {
    this.bounds = NullCheck.notNull(in_bounds, "Bounds");

    this.vertex_connectivity =
      new SimpleGraph<>(Edge::new);
    this.polygons =
      new Reference2ReferenceOpenHashMap<>(32);
    this.polygons_view =
      Collections.unmodifiableSet(this.polygons.keySet());
    this.polygons_tree =
      QuadTreeL.create(
        QuadTreeConfigurationL.of(
          this.bounds, 16L, 16L, true));

    this.vertex_ids = 0L;
    this.poly_ids = 0L;
  }

  private static List<Pair<Vertex, Vertex>> pairVertices(
    final List<RoomPolyVertexType> vertices)
  {
    final ReferenceArrayList<Pair<Vertex, Vertex>> edges =
      new ReferenceArrayList<>(vertices.size());
    for (int index = 0; index < vertices.size(); ++index) {
      final Vertex v0 = (Vertex) vertices.get(index);
      final int index_next;
      if (index + 1 < vertices.size()) {
        index_next = index + 1;
      } else {
        index_next = 0;
      }
      final Vertex v1 = (Vertex) vertices.get(index_next);
      edges.add(Pair.pair(v0, v1));
    }
    return edges;
  }

  private static void checkVerticesAreConvex(
    final List<RoomPolyVertexType> vs)
  {
    final ReferenceArrayList<Vector2I> positions =
      new ReferenceArrayList<>(vs.size());
    for (int index = 0; index < vs.size(); ++index) {
      positions.add(vs.get(index).position());
    }

    if (!RoomPolygons.isConvex(positions)) {
      throw new RoomModelExceptionPolygonNotConvex(
        "Polygon is not convex");
    }
  }

  @Override
  public RoomPolyVertexType vertexCreate(
    final Vector2I position)
  {
    NullCheck.notNull(position, "Position");

    final Vertex v = new Vertex(this.vertex_ids, position);
    this.vertex_ids = Math.addExact(this.vertex_ids, 1L);
    this.vertex_connectivity.addVertex(v);
    return v;
  }

  @Override
  public RoomPolygonType polygonCreate(
    final List<RoomPolyVertexType> poly_vertices)
  {
    NullCheck.notNull(poly_vertices, "Vertices");

    if (poly_vertices.size() < 3) {
      throw new RoomModelExceptionPolygonTooFewVertices(
        "Polygon must have at least three vertices");
    }

    this.checkVerticesExist(poly_vertices);
    checkVerticesAreConvex(poly_vertices);

    final List<Pair<Vertex, Vertex>> edge_pairs = pairVertices(poly_vertices);
    final ReferenceArrayList<Edge> edges = new ReferenceArrayList<>(
      poly_vertices.size());
    final ReferenceOpenHashSet<Polygon> connected_polygons = new ReferenceOpenHashSet<>();
    for (int index = 0; index < poly_vertices.size(); ++index) {
      final Vertex v0 = edge_pairs.get(index).getLeft();
      final Vertex v1 = edge_pairs.get(index).getRight();
      final Edge edge;
      if (this.vertex_connectivity.containsEdge(v0, v1)) {
        edge = this.vertex_connectivity.getEdge(v0, v1);
        connected_polygons.addAll(v0.polygons);
        connected_polygons.addAll(v1.polygons);
      } else {
        edge = new Edge(v0, v1);
      }
      edges.add(edge);
    }

    final AreaL poly_bounds =
      RoomPolygons.bounds(poly_vertices.stream()
                            .map(RoomPolyVertexType::position)
                            .collect(Collectors.toList()));

    if (!AreasL.contains(this.bounds, poly_bounds)) {
      throw new RoomModelExceptionPolygonOutsideBounds(
        "Polygon cannot fit into the room");
    }

    final Polygon poly = new Polygon(this.poly_ids, poly_bounds);
    final boolean inserted = this.polygons_tree.insert(poly, poly_bounds);
    Invariants.checkInvariant(inserted, "Polygon must have been inserted");

    this.poly_ids = Math.addExact(this.poly_ids, 1L);

    for (int index = 0; index < poly_vertices.size(); ++index) {
      poly.vertices.add((Vertex) poly_vertices.get(index));
    }

    for (int index = 0; index < edges.size(); ++index) {
      final Edge edge = edges.get(index);
      this.vertex_connectivity.addEdge(edge.vertex0, edge.vertex1, edge);
      edge.vertex0.polygons.add(poly);
      edge.vertex1.polygons.add(poly);
      edge.polygons.add(poly);
      poly.edges.add(edge);
    }

    this.polygons.put(poly, unit());
    return poly;
  }

  @Override
  public void polygonDelete(
    final RoomPolygonType p)
  {
    NullCheck.notNull(p, "Polygon");

    final Polygon poly = this.checkPolygonExists(p);

    final ReferenceOpenHashSet<Vertex> vertices_delete =
      new ReferenceOpenHashSet<>();
    for (final Vertex v : poly.vertices) {
      if (v.polygons.size() == 1) {
        Invariants.checkInvariant(
          v.polygons.contains(poly),
          "Vertex must reference this polygon");
        vertices_delete.add(v);
      }
      v.polygons.remove(poly);
    }

    for (int index = 0; index < poly.edges.size(); ++index) {
      final Edge e = poly.edges.get(index);
      e.polygons.remove(poly);
    }

    for (final Vertex v : vertices_delete) {
      this.vertex_connectivity.removeVertex(v);
    }

    this.polygons_tree.remove(poly);
    this.polygons.remove(poly);
  }

  private Polygon checkPolygonExists(
    final RoomPolygonType polygon)
  {
    if (polygon instanceof Polygon) {
      if (this.polygons.containsKey(polygon)) {
        return (Polygon) polygon;
      }
    }

    throw new RoomModelExceptionPolygonNonexistent(
      String.format("Polygon %s does not belong to this model", polygon));
  }

  private void checkVerticesExist(
    final List<RoomPolyVertexType> vertices)
  {
    for (int index = 0; index < vertices.size(); ++index) {
      this.checkVertexExists(vertices.get(index));
    }
  }

  private void checkVertexExists(
    final RoomPolyVertexType v)
  {
    if (v instanceof Vertex) {
      if (this.vertex_connectivity.containsVertex((Vertex) v)) {
        return;
      }
    }

    throw new RoomModelExceptionVertexNonexistent(
      String.format("Vertex %s does not belong to this model", v));
  }

  @Override
  public Set<RoomPolygonType> polygons()
  {
    return this.polygons_view;
  }

  @Override
  public QuadTreeReadableLType<RoomPolygonType> polygonTree()
  {
    return cast(this.polygons_tree);
  }

  @Override
  public Optional<RoomPolyVertexType> vertexFind(
    final Vector2I position)
  {
    final ReferenceOpenHashSet<Polygon> results =
      new ReferenceOpenHashSet<>();
    final AreaL area = AreasL.create(
      (long) (position.x() - 1),
      (long) (position.y() - 1),
      2L,
      2L);
    this.polygons_tree.overlappedBy(area, results);

    for (final Polygon poly : results) {
      for (final Vertex v : poly.vertices) {
        if (Objects.equals(v.position, position)) {
          return Optional.of(v);
        }
      }
    }

    return Optional.empty();
  }

  private static final class Polygon implements RoomPolygonType
  {
    private final ReferenceArrayList<Edge> edges;
    private final List<RoomPolyEdgeType> edges_view;
    private final ReferenceArrayList<Vertex> vertices;
    private final List<RoomPolyVertexType> vertices_view;
    private final long id;
    private final AreaL bounds;

    @Override
    public String toString()
    {
      final StringBuilder sb = new StringBuilder("[Polygon ");
      sb.append(this.id);
      sb.append("]");
      return sb.toString();
    }

    Polygon(
      final long in_id,
      final AreaL in_bounds)
    {
      this.id = in_id;
      this.bounds = NullCheck.notNull(in_bounds, "Bounds");
      this.vertices = new ReferenceArrayList<>();
      this.vertices_view = Collections.unmodifiableList(this.vertices);
      this.edges = new ReferenceArrayList<>();
      this.edges_view = Collections.unmodifiableList(this.edges);
    }

    @Override
    public long id()
    {
      return this.id;
    }

    @Override
    public AreaL bounds()
    {
      return this.bounds;
    }

    @Override
    public List<RoomPolyEdgeType> edges()
    {
      return this.edges_view;
    }

    @Override
    public List<RoomPolyVertexType> vertices()
    {
      return this.vertices_view;
    }
  }

  private static final class Edge implements RoomPolyEdgeType
  {
    private final Vertex vertex0;
    private final Vertex vertex1;
    private final ReferenceOpenHashSet<Polygon> polygons;
    private final Set<RoomPolygonType> polygons_view;

    Edge(
      final Vertex in_vertex0,
      final Vertex in_vertex1)
    {
      this.vertex0 = NullCheck.notNull(in_vertex0, "Vertex 0");
      this.vertex1 = NullCheck.notNull(in_vertex1, "Vertex 1");
      this.polygons = new ReferenceOpenHashSet<>();
      this.polygons_view = Collections.unmodifiableSet(this.polygons);
    }

    @Override
    public boolean equals(
      final Object o)
    {
      if (this == o) {
        return true;
      }
      if (o == null || this.getClass() != o.getClass()) {
        return false;
      }

      final Edge edge = (Edge) o;
      return Objects.equals(this.vertex0, edge.vertex0)
        && Objects.equals(this.vertex1, edge.vertex1);
    }

    @Override
    public int hashCode()
    {
      int result = this.vertex0.hashCode();
      result = 31 * result + this.vertex1.hashCode();
      return result;
    }

    @Override
    public Set<RoomPolygonType> polygons()
    {
      return this.polygons_view;
    }

    @Override
    public RoomPolyVertexType vertex0()
    {
      return this.vertex0;
    }

    @Override
    public RoomPolyVertexType vertex1()
    {
      return this.vertex1;
    }

    @Override
    public boolean isExternal()
    {
      return this.polygons.size() < 2;
    }

    @Override
    public Vector2D normal()
    {
      return RoomPolygons.normal(
        this.vertex0.position,
        this.vertex1.position);
    }
  }

  @Override
  public List<String> check()
  {
    final ReferenceArrayList<String> errors = new ReferenceArrayList<>();

    for (final Polygon p : this.polygons.keySet()) {
      for (final Vertex v : p.vertices) {
        for (final Polygon q : v.polygons) {
          if (!this.polygons.containsKey(q)) {
            final StringBuilder sb = new StringBuilder(128);
            sb.append("Polygon ");
            sb.append(p);
            sb.append(": Vertex ");
            sb.append(v);
            sb.append(" references nonexistent polygon ");
            sb.append(q);
            errors.add(sb.toString());
          }
        }

        if (!this.vertex_connectivity.containsVertex(v)) {
          final StringBuilder sb = new StringBuilder(128);
          sb.append("Polygon ");
          sb.append(p);
          sb.append(": Nonexistent vertex ");
          sb.append(v);
          errors.add(sb.toString());
        }
        final long vx = (long) v.position.x();
        final long vy = (long) v.position.y();
        if (!AreasL.containsPoint(this.bounds, vx, vy)) {
          final StringBuilder sb = new StringBuilder(128);
          sb.append("Polygon ");
          sb.append(p);
          sb.append(": Vertex ");
          sb.append(v);
          sb.append(" has out-of-bounds position (");
          sb.append(vx);
          sb.append(", ");
          sb.append(vy);
          sb.append(")");
          errors.add(sb.toString());
        }
      }

      for (final Edge e : p.edges) {
        if (!this.vertex_connectivity.containsVertex(e.vertex0)) {
          final StringBuilder sb = new StringBuilder(128);
          sb.append("Polygon ");
          sb.append(p);
          sb.append(": Edge references nonexistent vertex ");
          sb.append(e.vertex0);
          errors.add(sb.toString());
        }
        if (!this.vertex_connectivity.containsVertex(e.vertex1)) {
          final StringBuilder sb = new StringBuilder(128);
          sb.append("Polygon ");
          sb.append(p);
          sb.append(": Edge references nonexistent vertex ");
          sb.append(e.vertex1);
          errors.add(sb.toString());
        }

        for (final Polygon q : e.polygons) {
          if (!this.polygons.containsKey(q)) {
            final StringBuilder sb = new StringBuilder(128);
            sb.append("Polygon ");
            sb.append(p);
            sb.append(": Edge references nonexistent polygon ");
            sb.append(q);
            errors.add(sb.toString());
          }
        }
      }
    }

    for (final Vertex v : this.vertex_connectivity.vertexSet()) {
      boolean referenced = false;
      for (final Polygon p : this.polygons.keySet()) {
        referenced = referenced || p.vertices.contains(v);
      }

      if (!referenced) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Vertex ");
        sb.append(v);
        sb.append(": Vertex is not referenced by any polygon ");
        errors.add(sb.toString());
      }
    }

    return errors;
  }

  private static final class Vertex implements RoomPolyVertexType
  {
    private final Vector2I position;
    private final ReferenceOpenHashSet<Polygon> polygons;
    private final Set<RoomPolygonType> polygons_view;
    private final long id;

    private Vertex(
      final long in_id,
      final Vector2I in_position)
    {
      this.position = NullCheck.notNull(in_position, "Position");
      this.polygons = new ReferenceOpenHashSet<>(4);
      this.polygons_view = Collections.unmodifiableSet(this.polygons);
      this.id = in_id;
    }

    @Override
    public String toString()
    {
      final StringBuilder sb = new StringBuilder("[Vertex ");
      sb.append(this.id);
      sb.append("]");
      return sb.toString();
    }

    @Override
    public long id()
    {
      return this.id;
    }

    @Override
    public Set<RoomPolygonType> polygons()
    {
      return this.polygons_view;
    }

    @Override
    public Vector2I position()
    {
      return this.position;
    }
  }
}
