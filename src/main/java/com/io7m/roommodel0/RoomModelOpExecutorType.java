package com.io7m.roommodel0;

import io.reactivex.Observable;

public interface RoomModelOpExecutorType
{
  RoomModelReadableType model();

  <T> T evaluate(
    RoomModelOpType<T> op);

  Observable<RoomModelChanged> observable();

  int undoStackSize();

  boolean undoAvailable();

  void undo();
}
