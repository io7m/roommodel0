package com.io7m.roommodel0;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface RoomModelType
  extends RoomModelReadableType, RoomModelDebuggingType
{
  RoomPolyVertexType vertexCreate(
    Vector2I position);

  RoomPolyVertexType vertexCreateWithID(
    RoomPolyVertexID vertex,
    Vector2I position);

  void vertexSetPosition(
    RoomPolyVertexID vertex,
    Vector2I position);

  RoomPolygonType polygonCreate(
    List<RoomPolyVertexID> vertices);

  RoomPolygonType polygonCreateWithID(
    RoomPolygonID id,
    List<RoomPolyVertexID> vertices);

  void polygonDelete(
    RoomPolygonID polygon);

  default RoomPolygonType polygonCreateV(
    final RoomPolyVertexID... vertices)
  {
    return this.polygonCreate(Arrays.asList(vertices));
  }

  default RoomPolygonType polygonCreateWithIDV(
    final RoomPolygonID polygon,
    final RoomPolyVertexID... vertices)
  {
    return this.polygonCreateWithID(polygon, Arrays.asList(vertices));
  }

  default RoomPolygonType polygonCreateVV(
    final RoomPolyVertexType... vertices)
  {
    return this.polygonCreate(
      Arrays.asList(vertices).stream()
        .map(RoomPolyVertexType::id)
        .collect(Collectors.toList()));
  }

  default RoomPolygonType polygonCreateWithIDVV(
    final RoomPolygonID polygon,
    final RoomPolyVertexType... vertices)
  {
    return this.polygonCreateWithID(
      polygon,
      Arrays.asList(vertices).stream()
        .map(RoomPolyVertexType::id)
        .collect(Collectors.toList()));
  }
}
