package com.io7m.roommodel0.mesh;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.roommodel0.RoomPolygonType;

import java.util.List;

public interface MeshEditingPolygonCreatorType
{
  boolean addVertex(Vector2I position);

  PolygonType create();

  List<Vector2I> vertices();
}
