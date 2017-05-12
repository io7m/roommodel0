package com.io7m.roommodel0;

import static com.io7m.jnull.NullCheck.notNull;

public final class RoomModelOpIdentity<T> implements RoomModelOpType<T>
{
  private final T value;

  public RoomModelOpIdentity(
    final T value)
  {
    this.value = notNull(value, "Value");
  }

  public static <T> RoomModelOpIdentity<T> identity(
    final T value)
  {
    return new RoomModelOpIdentity<>(value);
  }

  @Override
  public T evaluate(
    final RoomModelType model)
  {
    notNull(model, "Model");
    return this.value;
  }

  @Override
  public void undo(
    final RoomModelType model)
  {
    notNull(model, "Model");
  }
}
