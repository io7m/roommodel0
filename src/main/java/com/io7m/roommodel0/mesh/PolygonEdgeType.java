package com.io7m.roommodel0.mesh;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2D;

import java.util.Set;

public interface PolygonEdgeType
{
  Set<PolygonType> polygons();

  PolygonVertexType vertex0();

  PolygonVertexType vertex1();

  boolean isExternal();

  Vector2D normal();
}
