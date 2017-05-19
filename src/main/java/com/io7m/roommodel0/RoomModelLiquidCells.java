package com.io7m.roommodel0;

import com.io7m.jnull.Nullable;
import com.io7m.jregions.core.unparameterized.areas.AreaI;
import com.io7m.jregions.core.unparameterized.areas.AreasI;
import com.io7m.jspatial.api.quadtrees.QuadTreeReadableIType;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2D;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.roommodel0.mesh.Mesh;
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

import java.util.Comparator;

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
    final IntRBTreeSet y_values = collectVertices(mesh);
    int y_previous = bounds.minimumY();
    for (final int y_current : y_values) {

      final AreaI area =
        AreaI.of(bounds.minimumX(), bounds.maximumX(), y_previous, y_current);

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

          final Vector2I v0p = record0.intersection_max;
          final Vector2I v1p = record0.intersection_min;
          final Vector2I v2p = record1.intersection_min;
          final Vector2I v3p = record1.intersection_max;

          {
            final ReferenceArrayList<Vector2I> vs = new ReferenceArrayList<>(4);
            vs.add(v0p);
            vs.add(v1p);
            vs.add(v2p);
            vs.add(v3p);

            if (vs.stream().distinct().count() != 4L) {
              LOG.debug("discarding zero size poly");
              continue;
            }
          }

          final PolygonVertexType v0 =
            cell_mesh.vertexFind(v0p).orElse(cell_mesh.vertexCreate(v0p));
          final PolygonVertexType v1 =
            cell_mesh.vertexFind(v1p).orElse(cell_mesh.vertexCreate(v1p));
          final PolygonVertexType v2 =
            cell_mesh.vertexFind(v2p).orElse(cell_mesh.vertexCreate(v2p));
          final PolygonVertexType v3 =
            cell_mesh.vertexFind(v3p).orElse(cell_mesh.vertexCreate(v3p));

          LOG.debug("create {} {} {} {}", v0, v1, v2, v3);

          final PolygonType polygon =
            cell_mesh.polygonCreateVV(v0, v1, v2, v3);

          for (final PolygonVertexType v : polygon.vertices()) {
            for (final PolygonType vp : v.polygons()) {
              LOG.debug(
                "{} touches {}",
                Long.valueOf(polygon.id().value()),
                Long.valueOf(vp.id().value()));
            }
          }
        }
      }

      y_previous = y_current;
    }

    LOG.debug(
      "created {} polygons",
      Integer.valueOf(cell_mesh.polygons().size()));
    return new RoomModelLiquidCells(cell_mesh);
  }

  public MeshReadableType mesh()
  {
    return this.mesh;
  }

  private static ReferenceArrayList<PolygonEdgeType> collectEdges(
    final QuadTreeReadableIType<PolygonType> tree,
    final AreaI area)
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

        final AreaI edge_area = e.bounds();
        if (AreasI.overlaps(area, edge_area)) {
          edges_sorted.add(e);
        }
      }
    }

    edges_sorted.sort(Comparator.comparingLong(o -> o.bounds().minimumX()));
    return edges_sorted;
  }

  private static ReferenceArrayList<EdgeRecord> collectEdgeRecords(
    final ReferenceArrayList<PolygonEdgeType> edges_sorted,
    final AreaI area)
  {
    final ReferenceArrayList<EdgeRecord> records =
      new ReferenceArrayList<>(edges_sorted.size() + 2);

    final int y_min = area.minimumY();
    final int y_max = area.maximumY();

    records.add(new EdgeRecord(
      null,
      Vector2I.of(area.minimumX(), y_min),
      Vector2I.of(area.minimumX(), y_max)));

    for (int index = 0; index < edges_sorted.size(); ++index) {
      final PolygonEdgeType edge = edges_sorted.get(index);

      final Vector2D inter_y_min =
        RoomLineIntersections.intersection(
          edge.vertex0().position(),
          edge.vertex1().position(),
          Vector2I.of(area.minimumX(), y_min),
          Vector2I.of(area.maximumX(), y_min)).get();

      final Vector2D inter_y_max =
        RoomLineIntersections.intersection(
          edge.vertex0().position(),
          edge.vertex1().position(),
          Vector2I.of(area.minimumX(), y_max),
          Vector2I.of(area.maximumX(), y_max)).get();

      records.add(new EdgeRecord(
        edge,
        Vector2I.of((int) inter_y_min.x(), y_min),
        Vector2I.of((int) inter_y_max.x(), y_max)));
    }

    records.add(new EdgeRecord(
      null,
      Vector2I.of(area.maximumX(), y_min),
      Vector2I.of(area.maximumX(), y_max)));

    return records;
  }

  private static IntRBTreeSet collectVertices(
    final MeshReadableType mesh)
  {
    final IntRBTreeSet ys = new IntRBTreeSet();
    for (final PolygonVertexType v : mesh.vertices()) {
      ys.add(v.position().y());
    }
    ys.add(mesh.polygonTree().bounds().maximumY());
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
