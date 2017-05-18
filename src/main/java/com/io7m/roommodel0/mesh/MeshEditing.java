package com.io7m.roommodel0.mesh;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.roommodel0.RoomPolyVertexType;
import com.io7m.roommodel0.RoomPolygonType;
import com.io7m.roommodel0.undo.UndoControllerType;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.jnull.NullCheck.notNull;
import static com.io7m.roommodel0.mesh.MeshCommandPolygonDelete.deletePolygon;
import static com.io7m.roommodel0.mesh.MeshPolygons.isConvex;
import static java.util.stream.Collectors.toList;

public final class MeshEditing implements MeshEditingType
{
  private final UndoControllerType<MeshType> controller;

  private MeshEditing(
    final UndoControllerType<MeshType> in_controller)
  {
    this.controller = notNull(in_controller, "Controller");
  }

  public static MeshEditingType create(
    final UndoControllerType<MeshType> in_controller)
  {
    return new MeshEditing(in_controller);
  }

  @Override
  public MeshEditingPolygonCreatorType polygonCreate()
  {
    return new PolygonCreator(this.controller);
  }

  @Override
  public MeshEditingVertexMoverType vertexMove()
  {
    return new VertexMover(this.controller);
  }

  @Override
  public boolean polygonDelete(
    final int x,
    final int y)
  {
    final Optional<PolygonType> poly_opt =
      this.controller.state().polygonFind(Vector2I.of(x, y));

    if (poly_opt.isPresent()) {
      this.controller.evaluate(deletePolygon(poly_opt.get()));
      return true;
    }

    return false;
  }

  private static final class PolygonCreator
    implements MeshEditingPolygonCreatorType
  {
    private final UndoControllerType<MeshType> exec;
    private final List<Vector2I> positions;

    PolygonCreator(
      final UndoControllerType<MeshType> in_exec)
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
    public PolygonType create()
    {
      try {
        return this.exec.evaluate(new MeshCommandPolygonCreate(this.positions));
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

  private static final class LaxVertex implements MeshEditingVertexMoverType.TemporaryVertexType
  {
    private final ReferenceOpenHashSet<LaxPolygon> polygons;
    private Vector2I position;
    private PolygonVertexType original;

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

  private static final class LaxPolygon implements MeshEditingVertexMoverType.TemporaryPolygonType
  {
    private final ReferenceArrayList<LaxVertex> vertices;
    private final List<MeshEditingVertexMoverType.TemporaryVertexType> vertices_view;
    private boolean convex;

    LaxPolygon()
    {
      this.vertices = new ReferenceArrayList<>();
      this.vertices_view = Collections.unmodifiableList(this.vertices);
    }

    @Override
    public List<MeshEditingVertexMoverType.TemporaryVertexType> vertices()
    {
      return this.vertices_view;
    }

    @Override
    public boolean isConvex()
    {
      return this.convex;
    }
  }

  private static final class VertexMover implements MeshEditingVertexMoverType
  {
    private final UndoControllerType<MeshType> exec;
    private final List<TemporaryPolygonType> polygons_view;
    private LaxVertex moving_vertex;
    private ReferenceArrayList<LaxPolygon> polygons;

    VertexMover(
      final UndoControllerType<MeshType> in_exec)
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
      this.exec.evaluate(new MeshCommandVertexMove(
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

      final Optional<PolygonVertexType> vertex_opt =
        this.exec.state().vertexFind(position);

      if (vertex_opt.isPresent()) {
        final PolygonVertexType room_vertex = vertex_opt.get();

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

      final PolygonVertexType room_vertex = this.moving_vertex.original;
      for (final PolygonType polygon : room_vertex.polygons()) {
        final LaxPolygon lax_poly = new LaxPolygon();
        this.moving_vertex.polygons.add(lax_poly);

        for (final PolygonVertexType v : polygon.vertices()) {
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
