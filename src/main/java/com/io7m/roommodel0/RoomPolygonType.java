package com.io7m.roommodel0;

import com.io7m.jregions.core.unparameterized.areas.AreaL;

import java.util.List;

public interface RoomPolygonType
{
  RoomPolygonID id();

  AreaL bounds();

  boolean deleted();

  List<RoomPolyEdgeType> edges();

  List<RoomPolyVertexType> vertices();
}
