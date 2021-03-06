package com.io7m.roommodel0.mesh;

import com.io7m.jspatial.api.quadtrees.QuadTreeReadableIType;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;

import java.util.Collection;
import java.util.Optional;

public interface MeshReadableType
{
  Collection<PolygonType> polygons();

  QuadTreeReadableIType<PolygonType> polygonTree();

  Optional<PolygonVertexType> vertexFind(
    Vector2I position);

  Optional<PolygonType> polygonFind(
    Vector2I position);

  Collection<PolygonVertexType> vertices();
}
