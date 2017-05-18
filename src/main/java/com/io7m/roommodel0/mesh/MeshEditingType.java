package com.io7m.roommodel0.mesh;

public interface MeshEditingType
{
  MeshEditingPolygonCreatorType polygonCreate();

  MeshEditingVertexMoverType vertexMove();

  boolean polygonDelete(
    int x,
    int y);
}
