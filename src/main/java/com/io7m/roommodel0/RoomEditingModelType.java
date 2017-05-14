package com.io7m.roommodel0;

public interface RoomEditingModelType
{
  RoomEditingPolygonCreatorType polygonCreate();

  RoomEditingVertexMoverType vertexMove();

  boolean polygonDelete(
    int x,
    int y);
}
