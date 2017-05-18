package com.io7m.roommodel0.mesh;

import com.io7m.roommodel0.RoomImmutableStyleType;
import org.immutables.value.Value;

@RoomImmutableStyleType
@Value.Immutable
public interface PolygonVertexIDType
{
  @Value.Parameter
  long value();
}
