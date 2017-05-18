package com.io7m.roommodel0.mesh;

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MeshCommandVertexMove implements MeshCommandType<Unit>
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(MeshCommandVertexMove.class);
  }

  private final Vector2I position;
  private final PolygonVertexID id;
  private Vector2I original;

  MeshCommandVertexMove(
    final PolygonVertexType in_vertex,
    final Vector2I in_position)
  {
    NullCheck.notNull(in_vertex, "Vertex");
    this.position = NullCheck.notNull(in_position, "Position");
    this.original = in_vertex.position();
    this.id = in_vertex.id();
  }

  @Override
  public String description()
  {
    return "Move Vertex";
  }

  @Override
  public Unit evaluate(
    final MeshType mesh)
  {
    LOG.debug("move {}", this.position);

    mesh.vertexSetPosition(this.id, this.position);
    return Unit.unit();
  }

  @Override
  public void undo(
    final MeshType mesh)
  {
    LOG.debug("undo move {}", this.position);

    mesh.vertexSetPosition(this.id, this.original);
  }
}
