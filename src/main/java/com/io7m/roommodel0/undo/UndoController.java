package com.io7m.roommodel0.undo;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jnull.NullCheck;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.util.List;
import java.util.Optional;

public final class UndoController<A> implements UndoControllerType<A>
{
  private final int stack_max;
  private final A state;
  private final PublishSubject<UndoAvailability> observable;
  private List<UndoCommandType<A, ?>> undo_stack;

  public UndoController(
    final A in_state,
    final int in_stack_max)
  {
    if (in_stack_max < 1) {
      throw new IllegalArgumentException("Stack size must be >= 1");
    }

    this.state = NullCheck.notNull(in_state, "State");
    this.undo_stack = new ReferenceArrayList<>(in_stack_max);
    this.observable = PublishSubject.create();
    this.stack_max = in_stack_max;
  }

  private UndoCommandType<?, ?> currentOp()
  {
    Preconditions.checkPrecondition(
      this.undoAvailable(),
      "Undo must be available");
    return this.undo_stack.get(this.undoStackSize() - 1);
  }

  private UndoAvailability availability()
  {
    final Optional<String> description;
    if (this.undoAvailable()) {
      description = Optional.of(this.currentOp().description());
    } else {
      description = Optional.empty();
    }

    return UndoAvailability.of(this.undoStackSize(), description);
  }

  @Override
  public <B> B evaluate(
    final UndoCommandType<A, B> op)
  {
    NullCheck.notNull(op, "Op");

    final B result = op.evaluate(this.state);
    if (this.undoStackSize() == this.stack_max) {
      this.undo_stack.remove(0);
    }

    this.undo_stack.add(op);
    this.observable.onNext(this.availability());
    return result;
  }

  @Override
  public A state()
  {
    return this.state;
  }

  @Override
  public Observable<UndoAvailability> observable()
  {
    return this.observable;
  }

  @Override
  public int undoStackSize()
  {
    return this.undo_stack.size();
  }

  @Override
  public boolean undoAvailable()
  {
    return this.undoStackSize() >= 1;
  }

  @Override
  public void undo()
  {
    if (this.undoAvailable()) {
      final int last = this.undoStackSize() - 1;
      final UndoCommandType<A, ?> op = this.undo_stack.get(last);
      op.undo(this.state);
      this.undo_stack.remove(last);
      this.observable.onNext(this.availability());
    }
  }
}
