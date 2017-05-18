package com.io7m.roommodel0.undo;

public interface UndoCommandType<A, B>
{
  String description();

  B evaluate(A input);

  void undo(A input);
}
