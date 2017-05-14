package com.io7m.roommodel0;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.jnull.NullCheck.notNull;
import static com.io7m.roommodel0.RoomModelOpPolygonDelete.deletePolygon;
import static com.io7m.roommodel0.RoomPolygons.isConvex;
import static java.util.stream.Collectors.toList;

public final class RoomEditingModel implements RoomEditingModelType
{
  private final RoomModelOpExecutorType exec;

  private RoomEditingModel(
    final RoomModelOpExecutorType in_model)
  {
    this.exec = notNull(in_model, "Executor");
  }

  public static RoomEditingModelType create(
    final RoomModelOpExecutorType in_exec)
  {
    return new RoomEditingModel(in_exec);
  }

  @Override
  public RoomEditingPolygonCreatorType polygonCreate()
  {
    return new PolygonCreator(this.exec);
  }

  @Override
  public RoomEditingVertexMoverType vertexMove()
  {
    return new VertexMover(this.exec);
  }

  @Override
  public boolean polygonDelete(
    final int x,
    final int y)
  {
    final Optional<RoomPolygonType> poly_opt =
      this.exec.model().polygonFind(Vector2I.of(x, y));

    if (poly_opt.isPresent()) {
      this.exec.evaluate(deletePolygon(poly_opt.get()));
      return true;
    }

    return false;
  }

  private static final class PolygonCreator
    implements RoomEditingPolygonCreatorType
  {
    private final RoomModelOpExecutorType exec;
    private final List<Vector2I> positions;

    PolygonCreator(
      final RoomModelOpExecutorType in_exec)
    {
      this.exec = notNull(in_exec, "Executor");
      this.positions = new ReferenceArrayList<>();
    }

    @Override
    public boolean addVertex(
      final Vector2I position)
    {
      notNull(position, "Position");

      if (!this.positions.isEmpty()) {
        if (Objects.equals(this.positions.get(0), position)) {
          return true;
        }
      }

      this.positions.add(position);
      return false;
    }

    @Override
    public RoomPolygonType create()
    {
      try {
        return this.exec.evaluate(new RoomModelOpPolygonCreate(this.positions));
      } finally {
        this.positions.clear();
      }
    }

    @Override
    public List<Vector2I> vertices()
    {
      return this.positions;
    }
  }

  private static final class LaxVertex implements RoomEditingVertexMoverType.TemporaryVertexType
  {
    private final ReferenceOpenHashSet<LaxPolygon> polygons;
    private Vector2I position;
    private RoomPolyVertexType original;

    LaxVertex()
    {
      this.polygons = new ReferenceOpenHashSet<>();
    }

    @Override
    public Vector2I position()
    {
      return this.position;
    }
  }

  private static final class LaxPolygon implements RoomEditingVertexMoverType.TemporaryPolygonType
  {
    private final ReferenceArrayList<LaxVertex> vertices;
    private final List<RoomEditingVertexMoverType.TemporaryVertexType> vertices_view;
    private boolean convex;

    LaxPolygon()
    {
      this.vertices = new ReferenceArrayList<>();
      this.vertices_view = Collections.unmodifiableList(this.vertices);
    }

    @Override
    public List<RoomEditingVertexMoverType.TemporaryVertexType> vertices()
    {
      return this.vertices_view;
    }

    @Override
    public boolean isConvex()
    {
      return this.convex;
    }
  }

  private static final class VertexMover implements RoomEditingVertexMoverType
  {
    private final RoomModelOpExecutorType exec;
    private final List<TemporaryPolygonType> polygons_view;
    private LaxVertex moving_vertex;
    private ReferenceArrayList<LaxPolygon> polygons;

    VertexMover(
      final RoomModelOpExecutorType in_exec)
    {
      this.exec = notNull(in_exec, "Executor");
      this.polygons = new ReferenceArrayList<>();
      this.polygons_view = Collections.unmodifiableList(this.polygons);
    }

    @Override
    public boolean isVertexSelected()
    {
      return this.moving_vertex != null;
    }

    @Override
    public void setVertexPosition(
      final Vector2I position)
    {
      this.moving_vertex.position = position;
      this.updatePolygonsAndPosition();
    }

    @Override
    public void commit()
    {
      this.exec.evaluate(new RoomModelOpVertexMove(
        this.moving_vertex.original,
        this.moving_vertex.position));
      this.moving_vertex = null;
      this.polygons.clear();
    }

    @Override
    public boolean isVertexOK()
    {
      for (final LaxPolygon p : this.polygons) {
        if (!p.convex) {
          return false;
        }
      }
      return true;
    }

    @Override
    public boolean selectVertex(
      final Vector2I position)
    {
      notNull(position, "Position");

      final Optional<RoomPolyVertexType> vertex_opt =
        this.exec.model().vertexFind(position);

      if (vertex_opt.isPresent()) {
        final RoomPolyVertexType room_vertex = vertex_opt.get();

        this.moving_vertex = new LaxVertex();
        this.moving_vertex.original = room_vertex;
        this.moving_vertex.position = room_vertex.position();

        this.updatePolygonsAndPosition();
        return true;
      }

      return false;
    }

    private void updatePolygonsAndPosition()
    {
      this.polygons.clear();

      final RoomPolyVertexType room_vertex = this.moving_vertex.original;
      for (final RoomPolygonType polygon : room_vertex.polygons()) {
        final LaxPolygon lax_poly = new LaxPolygon();
        this.moving_vertex.polygons.add(lax_poly);

        for (final RoomPolyVertexType v : polygon.vertices()) {
          if (Objects.equals(v, room_vertex)) {
            lax_poly.vertices.add(this.moving_vertex);
          } else {
            final LaxVertex other_vertex = new LaxVertex();
            other_vertex.position = v.position();
            lax_poly.vertices.add(other_vertex);
          }
        }

        lax_poly.convex =
          isConvex(lax_poly.vertices.stream()
                     .map(LaxVertex::position)
                     .collect(toList()));
        this.polygons.add(lax_poly);
      }
    }

    @Override
    public List<TemporaryPolygonType> temporaryPolygons()
    {
      return this.polygons_view;
    }
  }
}
