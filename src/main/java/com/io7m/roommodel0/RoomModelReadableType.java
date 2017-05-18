package com.io7m.roommodel0;

import com.io7m.jspatial.api.quadtrees.QuadTreeReadableLType;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;

import java.util.Collection;
import java.util.Optional;

public interface RoomModelReadableType
{
  Collection<RoomPolygonType> polygons();

  QuadTreeReadableLType<RoomPolygonType> polygonTree();

  Optional<RoomPolyVertexType> vertexFind(Vector2I position);

  Optional<RoomPolygonType> polygonFind(Vector2I position);

  Collection<RoomPolyVertexType> vertices();
}
