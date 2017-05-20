package com.io7m.roommodel0;

import com.io7m.jaffirm.core.Invariants;
import com.io7m.jaffirm.core.Preconditions;
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
import com.io7m.roommodel0.mesh.PolygonID;
import com.io7m.roommodel0.mesh.PolygonType;
import com.io7m.roommodel0.mesh.PolygonVertexType;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.io7m.jnull.NullCheck.notNull;

public final class RoomModelLiquidCells implements RoomLiquidCellsType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(RoomModelLiquidCells.class);
  }

  private final MeshType mesh;
  private final Set<Cell> cell_roots;
  private final Set<CellType> cell_roots_view;
  private final Map<PolygonID, CellType> cells_all_view;
  private final Map<PolygonID, Cell> cells_all;

  private RoomModelLiquidCells(
    final MeshType in_mesh,
    final Map<PolygonID, Cell> in_cells_all,
    final Set<Cell> in_cell_roots)
  {
    this.mesh = notNull(in_mesh, "Mesh");
    this.cell_roots = notNull(in_cell_roots, "Cell roots");
    this.cell_roots_view = Collections.unmodifiableSet(this.cell_roots);
    this.cells_all = notNull(in_cells_all, "Cells");
    this.cells_all_view = Collections.unmodifiableMap(this.cells_all);
  }

  public static RoomModelLiquidCells generate(
    final MeshReadableType mesh)
  {
    notNull(mesh, "Mesh");

    final MeshType cell_mesh = Mesh.create(mesh.polygonTree().bounds());
    final QuadTreeReadableIType<PolygonType> tree = mesh.polygonTree();
    final AreaI bounds = tree.bounds();
    final IntRBTreeSet y_values = collectVertexYValues(mesh);
    final Map<PolygonID, Cell> cells_all = new HashMap<>();
    Map<PolygonID, Cell> cells_current = Collections.emptyMap();
    Set<Cell> cell_roots = Collections.emptySet();
    int y_previous = bounds.minimumY();
    for (final int y_current : y_values) {
      final AreaI span_bounds =
        AreaI.of(bounds.minimumX(), bounds.maximumX(), y_previous, y_current);
      cells_current = processSpan(
        cell_mesh, cells_all, cells_current, tree, span_bounds);

      if (y_previous == bounds.minimumY()) {
        cell_roots = new ReferenceOpenHashSet<>(cells_current.values());
      }

      y_previous = y_current;
    }

    LOG.debug(
      "created {} polygons",
      Integer.valueOf(cell_mesh.polygons().size()));
    return new RoomModelLiquidCells(cell_mesh, cells_all, cell_roots);
  }

  private static Map<PolygonID, Cell> processSpan(
    final MeshType cell_mesh,
    final Map<PolygonID, Cell> all_cells,
    final Map<PolygonID, Cell> previous_cells,
    final QuadTreeReadableIType<PolygonType> tree,
    final AreaI span_bounds)
  {
    final ReferenceArrayList<EdgeIntersection> intersections =
      collectEdgeIntersections(tree, span_bounds);
    final ReferenceArrayList<EdgePair> pairs =
      collectEdgePairs(intersections);

    final Map<PolygonID, Cell> cells = new HashMap<>();
    for (int index = 0; index < pairs.size(); ++index) {
      final EdgePair pair = pairs.get(index);

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

      final Cell cell = createCell(previous_cells, polygon);
      all_cells.put(polygon.id(), cell);
      cells.put(polygon.id(), cell);
    }

    return cells;
  }

  private static int overlapSize(
    final int line0_x0,
    final int line0_x1,
    final int line1_x0,
    final int line1_x1)
  {
    final int x1 = Math.min(line0_x1, line1_x1);
    final int x0 = Math.max(line0_x0, line1_x0);
    return Math.max(0, Math.subtractExact(x1, x0));
  }

  private static boolean overlaps(
    final int line0_x0,
    final int line0_x1,
    final int line1_x0,
    final int line1_x1)
  {
    return overlapSize(line0_x0, line0_x1, line1_x0, line1_x1) > 0;
  }

  private static Cell createCell(
    final Map<PolygonID, Cell> previous_cells,
    final PolygonType curr_polygon)
  {
    final Optional<PolygonEdgeType> curr_bottom_opt =
      findBottomEdge(curr_polygon);
    final Optional<PolygonEdgeType> curr_top_opt =
      findTopEdge(curr_polygon);
    final Cell cell =
      new Cell(curr_polygon, curr_bottom_opt);

    //
    // If the current cell has a top edge, then check to see if any cells in
    // the span above have bottom edges that overlap it.
    //

    if (curr_top_opt.isPresent()) {
      final PolygonEdgeType curr_top = curr_top_opt.get();
      final PolygonVertexType curr_top_v0 = curr_top.vertex0();
      final PolygonVertexType curr_top_v1 = curr_top.vertex1();

      final int curr_x_min =
        Math.min(curr_top_v0.position().x(), curr_top_v1.position().x());
      final int curr_x_max =
        Math.max(curr_top_v0.position().x(), curr_top_v1.position().x());

      //
      // For each cell in the previous span, check any cell that has a bottom
      // edge, and check if the bottom edge overlaps the top of this cell.
      //

      for (final Cell above_cell : previous_cells.values()) {
        if (above_cell.bottom.isPresent()) {
          final PolygonEdgeType above_bottom = above_cell.bottom.get();

          final PolygonVertexType above_v0 = above_bottom.vertex0();
          final PolygonVertexType above_v1 = above_bottom.vertex1();
          final int above_x_min =
            Math.min(above_v0.position().x(), above_v1.position().x());
          final int above_x_max =
            Math.max(above_v0.position().x(), above_v1.position().x());

          if (overlaps(curr_x_min, curr_x_max, above_x_min, above_x_max)) {
            above_cell.below.add(cell);
            cell.above.add(above_cell);
          }
        }
      }
    }

    return cell;
  }

  private static Optional<PolygonEdgeType> findBottomEdge(
    final PolygonType polygon)
  {
    final AreaI bounds = polygon.bounds();

    for (final PolygonEdgeType e : polygon.edges()) {
      if (e.vertex0().position().y() == bounds.maximumY()
        && e.vertex1().position().y() == bounds.maximumY()) {
        return Optional.of(e);
      }
    }

    return Optional.empty();
  }

  private static Optional<PolygonEdgeType> findTopEdge(
    final PolygonType polygon)
  {
    final AreaI bounds = polygon.bounds();

    for (final PolygonEdgeType e : polygon.edges()) {
      if (e.vertex0().position().y() == bounds.minimumY()
        && e.vertex1().position().y() == bounds.minimumY()) {
        return Optional.of(e);
      }
    }

    return Optional.empty();
  }

  /**
   * Partition the list of intersections into pairs.
   */

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

  /**
   * Determine the set of Y values on which all of the vertices in the mesh
   * occur.
   */

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

  /**
   * Collect the edge intersections for the current span.
   */

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

  @Override
  public MeshReadableType mesh()
  {
    return this.mesh;
  }

  @Override
  public Set<CellType> cellRoots()
  {
    return this.cell_roots_view;
  }

  @Override
  public Map<PolygonID, CellType> cellsAll()
  {
    return this.cells_all_view;
  }

  private static final class Cell implements CellType
  {
    private final PolygonType polygon;
    private final Optional<PolygonEdgeType> bottom;
    private final ReferenceOpenHashSet<Cell> below;
    private final ReferenceOpenHashSet<Cell> above;
    private final Set<CellType> below_view;
    private final Set<CellType> above_view;

    Cell(
      final PolygonType in_polygon,
      final Optional<PolygonEdgeType> in_bottom)
    {
      this.polygon = notNull(in_polygon, "Polygon");
      this.bottom = notNull(in_bottom, "Bottom");
      this.below = new ReferenceOpenHashSet<>();
      this.below_view = Collections.unmodifiableSet(this.below);
      this.above = new ReferenceOpenHashSet<>();
      this.above_view = Collections.unmodifiableSet(this.above);
    }

    @Override
    public PolygonType polygon()
    {
      return this.polygon;
    }

    @Override
    public Set<CellType> cellsAbove()
    {
      return this.above_view;
    }

    @Override
    public Set<CellType> cellsBelow()
    {
      return this.below_view;
    }
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
