package com.io7m.roommodel0;

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoomModelOpVertexMove implements RoomModelOpType<Unit>
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(RoomModelOpVertexMove.class);
  }

  private final Vector2I position;
  private final RoomPolyVertexID id;
  private Vector2I original;

  RoomModelOpVertexMove(
    final RoomPolyVertexType in_vertex,
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
    final RoomModelType model)
  {
    LOG.debug("move {}", this.position);

    model.vertexSetPosition(this.id, this.position);
    return Unit.unit();
  }

  @Override
  public void undo(
    final RoomModelType model)
  {
    LOG.debug("undo move {}", this.position);

    model.vertexSetPosition(this.id, this.original);
  }
}
