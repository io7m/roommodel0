package com.io7m.roommodel0;

import com.io7m.jnull.NullCheck;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.util.List;
import java.util.Optional;

public final class RoomModelOpExecutor implements RoomModelOpExecutorType
{
  private final int stack_max;
  private final RoomModelType model;
  private final PublishSubject<RoomModelChanged> observable;
  private List<RoomModelOpType<?>> undo_stack;

  public RoomModelOpExecutor(
    final RoomModelType in_model,
    final int in_stack_max)
  {
    if (in_stack_max < 1) {
      throw new IllegalArgumentException("Stack size must be >= 1");
    }

    this.model = NullCheck.notNull(in_model, "Model");
    this.undo_stack = new ReferenceArrayList<>(in_stack_max);
    this.observable = PublishSubject.create();
    this.stack_max = in_stack_max;
  }

  @Override
  public RoomModelReadableType model()
  {
    return this.model;
  }

  @Override
  public <T> T evaluate(
    final RoomModelOpType<T> op)
  {
    NullCheck.notNull(op, "Op");

    final T result = op.evaluate(this.model);
    if (this.undoStackSize() == this.stack_max) {
      this.undo_stack.remove(0);
    }

    this.undo_stack.add(op);
    this.observable.onNext(RoomModelChanged.of(this.availability()));
    return result;
  }

  private Optional<RoomModelUndoAvailability> availability()
  {
    if (this.undoAvailable()) {
      return Optional.of(RoomModelUndoAvailability.of(
        this.undo_stack.get(this.undoStackSize() - 1).description()));
    }
    return Optional.empty();
  }

  @Override
  public Observable<RoomModelChanged> observable()
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
      final RoomModelOpType<?> op = this.undo_stack.get(last);
      op.undo(this.model);
      this.undo_stack.remove(last);
      this.observable.onNext(RoomModelChanged.of(this.availability()));
    }
  }
}
