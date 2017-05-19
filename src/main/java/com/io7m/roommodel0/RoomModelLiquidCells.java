package com.io7m.roommodel0;

import com.io7m.jaffirm.core.Invariants;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.jregions.core.unparameterized.areas.AreaI;
import com.io7m.jregions.core.unparameterized.areas.AreasI;
import com.io7m.jspatial.api.quadtrees.QuadTreeReadableIType;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2D;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.roommodel0.mesh.Mesh;
import com.io7m.roommodel0.mesh.MeshPolygons;
import com.io7m.roommodel0.mesh.MeshReadableType;
import com.io7m.roommodel0.mesh.MeshType;
import com.io7m.roommodel0.mesh.PolygonEdgeType;
import com.io7m.roommodel0.mesh.PolygonType;
import com.io7m.roommodel0.mesh.PolygonVertexType;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.io7m.jnull.NullCheck.notNull;

public final class RoomModelLiquidCells
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(RoomModelLiquidCells.class);
  }

  private final MeshType mesh;

  private RoomModelLiquidCells(
    final MeshType in_mesh)
  {
    this.mesh = notNull(in_mesh, "Mesh");
  }

  public static RoomModelLiquidCells generate(
    final MeshReadableType mesh)
  {
    notNull(mesh, "Mesh");

    final MeshType cell_mesh = Mesh.create(mesh.polygonTree().bounds());
    final QuadTreeReadableIType<PolygonType> tree = mesh.polygonTree();
    final AreaI bounds = tree.bounds();
    final IntRBTreeSet y_values = collectVertexYValues(mesh);
    int y_previous = bounds.minimumY();
    for (final int y_current : y_values) {
      final AreaI span_bounds =
        AreaI.of(bounds.minimumX(), bounds.maximumX(), y_previous, y_current);
      processSpan(cell_mesh, tree, span_bounds);
      y_previous = y_current;
    }

    LOG.debug(
      "created {} polygons",
      Integer.valueOf(cell_mesh.polygons().size()));
    return new RoomModelLiquidCells(cell_mesh);
  }

  private static void processSpan(
    final MeshType cell_mesh,
    final QuadTreeReadableIType<PolygonType> tree,
    final AreaI span_bounds)
  {
    final ReferenceArrayList<EdgeIntersection> intersections =
      collectEdgeIntersections(tree, span_bounds);
    final ReferenceArrayList<EdgePair> pairs =
      collectEdgePairs(intersections);

    LOG.debug(
      "{}:{} edges {} ({})",
      Integer.valueOf(span_bounds.minimumY()),
      Integer.valueOf(span_bounds.maximumY()),
      Integer.valueOf(intersections.size()),
      Integer.valueOf(pairs.size()));

    for (int index = 0; index < pairs.size(); ++index) {
      final EdgePair pair = pairs.get(index);

      LOG.debug("  {}:{}", pair.intersection0.edge, pair.intersection1.edge);

      final ReferenceArrayList<Vector2I> positions =
        new ReferenceArrayList<>(4);
      positions.add(pair.intersection0.maximum);
      positions.add(pair.intersection0.minimum);
      positions.add(pair.intersection1.minimum);
      positions.add(pair.intersection1.maximum);

      final List<Vector2I> distinct_positions =
        positions.stream().distinct().collect(Collectors.toList());

      final PolygonType polygon;
      switch (distinct_positions.size()) {
        case 3: {
          final Vector2I v0p = distinct_positions.get(0);
          final Vector2I v1p = distinct_positions.get(1);
          final Vector2I v2p = distinct_positions.get(2);
          polygon = cell_mesh.polygonCreateVV(
            cell_mesh.vertexFind(v0p).orElse(cell_mesh.vertexCreate(v0p)),
            cell_mesh.vertexFind(v1p).orElse(cell_mesh.vertexCreate(v1p)),
            cell_mesh.vertexFind(v2p).orElse(cell_mesh.vertexCreate(v2p)));
          break;
        }
        case 4: {
          final Vector2I v0p = distinct_positions.get(0);
          final Vector2I v1p = distinct_positions.get(1);
          final Vector2I v2p = distinct_positions.get(2);
          final Vector2I v3p = distinct_positions.get(3);
          polygon = cell_mesh.polygonCreateVV(
            cell_mesh.vertexFind(v0p).orElse(cell_mesh.vertexCreate(v0p)),
            cell_mesh.vertexFind(v1p).orElse(cell_mesh.vertexCreate(v1p)),
            cell_mesh.vertexFind(v2p).orElse(cell_mesh.vertexCreate(v2p)),
            cell_mesh.vertexFind(v3p).orElse(cell_mesh.vertexCreate(v3p)));
          break;
        }
        default: {
          LOG.debug(
            "ignoring polygon with {} vertices",
            Integer.valueOf(distinct_positions.size()));
          continue;
        }
      }

      final AreaI poly_bounds = polygon.bounds();
      for (final PolygonVertexType v : polygon.vertices()) {
        for (final PolygonType vp : v.polygons()) {
          if (!Objects.equals(vp.id(), polygon.id())) {
            LOG.debug(
              "{} touches {}",
              Long.valueOf(polygon.id().value()),
              Long.valueOf(vp.id().value()));
          }
        }
      }
    }
  }

  private static ReferenceArrayList<EdgePair> collectEdgePairs(
    final ReferenceArrayList<EdgeIntersection> intersections)
  {
    Preconditions.checkPreconditionI(
      intersections.size(),
      intersections.size() % 2 == 0,
      i -> "Intersection list size must be even");

    final ReferenceArrayList<EdgePair> pairs =
      new ReferenceArrayList<>(intersections.size() / 2);
    for (int index = 0; index < intersections.size(); index += 2) {
      pairs.add(new EdgePair(
        intersections.get(index),
        intersections.get(index + 1)));
    }
    return pairs;
  }

  private static IntRBTreeSet collectVertexYValues(
    final MeshReadableType mesh)
  {
    final IntRBTreeSet ys = new IntRBTreeSet();
    for (final PolygonVertexType v : mesh.vertices()) {
      ys.add(v.position().y());
    }
    ys.add(mesh.polygonTree().bounds().maximumY());
    return ys;
  }

  private static ReferenceArrayList<EdgeIntersection> collectEdgeIntersections(
    final QuadTreeReadableIType<PolygonType> tree,
    final AreaI span_bounds)
  {
    //
    // Get the set of polygons that are overlapped by the current span.
    //

    final ReferenceOpenHashSet<PolygonType> overlapped_polygons =
      new ReferenceOpenHashSet<>();
    tree.overlappedBy(span_bounds, overlapped_polygons);

    //
    // For each polygon, collect each external edge that overlaps the span,
    // ignoring those that point straight up/down.
    //

    final ReferenceArrayList<PolygonEdgeType> edges =
      new ReferenceArrayList<>();

    for (final PolygonType p : overlapped_polygons) {
      for (final PolygonEdgeType e : p.edges()) {
        if (!e.isExternal()) {
          continue;
        }

        final Vector2D n = e.normal();
        if (n.x() == 0.0) {
          continue;
        }

        final AreaI edge_area = e.bounds();
        if (AreasI.overlaps(span_bounds, edge_area)) {
          edges.add(e);
        }
      }
    }

    //
    // For each collected edge, store the points at which each edge intersects
    // the span bounds. Additionally, fake edge intersections are added for the
    // start and end of the span.
    //

    final int y_min = span_bounds.minimumY();
    final int y_max = span_bounds.maximumY();

    final ReferenceArrayList<EdgeIntersection> intersections =
      new ReferenceArrayList<>();

    intersections.add(new EdgeIntersection(
      null,
      Vector2I.of(span_bounds.minimumX(), y_min),
      Vector2I.of(span_bounds.minimumX(), y_max)));

    for (int index = 0; index < edges.size(); ++index) {
      final PolygonEdgeType edge = edges.get(index);

      final Optional<Vector2D> y_min_opt =
        RoomLineIntersections.intersection(
          edge.vertex0().position(),
          edge.vertex1().position(),
          Vector2I.of(span_bounds.minimumX(), y_min),
          Vector2I.of(span_bounds.maximumX(), y_min));

      Invariants.checkInvariantV(
        y_min_opt.isPresent(),
        "Edge %s must intersect span %s", edge, span_bounds);

      final Vector2D inter_y_min = y_min_opt.get();

      final Optional<Vector2D> y_max_opt1 =
        RoomLineIntersections.intersection(
          edge.vertex0().position(),
          edge.vertex1().position(),
          Vector2I.of(span_bounds.minimumX(), y_max),
          Vector2I.of(span_bounds.maximumX(), y_max));

      Invariants.checkInvariantV(
        y_min_opt.isPresent(),
        "Edge %s must intersect span %s", edge, span_bounds);

      final Vector2D inter_y_max = y_max_opt1.get();

      intersections.add(new EdgeIntersection(
        edge,
        Vector2I.of((int) inter_y_min.x(), y_min),
        Vector2I.of((int) inter_y_max.x(), y_max)));
    }

    intersections.add(new EdgeIntersection(
      null,
      Vector2I.of(span_bounds.maximumX(), y_min),
      Vector2I.of(span_bounds.maximumX(), y_max)));

    //
    // Sort the intersections on the X axis.
    //

    intersections.sort((intersection0, intersection1) -> {
      final AreaI bounds0 =
        MeshPolygons.edgeBounds(intersection0.minimum, intersection0.maximum);
      final AreaI bounds1 =
        MeshPolygons.edgeBounds(intersection1.minimum, intersection1.maximum);
      final int cmp_x_min =
        Integer.compare(bounds0.minimumX(), bounds1.minimumX());
      if (cmp_x_min == 0) {
        return Integer.compare(bounds0.maximumX(), bounds1.maximumX());
      }
      return cmp_x_min;
    });

    return intersections;
  }

  public MeshReadableType mesh()
  {
    return this.mesh;
  }

  private static final class EdgePair
  {
    private final EdgeIntersection intersection0;
    private final EdgeIntersection intersection1;

    EdgePair(
      final EdgeIntersection in_intersection0,
      final EdgeIntersection in_intersection1)
    {
      this.intersection0 = notNull(in_intersection0, "Intersection 0");
      this.intersection1 = notNull(in_intersection1, "Intersection 1");
    }
  }

  private static final class EdgeIntersection
  {
    private final @Nullable PolygonEdgeType edge;
    private final Vector2I minimum;
    private final Vector2I maximum;

    EdgeIntersection(
      final PolygonEdgeType edge,
      final Vector2I intersection_min,
      final Vector2I intersection_max)
    {
      this.edge = edge;
      this.minimum = notNull(intersection_min, "Minimum");
      this.maximum = notNull(intersection_max, "Maximum");
    }
  }
}
