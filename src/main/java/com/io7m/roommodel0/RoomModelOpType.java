package com.io7m.roommodel0;

public interface RoomModelOpType<T>
{
  T evaluate(
    RoomModelType model);

  void undo(
    RoomModelType model);
}
