package com.io7m.roommodel0;

import com.io7m.jregions.core.unparameterized.areas.AreaL;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2D;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;

import java.util.Set;

public interface RoomPolyEdgeType
{
  Set<RoomPolygonType> polygons();

  RoomPolyVertexType vertex0();

  RoomPolyVertexType vertex1();

  boolean isExternal();

  Vector2D normal();

  default AreaL bounds()
  {
    final Vector2I v0_pos = this.vertex0().position();
    final Vector2I v1_pos = this.vertex1().position();
    return AreaL.of(
      Math.min((long) v0_pos.x(), (long) v1_pos.x()),
      Math.max((long) v0_pos.x(), (long) v1_pos.x()),
      Math.min((long) v0_pos.y(), (long) v1_pos.y()),
      Math.max((long) v0_pos.y(), (long) v1_pos.y()));
  }
}
