package com.io7m.roommodel0;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;

import java.util.Set;

public interface RoomPolyVertexType
{
  long id();

  Set<RoomPolygonType> polygons();

  Vector2I position();
}
