package com.io7m.roommodel0;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;

import java.util.List;

public interface RoomEditingPolygonCreatorType
{
  boolean addVertex(Vector2I position);

  RoomPolygonType create();

  List<Vector2I> vertices();
}
