package com.io7m.roommodel0;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;

import java.util.Arrays;
import java.util.List;

public interface RoomModelType extends RoomModelReadableType,
  RoomModelDebuggingType
{
  RoomPolyVertexType vertexCreate(
    Vector2I position);

  RoomPolygonType polygonCreate(
    List<RoomPolyVertexType> vertices);

  void polygonDelete(
    RoomPolygonType polygon);

  default RoomPolygonType polygonCreateV(
    final RoomPolyVertexType... vertices)
  {
    return this.polygonCreate(Arrays.asList(vertices));
  }
}
