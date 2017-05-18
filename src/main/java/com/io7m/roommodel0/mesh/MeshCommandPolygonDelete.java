package com.io7m.roommodel0.mesh;

import com.io7m.jfunctional.Unit;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.io7m.jnull.NullCheck.notNull;

public final class MeshCommandPolygonDelete implements MeshCommandType<Unit>
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(MeshCommandPolygonDelete.class);
  }

  private PolygonType polygon;

  public MeshCommandPolygonDelete(
    final PolygonType in_polygon)
  {
    this.polygon = notNull(in_polygon, "Polygon");
  }

  public static MeshCommandPolygonDelete deletePolygon(
    final PolygonType in_polygon)
  {
    return new MeshCommandPolygonDelete(in_polygon);
  }

  @Override
  public String description()
  {
    return "Delete Polygon " + this.polygon.id().value();
  }

  @Override
  public Unit evaluate(
    final MeshType mesh)
  {
    notNull(mesh, "Mesh");

    LOG.debug("delete {}", this.polygon);
    mesh.polygonDelete(this.polygon.id());
    LOG.debug("deleted {}", this.polygon);
    return Unit.unit();
  }

  @Override
  public void undo(
    final MeshType mesh)
  {
    notNull(mesh, "Mesh");

    LOG.debug("undo delete {}", this.polygon);

    final ReferenceArrayList<PolygonVertexID> vertices =
      new ReferenceArrayList<>(this.polygon.vertices().size());
    for (final PolygonVertexType v : this.polygon.vertices()) {
      final Optional<PolygonVertexType> exist_opt =
        mesh.vertexFind(v.position());
      if (!exist_opt.isPresent()) {
        vertices.add(mesh.vertexCreateWithID(v.id(), v.position()).id());
      } else {
        vertices.add(exist_opt.get().id());
      }
    }

    final PolygonType recreated =
      mesh.polygonCreateWithID(this.polygon.id(), vertices);
    LOG.debug("created {}", recreated);
  }
}
