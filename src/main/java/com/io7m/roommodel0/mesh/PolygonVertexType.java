package com.io7m.roommodel0.mesh;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.roommodel0.RoomPolyVertexID;

import java.util.Set;

public interface PolygonVertexType
{
  PolygonVertexID id();

  boolean deleted();

  Set<PolygonType> polygons();

  Vector2I position();
}
