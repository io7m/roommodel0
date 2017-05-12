package com.io7m.roommodel0;

import com.io7m.jspatial.api.quadtrees.QuadTreeReadableLType;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;

import java.util.Optional;
import java.util.Set;

public interface RoomModelReadableType
{
  Set<RoomPolygonType> polygons();

  QuadTreeReadableLType<RoomPolygonType> polygonTree();

  Optional<RoomPolyVertexType> vertexFind(Vector2I position);
}
