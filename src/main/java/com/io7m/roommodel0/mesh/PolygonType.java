package com.io7m.roommodel0.mesh;

import com.io7m.jregions.core.unparameterized.areas.AreaI;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;

import java.util.List;
import java.util.stream.Collectors;

public interface PolygonType
{
  PolygonID id();

  AreaI bounds();

  boolean deleted();

  List<PolygonEdgeType> edges();

  List<PolygonVertexType> vertices();

  default List<Vector2I> positions()
  {
    return this.vertices()
      .stream()
      .map(PolygonVertexType::position)
      .collect(Collectors.toList());
  }
}
