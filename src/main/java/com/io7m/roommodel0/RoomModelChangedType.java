package com.io7m.roommodel0;

import org.immutables.value.Value;

import java.util.Optional;

@RoomImmutableStyleType
@Value.Immutable
public interface RoomModelChangedType
{
  @Value.Parameter
  Optional<RoomModelUndoAvailability> undoAvailable();
}
