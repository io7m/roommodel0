package com.io7m.roommodel0;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;

import java.util.Set;

public interface RoomPolyVertexType
{
  RoomPolyVertexID id();

  boolean deleted();

  Set<RoomPolygonType> polygons();

  Vector2I position();
}
