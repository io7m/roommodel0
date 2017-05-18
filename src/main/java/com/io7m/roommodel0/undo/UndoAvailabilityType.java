package com.io7m.roommodel0.undo;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.roommodel0.RoomImmutableStyleType;
import org.immutables.value.Value;

import java.util.Optional;

@RoomImmutableStyleType
@Value.Immutable
public interface UndoAvailabilityType
{
  @Value.Parameter
  int undoStackSize();

  @Value.Parameter
  Optional<String> undoOperation();

  default boolean undoAvailable()
  {
    return this.undoStackSize() > 0;
  }

  @Value.Check
  default void checkPreconditions()
  {
    Preconditions.checkPrecondition(
      this.undoAvailable() == this.undoOperation().isPresent(),
      "Undo description must be present");
  }
}
