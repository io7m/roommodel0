package com.io7m.roommodel0;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2D;

import java.util.List;
import java.util.Set;

public interface RoomPolyEdgeType
{
  Set<RoomPolygonType> polygons();

  RoomPolyVertexType vertex0();

  RoomPolyVertexType vertex1();

  boolean isExternal();

  Vector2D normal();
}
