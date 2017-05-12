package com.io7m.roommodel0;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.io7m.jnull.NullCheck.notNull;

public final class RoomModelOpPolygonCreate implements RoomModelOpType<RoomPolygonType>
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(RoomModelOpPolygonCreate.class);
  }

  private final List<RoomPolyVertexType> vertices;
  private RoomPolygonType polygon;

  public RoomModelOpPolygonCreate(
    final List<RoomPolyVertexType> in_vertices)
  {
    this.vertices = notNull(in_vertices, "Vertices");
  }

  public static RoomModelOpPolygonCreate createPolygon(
    final List<RoomPolyVertexType> in_vertices)
  {
    return new RoomModelOpPolygonCreate(in_vertices);
  }

  @Override
  public RoomPolygonType evaluate(
    final RoomModelType model)
  {
    notNull(model, "Model");

    LOG.debug("createPolygon: {}", this.vertices);

    this.polygon = model.polygonCreate(this.vertices);
    return this.polygon;
  }

  @Override
  public void undo(
    final RoomModelType model)
  {
    notNull(model, "Model");
    model.polygonDelete(this.polygon);
  }
}
