package com.io7m.roommodel0;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.jnull.NullCheck.notNull;
import static com.io7m.roommodel0.RoomModelOpBind.bind;
import static com.io7m.roommodel0.RoomModelOpIdentity.identity;
import static com.io7m.roommodel0.RoomModelOpIterate.iterate;
import static com.io7m.roommodel0.RoomModelOpVertexCreate.createVertex;

public final class RoomEditingModel implements RoomEditingModelType
{
  private final RoomModelOpExecutorType exec;

  private RoomEditingModel(
    final RoomModelOpExecutorType in_model)
  {
    this.exec = notNull(in_model, "Executor");
  }

  public static RoomEditingModelType create(
    final RoomModelOpExecutorType in_exec)
  {
    return new RoomEditingModel(in_exec);
  }

  @Override
  public RoomEditingPolygonCreatorType polygonCreate()
  {
    return new PolygonCreator(this.exec);
  }

  private static final class PolygonCreator
    implements RoomEditingPolygonCreatorType
  {
    private final RoomModelOpExecutorType exec;
    private List<RoomModelOpType<RoomPolyVertexType>> ops;
    private List<Vector2I> positions;

    PolygonCreator(
      final RoomModelOpExecutorType in_exec)
    {
      this.exec = notNull(in_exec, "Executor");
      this.ops = new ReferenceArrayList<>();
      this.positions = new ReferenceArrayList<>();
    }

    @Override
    public boolean addVertex(
      final Vector2I position)
    {
      notNull(position, "Position");

      if (!this.positions.isEmpty()) {
        if (Objects.equals(this.positions.get(0), position)) {
          return true;
        }
      }

      final Optional<RoomPolyVertexType> exist_opt =
        this.exec.model().vertexFind(position);
      if (exist_opt.isPresent()) {
        this.ops.add(identity(exist_opt.get()));
      } else {
        this.ops.add(createVertex(position));
      }
      this.positions.add(position);
      return false;
    }

    @Override
    public RoomPolygonType create()
    {
      try {
        return this.exec.evaluate(
          bind(iterate(this.ops), RoomModelOpPolygonCreate::createPolygon));
      } finally {
        this.positions.clear();
        this.ops.clear();
      }
    }

    @Override
    public List<Vector2I> vertices()
    {
      return this.positions;
    }
  }
}
