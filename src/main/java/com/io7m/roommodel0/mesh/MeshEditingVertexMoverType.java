package com.io7m.roommodel0.mesh;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;

import java.util.List;

public interface MeshEditingVertexMoverType
{
  boolean isVertexSelected();

  void setVertexPosition(Vector2I position);

  void commit();

  boolean isVertexOK();

  boolean selectVertex(Vector2I position);

  List<TemporaryPolygonType> temporaryPolygons();

  interface TemporaryVertexType
  {
    Vector2I position();
  }

  interface TemporaryPolygonType
  {
    List<TemporaryVertexType> vertices();

    boolean isConvex();
  }
}
