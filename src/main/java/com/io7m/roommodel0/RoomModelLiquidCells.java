package com.io7m.roommodel0;

import com.io7m.jfunctional.Pair;
import com.io7m.jnull.Nullable;
import com.io7m.jregions.core.unparameterized.areas.AreaL;
import com.io7m.jregions.core.unparameterized.areas.AreasL;
import com.io7m.jspatial.api.quadtrees.QuadTreeReadableLType;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2D;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.roommodel0.mesh.MeshReadableType;
import com.io7m.roommodel0.mesh.PolygonEdgeType;
import com.io7m.roommodel0.mesh.PolygonType;
import com.io7m.roommodel0.mesh.PolygonVertexType;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

import static com.io7m.jnull.NullCheck.notNull;

public final class RoomModelLiquidCells
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(RoomModelLiquidCells.class);
  }

  final ReferenceArrayList<List<Vector2I>> polygons;
  final ReferenceArrayList<Pair<Vector2I, Vector2I>> intersections;

  private RoomModelLiquidCells(
    final ReferenceArrayList<List<Vector2I>> in_polygons,
    final ReferenceArrayList<Pair<Vector2I, Vector2I>> intersections)
  {
    this.polygons = notNull(in_polygons, "Polygons");
    this.intersections = notNull(intersections, "Intersections");
  }

  public static RoomModelLiquidCells generate(
    final MeshReadableType mesh)
  {
    notNull(mesh, "Mesh");

    final ReferenceArrayList<List<Vector2I>> polygons =
      new ReferenceArrayList<>();
    final ReferenceArrayList<Pair<Vector2I, Vector2I>> intersections =
      new ReferenceArrayList<>();

    final QuadTreeReadableLType<PolygonType> tree = mesh.polygonTree();
    final AreaL bounds = tree.bounds();

    final IntRBTreeSet y_values = collectVertices(mesh);

    int y_previous = Math.toIntExact(bounds.minimumY());
    for (final int y_current : y_values) {

      final AreaL area =
        AreaL.of(
          bounds.minimumX(),
          bounds.maximumX(),
          (long) y_previous,
          (long) y_current);

      final ReferenceArrayList<PolygonEdgeType> edges_sorted =
        collectEdges(tree, area);
      final ReferenceArrayList<EdgeRecord> records =
        collectEdgeRecords(edges_sorted, area);

      LOG.debug(
        "{}:{} edges {} ({})",
        Integer.valueOf(y_previous),
        Integer.valueOf(y_current),
        Integer.valueOf(edges_sorted.size()),
        Integer.valueOf(records.size()));

      boolean inside_polygon = false;
      for (int index = 1; index < records.size(); ++index) {
        final EdgeRecord record0 = records.get(index - 1);
        final EdgeRecord record1 = records.get(index);

        inside_polygon = !inside_polygon;
        if (inside_polygon) {
          final ReferenceArrayList<Vector2I> vs = new ReferenceArrayList<>(4);
          vs.add(record0.intersection_max);
          vs.add(record0.intersection_min);
          vs.add(record1.intersection_min);
          vs.add(record1.intersection_max);
          polygons.add(vs);
        }
      }

      y_previous = y_current;
    }

    LOG.debug("created {} polygons", Integer.valueOf(polygons.size()));
    return new RoomModelLiquidCells(polygons, intersections);
  }

  private static ReferenceArrayList<PolygonEdgeType> collectEdges(
    final QuadTreeReadableLType<PolygonType> tree,
    final AreaL area)
  {
    final ReferenceOpenHashSet<PolygonType> overlapped_polygons =
      new ReferenceOpenHashSet<>();
    tree.overlappedBy(area, overlapped_polygons);

    final ReferenceArrayList<PolygonEdgeType> edges_sorted =
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

        final AreaL edge_area = e.bounds();
        if (AreasL.overlaps(area, edge_area)) {
          edges_sorted.add(e);
        }
      }
    }

    edges_sorted.sort(Comparator.comparingLong(o -> o.bounds().minimumX()));
    return edges_sorted;
  }

  private static ReferenceArrayList<EdgeRecord> collectEdgeRecords(
    final ReferenceArrayList<PolygonEdgeType> edges_sorted,
    final AreaL area)
  {
    final ReferenceArrayList<EdgeRecord> records =
      new ReferenceArrayList<>(edges_sorted.size() + 2);

    final int y_min = Math.toIntExact(area.minimumY());
    final int y_max = Math.toIntExact(area.maximumY());

    records.add(new EdgeRecord(
      null,
      Vector2I.of(Math.toIntExact(area.minimumX()), y_min),
      Vector2I.of(Math.toIntExact(area.minimumX()), y_max)));

    for (int index = 0; index < edges_sorted.size(); ++index) {
      final PolygonEdgeType edge = edges_sorted.get(index);

      final Vector2D inter_y_min =
        RoomLineIntersections.intersection(
          edge.vertex0().position(),
          edge.vertex1().position(),
          Vector2I.of(Math.toIntExact(area.minimumX()), y_min),
          Vector2I.of(Math.toIntExact(area.maximumX()), y_min)).get();

      final Vector2D inter_y_max =
        RoomLineIntersections.intersection(
          edge.vertex0().position(),
          edge.vertex1().position(),
          Vector2I.of(Math.toIntExact(area.minimumX()), y_max),
          Vector2I.of(Math.toIntExact(area.maximumX()), y_max)).get();

      records.add(new EdgeRecord(
        edge,
        Vector2I.of((int) inter_y_min.x(), y_min),
        Vector2I.of((int) inter_y_max.x(), y_max)));
    }

    records.add(new EdgeRecord(
      null,
      Vector2I.of(Math.toIntExact(area.maximumX()), y_min),
      Vector2I.of(Math.toIntExact(area.maximumX()), y_max)));

    return records;
  }

  private static IntRBTreeSet collectVertices(
    final MeshReadableType mesh)
  {
    final IntRBTreeSet ys = new IntRBTreeSet();
    for (final PolygonVertexType v : mesh.vertices()) {
      ys.add(v.position().y());
    }
    ys.add(Math.toIntExact(mesh.polygonTree().bounds().maximumY()));
    return ys;
  }

  private static final class EdgeRecord
  {
    private final @Nullable PolygonEdgeType edge;
    private final Vector2I intersection_min;
    private final Vector2I intersection_max;

    EdgeRecord(
      final PolygonEdgeType edge,
      final Vector2I intersection_min,
      final Vector2I intersection_max)
    {
      this.edge = edge;
      this.intersection_min = intersection_min;
      this.intersection_max = intersection_max;
    }
  }
}
