package com.io7m.roommodel0;

import java.util.function.Function;

import static com.io7m.jnull.NullCheck.notNull;

public final class RoomModelOpBind<A, B> implements RoomModelOpType<B>
{
  private final RoomModelOpType<A> op;
  private final Function<A, RoomModelOpType<B>> supplier;
  private RoomModelOpType<B> inter_op;

  public RoomModelOpBind(
    final RoomModelOpType<A> op,
    final Function<A, RoomModelOpType<B>> supplier)
  {
    this.op = notNull(op, "Op");
    this.supplier = notNull(supplier, "Supplier");
  }

  public static <A, B> RoomModelOpType<B> bind(
    final RoomModelOpType<A> op,
    final Function<A, RoomModelOpType<B>> f)
  {
    return new RoomModelOpBind<>(op, f);
  }

  @Override
  public B evaluate(
    final RoomModelType model)
  {
    notNull(model, "Model");

    final A x = this.op.evaluate(model);
    this.inter_op = this.supplier.apply(x);
    return this.inter_op.evaluate(model);
  }

  @Override
  public void undo(
    final RoomModelType model)
  {
    notNull(model, "Model");

    this.inter_op.undo(model);
    this.op.undo(model);
  }
}
