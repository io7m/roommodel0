package com.io7m.roommodel0;

import com.io7m.jfunctional.Unit;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.io7m.jnull.NullCheck.notNull;

public final class RoomModelOpPolygonDelete implements RoomModelOpType<Unit>
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(RoomModelOpPolygonDelete.class);
  }

  private RoomPolygonType polygon;

  public RoomModelOpPolygonDelete(
    final RoomPolygonType in_polygon)
  {
    this.polygon = notNull(in_polygon, "Polygon");
  }

  public static RoomModelOpPolygonDelete deletePolygon(
    final RoomPolygonType in_polygon)
  {
    return new RoomModelOpPolygonDelete(in_polygon);
  }

  @Override
  public String description()
  {
    return "Delete Polygon " + this.polygon.id().value();
  }

  @Override
  public Unit evaluate(
    final RoomModelType model)
  {
    notNull(model, "Model");

    LOG.debug("delete {}", this.polygon);
    model.polygonDelete(this.polygon.id());
    LOG.debug("deleted {}", this.polygon);
    return Unit.unit();
  }

  @Override
  public void undo(
    final RoomModelType model)
  {
    notNull(model, "Model");

    LOG.debug("undo delete {}", this.polygon);

    final ReferenceArrayList<RoomPolyVertexID> vertices =
      new ReferenceArrayList<>(this.polygon.vertices().size());
    for (final RoomPolyVertexType v : this.polygon.vertices()) {
      final Optional<RoomPolyVertexType> exist_opt =
        model.vertexFind(v.position());
      if (!exist_opt.isPresent()) {
        vertices.add(model.vertexCreateWithID(v.id(), v.position()).id());
      } else {
        vertices.add(exist_opt.get().id());
      }
    }

    final RoomPolygonType recreated =
      model.polygonCreateWithID(this.polygon.id(), vertices);
    LOG.debug("created {}", recreated);
  }
}
