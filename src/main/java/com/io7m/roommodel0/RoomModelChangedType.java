package com.io7m.roommodel0;

import org.immutables.value.Value;

@RoomImmutableStyleType
@Value.Immutable
public interface RoomModelChangedType
{
  @Value.Parameter
  boolean isUndoAvailable();
}
