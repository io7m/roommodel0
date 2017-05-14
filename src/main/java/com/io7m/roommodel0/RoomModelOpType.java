package com.io7m.roommodel0;

public interface RoomModelOpType<T>
{
  String description();

  T evaluate(
    RoomModelType model);

  void undo(
    RoomModelType model);
}
