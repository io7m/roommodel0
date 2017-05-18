package com.io7m.roommodel0.mesh;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface MeshType
  extends MeshReadableType, MeshDebuggingType
{
  PolygonVertexType vertexCreate(
    Vector2I position);

  PolygonVertexType vertexCreateWithID(
    PolygonVertexID vertex,
    Vector2I position);

  void vertexSetPosition(
    PolygonVertexID vertex,
    Vector2I position);

  PolygonType polygonCreate(
    List<PolygonVertexID> vertices);

  PolygonType polygonCreateWithID(
    PolygonID id,
    List<PolygonVertexID> vertices);

  void polygonDelete(
    PolygonID polygon);

  default PolygonType polygonCreateV(
    final PolygonVertexID... vertices)
  {
    return this.polygonCreate(Arrays.asList(vertices));
  }

  default PolygonType polygonCreateWithIDV(
    final PolygonID polygon,
    final PolygonVertexID... vertices)
  {
    return this.polygonCreateWithID(polygon, Arrays.asList(vertices));
  }

  default PolygonType polygonCreateVV(
    final PolygonVertexType... vertices)
  {
    return this.polygonCreate(
      Arrays.asList(vertices).stream()
        .map(PolygonVertexType::id)
        .collect(Collectors.toList()));
  }

  default PolygonType polygonCreateWithIDVV(
    final PolygonID polygon,
    final PolygonVertexType... vertices)
  {
    return this.polygonCreateWithID(
      polygon,
      Arrays.asList(vertices).stream()
        .map(PolygonVertexType::id)
        .collect(Collectors.toList()));
  }
}
