package com.io7m.roommodel0.undo;

import io.reactivex.Observable;

public interface UndoControllerType<A>
{
  <B> B evaluate(
    UndoCommandType<A, B> op);

  A state();

  Observable<UndoAvailability> observable();

  int undoStackSize();

  boolean undoAvailable();

  void undo();
}
