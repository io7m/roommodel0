package com.io7m.roommodel0.mesh;

import com.io7m.jregions.core.unparameterized.areas.AreaL;

import java.util.List;

public interface PolygonType
{
  PolygonID id();

  AreaL bounds();

  boolean deleted();

  List<PolygonEdgeType> edges();

  List<PolygonVertexType> vertices();
}
