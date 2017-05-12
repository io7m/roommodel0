package com.io7m.roommodel0.tests;

import com.io7m.jregions.core.unparameterized.areas.AreaL;
import com.io7m.jregions.core.unparameterized.areas.AreasL;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.roommodel0.RoomModelExceptionPolygonNonexistent;
import com.io7m.roommodel0.RoomModelExceptionPolygonNotConvex;
import com.io7m.roommodel0.RoomModelExceptionPolygonOutsideBounds;
import com.io7m.roommodel0.RoomModelExceptionPolygonTooFewVertices;
import com.io7m.roommodel0.RoomModelExceptionVertexNonexistent;
import com.io7m.roommodel0.RoomModelType;
import com.io7m.roommodel0.RoomPolyEdgeType;
import com.io7m.roommodel0.RoomPolyVertexType;
import com.io7m.roommodel0.RoomPolygonType;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class RoomModelContract
{
  @Rule public final ExpectedException expected = ExpectedException.none();

  protected abstract Logger log();

  protected abstract RoomModelType emptyModel();

  @Test
  public final void testCreatePolygon()
  {
    final RoomModelType model = this.emptyModel();

    final RoomPolyVertexType v0 = model.vertexCreate(Vector2I.of(0, 1));
    final RoomPolyVertexType v1 = model.vertexCreate(Vector2I.of(0, 0));
    final RoomPolyVertexType v2 = model.vertexCreate(Vector2I.of(1, 0));

    final RoomPolygonType poly = model.polygonCreateV(v0, v1, v2);

    Assert.assertTrue(model.polygons().contains(poly));

    {
      final List<RoomPolyVertexType> vs = poly.vertices();
      Assert.assertEquals(3L, (long) vs.size());

      Assert.assertEquals(v0, vs.get(0));
      Assert.assertEquals(v1, vs.get(1));
      Assert.assertEquals(v2, vs.get(2));
    }

    {
      final List<RoomPolyEdgeType> es = poly.edges();
      Assert.assertEquals(3L, (long) es.size());

      final RoomPolyEdgeType e0 = es.get(0);
      Assert.assertTrue(e0.isExternal());
      Assert.assertEquals(v0, e0.vertex0());
      Assert.assertEquals(v1, e0.vertex1());
      Assert.assertEquals(1L, (long) e0.polygons().size());
      Assert.assertTrue(e0.polygons().contains(poly));

      final RoomPolyEdgeType e1 = es.get(1);
      Assert.assertTrue(e1.isExternal());
      Assert.assertEquals(v1, e1.vertex0());
      Assert.assertEquals(v2, e1.vertex1());
      Assert.assertEquals(1L, (long) e1.polygons().size());
      Assert.assertTrue(e1.polygons().contains(poly));

      final RoomPolyEdgeType e2 = es.get(2);
      Assert.assertTrue(e2.isExternal());
      Assert.assertEquals(v2, e2.vertex0());
      Assert.assertEquals(v0, e2.vertex1());
      Assert.assertEquals(1L, (long) e2.polygons().size());
      Assert.assertTrue(e2.polygons().contains(poly));
    }
  }

  @Test
  public final void testCreatePolygonJoined()
  {
    final RoomModelType model = this.emptyModel();

    final RoomPolyVertexType v0 = model.vertexCreate(Vector2I.of(0, 1));
    final RoomPolyVertexType v1 = model.vertexCreate(Vector2I.of(0, 0));
    final RoomPolyVertexType v2 = model.vertexCreate(Vector2I.of(1, 0));
    final RoomPolyVertexType v3 = model.vertexCreate(Vector2I.of(1, 1));

    final RoomPolygonType poly0 = model.polygonCreateV(v0, v1, v2);
    final RoomPolygonType poly1 = model.polygonCreateV(v0, v2, v3);

    Assert.assertTrue(model.polygons().contains(poly0));
    Assert.assertTrue(model.polygons().contains(poly1));

    {
      final List<RoomPolyVertexType> vs = poly0.vertices();
      Assert.assertEquals(3L, (long) vs.size());

      Assert.assertEquals(v0, vs.get(0));
      Assert.assertEquals(v1, vs.get(1));
      Assert.assertEquals(v2, vs.get(2));
    }

    {
      final List<RoomPolyEdgeType> es = poly0.edges();
      Assert.assertEquals(3L, (long) es.size());

      final RoomPolyEdgeType e0 = es.get(0);
      Assert.assertTrue(e0.isExternal());
      Assert.assertEquals(v0, e0.vertex0());
      Assert.assertEquals(v1, e0.vertex1());
      Assert.assertEquals(1L, (long) e0.polygons().size());
      Assert.assertTrue(e0.polygons().contains(poly0));

      final RoomPolyEdgeType e1 = es.get(1);
      Assert.assertTrue(e1.isExternal());
      Assert.assertEquals(v1, e1.vertex0());
      Assert.assertEquals(v2, e1.vertex1());
      Assert.assertEquals(1L, (long) e1.polygons().size());
      Assert.assertTrue(e1.polygons().contains(poly0));

      final RoomPolyEdgeType e2 = es.get(2);
      Assert.assertFalse(e2.isExternal());
      Assert.assertEquals(v2, e2.vertex0());
      Assert.assertEquals(v0, e2.vertex1());
      Assert.assertEquals(2L, (long) e2.polygons().size());
      Assert.assertTrue(e2.polygons().contains(poly0));
      Assert.assertTrue(e2.polygons().contains(poly1));
    }

    {
      final List<RoomPolyVertexType> vs = poly1.vertices();
      Assert.assertEquals(3L, (long) vs.size());

      Assert.assertEquals(v0, vs.get(0));
      Assert.assertEquals(v2, vs.get(1));
      Assert.assertEquals(v3, vs.get(2));
    }

    {
      final List<RoomPolyEdgeType> es = poly1.edges();
      Assert.assertEquals(3L, (long) es.size());

      final RoomPolyEdgeType e0 = es.get(0);
      Assert.assertFalse(e0.isExternal());
      Assert.assertEquals(v2, e0.vertex0());
      Assert.assertEquals(v0, e0.vertex1());
      Assert.assertEquals(2L, (long) e0.polygons().size());
      Assert.assertTrue(e0.polygons().contains(poly0));
      Assert.assertTrue(e0.polygons().contains(poly1));

      final RoomPolyEdgeType e1 = es.get(1);
      Assert.assertTrue(e1.isExternal());
      Assert.assertEquals(v2, e1.vertex0());
      Assert.assertEquals(v3, e1.vertex1());
      Assert.assertEquals(1L, (long) e1.polygons().size());
      Assert.assertTrue(e1.polygons().contains(poly1));

      final RoomPolyEdgeType e2 = es.get(2);
      Assert.assertTrue(e2.isExternal());
      Assert.assertEquals(v3, e2.vertex0());
      Assert.assertEquals(v0, e2.vertex1());
      Assert.assertEquals(1L, (long) e2.polygons().size());
      Assert.assertTrue(e2.polygons().contains(poly1));
    }

    this.checkModel(model);
  }

  private void checkModel(
    final RoomModelType model)
  {
    final List<String> errors = model.check();
    errors.forEach(e -> this.log().error("{}", e));
    Assert.assertTrue(errors.isEmpty());
  }

  @Test
  public final void testCreatePolygonNonexistent()
  {
    final RoomModelType model = this.emptyModel();

    final RoomPolyVertexType v0 = model.vertexCreate(Vector2I.of(0, 1));
    final RoomPolyVertexType v1 = model.vertexCreate(Vector2I.of(0, 0));
    final RoomPolyVertexType v2 = new RoomPolyVertexType()
    {
      @Override
      public long id()
      {
        return Long.MAX_VALUE;
      }

      @Override
      public Set<RoomPolygonType> polygons()
      {
        throw new UnreachableCodeException();
      }

      @Override
      public Vector2I position()
      {
        throw new UnreachableCodeException();
      }
    };

    this.expected.expect(RoomModelExceptionVertexNonexistent.class);
    this.expected.expectMessage(StringContains.containsString("does not belong to this model"));
    model.polygonCreateV(v0, v1, v2);
  }

  @Test
  public final void testCreatePolygonNonConvex()
  {
    final RoomModelType model = this.emptyModel();

    final RoomPolyVertexType v0 = model.vertexCreate(Vector2I.of(0, 1));
    final RoomPolyVertexType v1 = model.vertexCreate(Vector2I.of(0, 0));
    final RoomPolyVertexType v2 = model.vertexCreate(Vector2I.of(1, 1));
    final RoomPolyVertexType v3 = model.vertexCreate(Vector2I.of(1, 0));

    this.expected.expect(RoomModelExceptionPolygonNotConvex.class);
    this.expected.expectMessage(StringContains.containsString("not convex"));
    model.polygonCreateV(v0, v1, v2, v3);
  }

  @Test
  public final void testCreatePolygonTooLarge()
  {
    final RoomModelType model = this.emptyModel();

    final RoomPolyVertexType v0 = model.vertexCreate(Vector2I.of(-1000000, 1000000));
    final RoomPolyVertexType v1 = model.vertexCreate(Vector2I.of(-1000000, -1000000));
    final RoomPolyVertexType v2 = model.vertexCreate(Vector2I.of(1000000, 1000000));

    this.expected.expect(RoomModelExceptionPolygonOutsideBounds.class);
    this.expected.expectMessage(StringContains.containsString("fit"));
    model.polygonCreateV(v0, v1, v2);
  }

  @Test
  public final void testCreatePolygonTooFew()
  {
    final RoomModelType model = this.emptyModel();

    final RoomPolyVertexType v0 = model.vertexCreate(Vector2I.of(0, 1));
    final RoomPolyVertexType v1 = model.vertexCreate(Vector2I.of(0, 0));

    this.expected.expect(RoomModelExceptionPolygonTooFewVertices.class);
    this.expected.expectMessage(StringContains.containsString("must have at least three vertices"));
    model.polygonCreateV(v0, v1);
  }

  @Test
  public final void testDeletePolygon()
  {
    final RoomModelType model = this.emptyModel();

    final RoomPolyVertexType v0 = model.vertexCreate(Vector2I.of(0, 1));
    final RoomPolyVertexType v1 = model.vertexCreate(Vector2I.of(0, 0));
    final RoomPolyVertexType v2 = model.vertexCreate(Vector2I.of(1, 0));

    final RoomPolygonType poly = model.polygonCreateV(v0, v1, v2);
    Assert.assertTrue(model.polygons().contains(poly));

    model.polygonDelete(poly);
    Assert.assertFalse(model.polygons().contains(poly));
    this.checkModel(model);
  }

  @Test
  public final void testDeletePolygonNonexistent()
  {
    final RoomModelType model = this.emptyModel();

    this.expected.expect(RoomModelExceptionPolygonNonexistent.class);
    this.expected.expectMessage(StringContains.containsString("does not belong to this model"));
    model.polygonDelete(new RoomPolygonType()
    {
      @Override
      public long id()
      {
        return 0L;
      }

      @Override
      public AreaL bounds()
      {
        return AreasL.create(0L, 0L, 4L, 4L);
      }

      @Override
      public List<RoomPolyEdgeType> edges()
      {
        return Collections.emptyList();
      }

      @Override
      public List<RoomPolyVertexType> vertices()
      {
        return Collections.emptyList();
      }
    });
  }

  @Test
  public final void testDeletePolygonJoined()
  {
    final RoomModelType model = this.emptyModel();

    final RoomPolyVertexType v0 = model.vertexCreate(Vector2I.of(0, 1));
    final RoomPolyVertexType v1 = model.vertexCreate(Vector2I.of(0, 0));
    final RoomPolyVertexType v2 = model.vertexCreate(Vector2I.of(1, 0));
    final RoomPolyVertexType v3 = model.vertexCreate(Vector2I.of(1, 1));

    final RoomPolygonType poly0 = model.polygonCreateV(v0, v1, v2);
    final RoomPolygonType poly1 = model.polygonCreateV(v0, v2, v3);
    this.checkModel(model);

    model.polygonDelete(poly1);
    Assert.assertTrue(model.polygons().contains(poly0));
    Assert.assertFalse(model.polygons().contains(poly1));
    this.checkModel(model);

    model.polygonDelete(poly0);
    Assert.assertFalse(model.polygons().contains(poly0));
    Assert.assertFalse(model.polygons().contains(poly1));
    this.checkModel(model);
  }
}
