package com.io7m.roommodel0;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.junreachable.UnimplementedCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.io7m.jnull.NullCheck.notNull;

public final class RoomModelOpVertexCreate implements RoomModelOpType<RoomPolyVertexType>
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(RoomModelOpVertexCreate.class);
  }

  private final Vector2I position;
  private RoomPolyVertexType vertex;

  public RoomModelOpVertexCreate(
    final Vector2I in_position)
  {
    this.position = notNull(in_position, "Position");
  }

  public static RoomModelOpVertexCreate createVertex(
    final Vector2I in_position)
  {
    return new RoomModelOpVertexCreate(in_position);
  }

  @Override
  public RoomPolyVertexType evaluate(
    final RoomModelType model)
  {
    notNull(model, "Model");

    LOG.debug("createVertex: {}", this.position);
    return model.vertexCreate(this.position);
  }

  @Override
  public void undo(
    final RoomModelType model)
  {
    throw new UnimplementedCodeException();
  }
}
