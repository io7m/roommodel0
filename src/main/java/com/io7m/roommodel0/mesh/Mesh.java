package com.io7m.roommodel0.mesh;

import com.io7m.jaffirm.core.Invariants;
import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jfunctional.Pair;
import com.io7m.jnull.NullCheck;
import com.io7m.jregions.core.unparameterized.areas.AreaL;
import com.io7m.jregions.core.unparameterized.areas.AreasL;
import com.io7m.jspatial.api.quadtrees.QuadTreeConfigurationL;
import com.io7m.jspatial.api.quadtrees.QuadTreeLType;
import com.io7m.jspatial.api.quadtrees.QuadTreeReadableLType;
import com.io7m.jspatial.implementation.QuadTreeL;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2D;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceCollections;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.jgrapht.graph.SimpleGraph;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class Mesh implements MeshType
{
  private final SimpleGraph<Vertex, Edge> vertex_connectivity;
  private final Long2ReferenceRBTreeMap<Vertex> vertices;
  private final Collection<PolygonVertexType> vertices_view;
  private final Long2ReferenceRBTreeMap<Polygon> polygons;
  private final Collection<PolygonType> polygons_view;
  private final QuadTreeLType<Polygon> polygons_tree;
  private final AreaL bounds;
  private long vertex_ids;
  private long polygon_ids;

  private Mesh(
    final AreaL in_bounds)
  {
    this.bounds = NullCheck.notNull(in_bounds, "Bounds");

    this.vertices =
      new Long2ReferenceRBTreeMap<>();
    this.vertices_view =
      Collections.unmodifiableCollection(this.vertices.values());
    this.vertex_connectivity =
      new SimpleGraph<>(Edge::new);

    this.polygons =
      new Long2ReferenceRBTreeMap<>();
    this.polygons_view =
      castCollection(ReferenceCollections.unmodifiable(this.polygons.values()));
    this.polygons_tree =
      QuadTreeL.create(
        QuadTreeConfigurationL.of(
          this.bounds, 16L, 16L, true));

    this.vertex_ids = 0L;
    this.polygon_ids = 0L;
  }

  @SuppressWarnings("unchecked")
  private static <A, B> QuadTreeReadableLType<B> castQuadTree(
    final QuadTreeReadableLType<A> q)
  {
    return (QuadTreeReadableLType<B>) q;
  }

  @SuppressWarnings("unchecked")
  private static <A, B> Collection<B> castCollection(
    final Collection<A> q)
  {
    return (Collection<B>) q;
  }

  public static MeshType create(
    final AreaL bounds)
  {
    return new Mesh(bounds);
  }

  private static List<Pair<Vertex, Vertex>> pairVertices(
    final ReferenceArrayList<Vertex> vertices)
  {
    final ReferenceArrayList<Pair<Vertex, Vertex>> edges =
      new ReferenceArrayList<>(vertices.size());
    for (int index = 0; index < vertices.size(); ++index) {
      final Vertex v0 = vertices.get(index);
      final int index_next;
      if (index + 1 < vertices.size()) {
        index_next = index + 1;
      } else {
        index_next = 0;
      }
      final Vertex v1 = vertices.get(index_next);
      edges.add(Pair.pair(v0, v1));
    }
    return edges;
  }

  private static void checkVerticesAreConvex(
    final ReferenceArrayList<Vertex> vs)
  {
    final ReferenceArrayList<Vector2I> positions =
      new ReferenceArrayList<>(vs.size());
    for (int index = 0; index < vs.size(); ++index) {
      positions.add(vs.get(index).position());
    }

    if (!MeshPolygons.isConvex(positions)) {
      throw new MeshExceptionPolygonNotConvex("Polygon is not convex");
    }
  }

  @Override
  public PolygonVertexType vertexCreate(
    final Vector2I position)
  {
    NullCheck.notNull(position, "Position");
    return this.vertexCreateWithID(this.vertexIDFresh(), position);
  }

  @Override
  public PolygonVertexType vertexCreateWithID(
    final PolygonVertexID vertex,
    final Vector2I position)
  {
    NullCheck.notNull(vertex, "Vertex");
    NullCheck.notNull(position, "Position");

    if (this.vertices.containsKey(vertex.value())) {
      throw new MeshExceptionVertexDuplicate(
        "Vertex already exists with the given ID");
    }

    final Vertex v = new Vertex(vertex, position);
    this.vertex_connectivity.addVertex(v);
    this.vertices.put(v.id.value(), v);
    return v;
  }

  @Override
  public void vertexSetPosition(
    final PolygonVertexID vertex_id,
    final Vector2I position)
  {
    NullCheck.notNull(vertex_id, "Vertex");
    NullCheck.notNull(position, "Position");

    final Vertex v = this.checkVertexExists(vertex_id);

    final Long2ReferenceOpenHashMap<AreaL> bounds_by_polygon =
      new Long2ReferenceOpenHashMap<>(v.polygons.size());

    for (final Polygon p : v.polygons) {
      final List<Vector2I> pv =
        p.vertices.stream().map(vv -> {
          if (Objects.equals(vv, v)) {
            return position;
          }
          return vv.position;
        }).collect(Collectors.toList());

      if (!MeshPolygons.isConvex(pv)) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Setting vertex position would make polygon non-convex.");
        sb.append(System.lineSeparator());
        sb.append("  Vertex:   ");
        sb.append(vertex_id.value());
        sb.append(System.lineSeparator());
        sb.append("  Position: ");
        sb.append(position);
        sb.append(System.lineSeparator());
        sb.append("  Polygon: ");
        sb.append(p);
        sb.append(System.lineSeparator());
        throw new MeshExceptionPolygonNotConvex(sb.toString());
      }

      final AreaL new_bounds = MeshPolygons.bounds(pv);
      if (!AreasL.contains(this.bounds, new_bounds)) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Setting vertex position would make polygon exceed bounds.");
        sb.append(System.lineSeparator());
        sb.append("  Vertex:   ");
        sb.append(vertex_id.value());
        sb.append(System.lineSeparator());
        sb.append("  Position: ");
        sb.append(position);
        sb.append(System.lineSeparator());
        sb.append("  Polygon: ");
        sb.append(p);
        sb.append(System.lineSeparator());
        throw new MeshExceptionPolygonNotConvex(sb.toString());
      }

      bounds_by_polygon.put(p.id.value(), new_bounds);
    }

    v.position = position;
    for (final Polygon p : v.polygons) {
      this.polygons_tree.remove(p);
      p.bounds = bounds_by_polygon.get(p.id.value());
      final boolean inserted = this.polygons_tree.insert(p, p.bounds);
      Postconditions.checkPostcondition(
        inserted, "Polygon must have been inserted");
    }
  }

  @Override
  public PolygonType polygonCreate(
    final List<PolygonVertexID> poly_vertices)
  {
    return this.polygonCreateWithID(this.polygonIDFresh(), poly_vertices);
  }

  @Override
  public PolygonType polygonCreateWithID(
    final PolygonID id,
    final List<PolygonVertexID> vids)
  {
    NullCheck.notNull(id, "ID");
    NullCheck.notNull(vids, "Vertices");

    if (this.polygons.containsKey(id.value())) {
      throw new MeshExceptionPolygonDuplicate(
        "Polygon already exists with the given ID");
    }

    if (vids.size() < 3) {
      throw new MeshExceptionPolygonTooFewVertices(
        "Polygon must have at least three vertices");
    }

    final ReferenceArrayList<Vertex> poly_vertices =
      this.checkVerticesExist(vids);
    checkVerticesAreConvex(poly_vertices);

    final List<Vector2I> vertex_positions =
      poly_vertices.stream()
        .map(PolygonVertexType::position)
        .collect(Collectors.toList());

    if (MeshPolygons.isClockwiseOrder(vertex_positions)) {
      Collections.reverse(poly_vertices);
    }

    final AreaL poly_bounds =
      MeshPolygons.bounds(vertex_positions);

    if (!AreasL.contains(this.bounds, poly_bounds)) {
      throw new MeshExceptionPolygonOutsideBounds(
        "Polygon cannot fit into the room");
    }

    final List<Pair<Vertex, Vertex>> edge_pairs = pairVertices(
      poly_vertices);
    final ReferenceArrayList<Edge> edges =
      new ReferenceArrayList<>(poly_vertices.size());
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

    final Polygon poly = new Polygon(id, poly_bounds);
    final boolean inserted = this.polygons_tree.insert(poly, poly_bounds);
    Invariants.checkInvariant(inserted, "Polygon must have been inserted");

    for (int index = 0; index < poly_vertices.size(); ++index) {
      poly.vertices.add(poly_vertices.get(index));
    }

    for (int index = 0; index < edges.size(); ++index) {
      final Edge edge = edges.get(index);
      this.vertex_connectivity.addEdge(edge.vertex0, edge.vertex1, edge);
      edge.vertex0.polygons.add(poly);
      edge.vertex1.polygons.add(poly);
      edge.polygons.add(poly);
      poly.edges.add(edge);
    }

    this.polygons.put(poly.id.value(), poly);
    return poly;
  }

  @Override
  public void polygonDelete(
    final PolygonID pid)
  {
    NullCheck.notNull(pid, "Polygon ID");

    final Polygon poly = this.checkPolygonExists(pid);

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
      this.vertices.remove(v.id.value());
      v.deleted = true;
    }

    this.polygons_tree.remove(poly);
    this.polygons.remove(poly.id.value());
    poly.deleted = true;
  }

  private PolygonID polygonIDFresh()
  {
    final PolygonID id = PolygonID.of(this.polygon_ids);

    final long last_key;
    if (!this.polygons.isEmpty()) {
      last_key = this.polygons.lastLongKey();
    } else {
      last_key = Long.MIN_VALUE;
    }

    this.polygon_ids =
      Math.max(
        Math.addExact(last_key, 1L),
        Math.addExact(this.polygon_ids, 1L));

    Postconditions.checkPostcondition(
      !this.polygons.containsKey(id.value()),
      "Polygon ID must be fresh");
    return id;
  }

  private PolygonVertexID vertexIDFresh()
  {
    final PolygonVertexID id = PolygonVertexID.of(this.vertex_ids);

    final long last_key;
    if (!this.vertices.isEmpty()) {
      last_key = this.vertices.lastLongKey();
    } else {
      last_key = Long.MIN_VALUE;
    }

    this.vertex_ids =
      Math.max(
        Math.addExact(last_key, 1L),
        Math.addExact(this.vertex_ids, 1L));

    Postconditions.checkPostcondition(
      !this.vertices.containsKey(id.value()),
      "Vertex ID must be fresh");
    return id;
  }

  private Polygon checkPolygonExists(
    final PolygonID pid)
  {
    if (this.polygons.containsKey(pid.value())) {
      return this.polygons.get(pid.value());
    }

    throw new MeshExceptionPolygonNonexistent(
      String.format("Polygon %s does not exist", Long.valueOf(pid.value())));
  }

  private ReferenceArrayList<Vertex> checkVerticesExist(
    final List<PolygonVertexID> vids)
  {
    final ReferenceArrayList<Vertex> vertices =
      new ReferenceArrayList<>(vids.size());
    for (int index = 0; index < vids.size(); ++index) {
      vertices.add(this.checkVertexExists(vids.get(index)));
    }
    return vertices;
  }

  private Vertex checkVertexExists(
    final PolygonVertexID v)
  {
    if (this.vertices.containsKey(v.value())) {
      return this.vertices.get(v.value());
    }

    throw new MeshExceptionVertexNonexistent(
      String.format("Vertex %s does not exist", v));
  }

  @Override
  public Collection<PolygonType> polygons()
  {
    return this.polygons_view;
  }

  @Override
  public QuadTreeReadableLType<PolygonType> polygonTree()
  {
    return castQuadTree(this.polygons_tree);
  }

  @Override
  public Optional<PolygonVertexType> vertexFind(
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

  @Override
  public Optional<PolygonType> polygonFind(
    final Vector2I position)
  {
    NullCheck.notNull(position, "position");

    final ReferenceOpenHashSet<Polygon> results =
      new ReferenceOpenHashSet<>();
    final AreaL area = AreasL.create(
      (long) (position.x() - 1),
      (long) (position.y() - 1),
      2L,
      2L);
    this.polygons_tree.overlappedBy(area, results);

    for (final Polygon poly : results) {
      final List<Vector2I> points =
        poly.vertices.stream()
          .map(Vertex::position)
          .collect(Collectors.toList());

      if (MeshPolygons.containsPoint(points, position)) {
        return Optional.of(poly);
      }
    }

    return Optional.empty();
  }

  @Override
  public Collection<PolygonVertexType> vertices()
  {
    return this.vertices_view;
  }

  @Override
  public List<String> check()
  {
    final ReferenceArrayList<String> errors = new ReferenceArrayList<>();

    for (final Polygon p : this.polygons.values()) {
      if (p.deleted) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Polygon ");
        sb.append(p);
        sb.append(": Polygon has been deleted ");
        errors.add(sb.toString());
      }

      for (final Vertex v : p.vertices) {
        for (final Polygon q : v.polygons) {
          if (!this.polygons.containsKey(q.id.value())) {
            final StringBuilder sb = new StringBuilder(128);
            sb.append("Polygon ");
            sb.append(p);
            sb.append(": Vertex ");
            sb.append(v);
            sb.append(" references nonexistent polygon ");
            sb.append(q);
            errors.add(sb.toString());
          } else {
            final Polygon r = this.polygons.get(q.id.value());
            if (!Objects.equals(r, q)) {
              final StringBuilder sb = new StringBuilder(128);
              sb.append("Polygon ");
              sb.append(p);
              sb.append(": Vertex ");
              sb.append(v);
              sb.append(" references a polygon ");
              sb.append(q);
              sb.append(" but a polygon exists with the same id ");
              sb.append(r);
              errors.add(sb.toString());
            }
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

        if (v.deleted) {
          final StringBuilder sb = new StringBuilder(128);
          sb.append("Polygon ");
          sb.append(p);
          sb.append(": Vertex ");
          sb.append(v);
          sb.append(" has been deleted");
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
          if (!this.polygons.containsKey(q.id.value())) {
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
      for (final Polygon p : this.polygons.values()) {
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

  private static final class Polygon implements PolygonType
  {
    private final ReferenceArrayList<Edge> edges;
    private final List<PolygonEdgeType> edges_view;
    private final ReferenceArrayList<Vertex> vertices;
    private final List<PolygonVertexType> vertices_view;
    private final PolygonID id;
    private AreaL bounds;
    private boolean deleted;

    Polygon(
      final PolygonID in_id,
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
    public String toString()
    {
      final StringBuilder sb = new StringBuilder("[Polygon ");
      sb.append(this.id.value());
      sb.append("]");
      return sb.toString();
    }

    @Override
    public PolygonID id()
    {
      return this.id;
    }

    @Override
    public AreaL bounds()
    {
      return this.bounds;
    }

    @Override
    public List<PolygonEdgeType> edges()
    {
      return this.edges_view;
    }

    @Override
    public List<PolygonVertexType> vertices()
    {
      return this.vertices_view;
    }

    @Override
    public boolean deleted()
    {
      return this.deleted;
    }
  }

  private static final class Edge implements PolygonEdgeType
  {
    private final Vertex vertex0;
    private final Vertex vertex1;
    private final ReferenceOpenHashSet<Polygon> polygons;
    private final Set<PolygonType> polygons_view;

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
    public String toString()
    {
      final StringBuilder sb = new StringBuilder("[Edge ");
      sb.append(this.vertex0.id.value());
      sb.append(" <-> ");
      sb.append(this.vertex1.id.value());
      sb.append("]");
      return sb.toString();
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
    public Set<PolygonType> polygons()
    {
      return this.polygons_view;
    }

    @Override
    public PolygonVertexType vertex0()
    {
      return this.vertex0;
    }

    @Override
    public PolygonVertexType vertex1()
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
      return MeshPolygons.normal(
        this.vertex0.position,
        this.vertex1.position);
    }

    @Override
    public AreaL bounds()
    {
      return AreaL.of(
        Math.min(
          (long) this.vertex0.position.x(),
          (long) this.vertex1.position.x()),
        Math.max(
          (long) this.vertex0.position.x(),
          (long) this.vertex1.position.x()),
        Math.min(
          (long) this.vertex0.position.y(),
          (long) this.vertex1.position.y()),
        Math.max(
          (long) this.vertex0.position.y(),
          (long) this.vertex1.position.y()));
    }
  }

  private static final class Vertex implements PolygonVertexType
  {
    private final ReferenceOpenHashSet<Polygon> polygons;
    private final Set<PolygonType> polygons_view;
    private final PolygonVertexID id;
    private Vector2I position;
    private boolean deleted;

    private Vertex(
      final PolygonVertexID in_id,
      final Vector2I in_position)
    {
      this.id = NullCheck.notNull(in_id, "ID");
      this.position = NullCheck.notNull(in_position, "Position");
      this.polygons = new ReferenceOpenHashSet<>(4);
      this.polygons_view = Collections.unmodifiableSet(this.polygons);
    }

    @Override
    public String toString()
    {
      final StringBuilder sb = new StringBuilder("[Vertex ");
      sb.append(this.id.value());
      sb.append("]");
      return sb.toString();
    }

    @Override
    public PolygonVertexID id()
    {
      return this.id;
    }

    @Override
    public Set<PolygonType> polygons()
    {
      return this.polygons_view;
    }

    @Override
    public Vector2I position()
    {
      return this.position;
    }

    @Override
    public boolean deleted()
    {
      return this.deleted;
    }
  }
}
