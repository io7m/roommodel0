package com.io7m.roommodel0;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.util.List;

import static com.io7m.jnull.NullCheck.notNull;

public final class RoomModelOpIterate<A> implements RoomModelOpType<List<A>>
{
  private final List<RoomModelOpType<A>> ops;
  private int evaluated;

  public RoomModelOpIterate(
    final List<RoomModelOpType<A>> ops)
  {
    this.ops = notNull(ops, "Ops");
    this.evaluated = 0;
  }

  public static <A> RoomModelOpIterate<A> iterate(
    final List<RoomModelOpType<A>> ops)
  {
    return new RoomModelOpIterate<>(ops);
  }

  @Override
  public List<A> evaluate(
    final RoomModelType model)
  {
    notNull(model, "Model");

    final ReferenceArrayList<A> results =
      new ReferenceArrayList<>(this.ops.size());
    for (this.evaluated = 0; this.evaluated < this.ops.size(); ++this.evaluated) {
      results.add(this.ops.get(this.evaluated).evaluate(model));
    }

    return results;
  }

  @Override
  public void undo(
    final RoomModelType model)
  {
    notNull(model, "Model");

    --this.evaluated;
    for (; this.evaluated >= 0; --this.evaluated) {
      this.ops.get(this.evaluated).undo(model);
    }
  }
}
