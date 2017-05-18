package com.io7m.roommodel0.mesh;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public final class MeshCommandPolygonCreate implements MeshCommandType<PolygonType>
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(MeshCommandPolygonCreate.class);
  }

  private final ReferenceArrayList<Vector2I> positions;
  private PolygonID polygon;

  MeshCommandPolygonCreate(
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
  public PolygonType evaluate(
    final MeshType mesh)
  {
    LOG.debug("create {}", this.positions);

    final ReferenceArrayList<PolygonVertexID> vertices =
      new ReferenceArrayList<>(this.positions.size());
    for (final Vector2I position : this.positions) {
      final Optional<PolygonVertexType> exists_opt =
        mesh.vertexFind(position);
      if (exists_opt.isPresent()) {
        vertices.add(exists_opt.get().id());
      } else {
        vertices.add(mesh.vertexCreate(position).id());
      }
    }

    final PolygonType p = mesh.polygonCreate(vertices);
    LOG.debug("created {}", p.id());
    this.polygon = p.id();
    return p;
  }

  @Override
  public void undo(
    final MeshType mesh)
  {
    LOG.debug("undo create {}", this.polygon);
    mesh.polygonDelete(this.polygon);
    LOG.debug("deleted {}", this.polygon);
  }
}
