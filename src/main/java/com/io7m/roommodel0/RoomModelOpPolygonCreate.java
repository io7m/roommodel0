package com.io7m.roommodel0;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public final class RoomModelOpPolygonCreate implements RoomModelOpType<RoomPolygonType>
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(RoomModelOpPolygonCreate.class);
  }

  private final ReferenceArrayList<Vector2I> positions;
  private RoomPolygonID polygon;

  RoomModelOpPolygonCreate(
    final List<Vector2I> in_positions)
  {
    this.positions = new ReferenceArrayList<>(in_positions);
  }

  @Override
  public String description()
  {
    return "Create Polygon " + this.polygon.value();
  }

  @Override
  public RoomPolygonType evaluate(
    final RoomModelType model)
  {
    LOG.debug("create {}", this.positions);

    final ReferenceArrayList<RoomPolyVertexID> vertices =
      new ReferenceArrayList<>(this.positions.size());
    for (final Vector2I position : this.positions) {
      final Optional<RoomPolyVertexType> exists_opt =
        model.vertexFind(position);
      if (exists_opt.isPresent()) {
        vertices.add(exists_opt.get().id());
      } else {
        vertices.add(model.vertexCreate(position).id());
      }
    }

    final RoomPolygonType p = model.polygonCreate(vertices);
    LOG.debug("created {}", p.id());
    this.polygon = p.id();
    return p;
  }

  @Override
  public void undo(
    final RoomModelType model)
  {
    LOG.debug("undo create {}", this.polygon);
    model.polygonDelete(this.polygon);
    LOG.debug("deleted {}", this.polygon);
  }
}
