package com.io7m.roommodel0.tests;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.roommodel0.RoomModelExceptionPolygonDuplicate;
import com.io7m.roommodel0.RoomModelExceptionPolygonNonexistent;
import com.io7m.roommodel0.RoomModelExceptionPolygonNotConvex;
import com.io7m.roommodel0.RoomModelExceptionPolygonOutsideBounds;
import com.io7m.roommodel0.RoomModelExceptionPolygonTooFewVertices;
import com.io7m.roommodel0.RoomModelExceptionVertexNonexistent;
import com.io7m.roommodel0.RoomModelType;
import com.io7m.roommodel0.RoomPolyEdgeType;
import com.io7m.roommodel0.RoomPolyVertexID;
import com.io7m.roommodel0.RoomPolyVertexType;
import com.io7m.roommodel0.RoomPolygonID;
import com.io7m.roommodel0.RoomPolygonType;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;

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

    final RoomPolygonType poly = model.polygonCreateVV(v0, v1, v2);

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
  public final void testCreatePolygonWithID()
  {
    final RoomModelType model = this.emptyModel();

    final RoomPolyVertexType v0 = model.vertexCreate(Vector2I.of(0, 1));
    final RoomPolyVertexType v1 = model.vertexCreate(Vector2I.of(0, 0));
    final RoomPolyVertexType v2 = model.vertexCreate(Vector2I.of(1, 0));

    final RoomPolygonID pid = RoomPolygonID.of(1L);

    final RoomPolygonType poly0 =
      model.polygonCreateWithIDVV(pid, v0, v1, v2);
    final RoomPolygonType poly1 =
      model.polygonCreateVV(v0, v1, v2);
    final RoomPolygonType poly2 =
      model.polygonCreateVV(v0, v1, v2);

    Assert.assertNotEquals(poly0.id(), poly1.id());
    Assert.assertNotEquals(poly1.id(), poly2.id());
    Assert.assertNotEquals(poly2.id(), poly0.id());
  }

  @Test
  public final void testCreatePolygonWithIDExists()
  {
    final RoomModelType model = this.emptyModel();

    final RoomPolyVertexType v0 = model.vertexCreate(Vector2I.of(0, 1));
    final RoomPolyVertexType v1 = model.vertexCreate(Vector2I.of(0, 0));
    final RoomPolyVertexType v2 = model.vertexCreate(Vector2I.of(1, 0));

    final RoomPolygonID pid = RoomPolygonID.of(0L);
    model.polygonCreateWithIDVV(pid, v0, v1, v2);

    this.expected.expect(RoomModelExceptionPolygonDuplicate.class);
    model.polygonCreateWithIDVV(pid, v0, v1, v2);
  }

  @Test
  public final void testCreatePolygonJoined()
  {
    final RoomModelType model = this.emptyModel();

    final RoomPolyVertexType v0 = model.vertexCreate(Vector2I.of(0, 1));
    final RoomPolyVertexType v1 = model.vertexCreate(Vector2I.of(0, 0));
    final RoomPolyVertexType v2 = model.vertexCreate(Vector2I.of(1, 0));
    final RoomPolyVertexType v3 = model.vertexCreate(Vector2I.of(1, 1));

    final RoomPolygonType poly0 = model.polygonCreateVV(v0, v1, v2);
    final RoomPolygonType poly1 = model.polygonCreateVV(v0, v2, v3);

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
      public RoomPolyVertexID id()
      {
        return RoomPolyVertexID.of(Long.MAX_VALUE);
      }

      @Override
      public boolean deleted()
      {
        return false;
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
    model.polygonCreateVV(v0, v1, v2);
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
    model.polygonCreateVV(v0, v1, v2, v3);
  }

  @Test
  public final void testCreatePolygonTooLarge()
  {
    final RoomModelType model = this.emptyModel();

    final RoomPolyVertexType v0 = model.vertexCreate(Vector2I.of(
      -1000000,
      1000000));
    final RoomPolyVertexType v1 = model.vertexCreate(Vector2I.of(
      -1000000,
      -1000000));
    final RoomPolyVertexType v2 = model.vertexCreate(Vector2I.of(
      1000000,
      1000000));

    this.expected.expect(RoomModelExceptionPolygonOutsideBounds.class);
    this.expected.expectMessage(StringContains.containsString("fit"));
    model.polygonCreateVV(v0, v1, v2);
  }

  @Test
  public final void testCreatePolygonTooFew()
  {
    final RoomModelType model = this.emptyModel();

    final RoomPolyVertexType v0 = model.vertexCreate(Vector2I.of(0, 1));
    final RoomPolyVertexType v1 = model.vertexCreate(Vector2I.of(0, 0));

    this.expected.expect(RoomModelExceptionPolygonTooFewVertices.class);
    this.expected.expectMessage(StringContains.containsString(
      "must have at least three vertices"));
    model.polygonCreateVV(v0, v1);
  }

  @Test
  public final void testDeletePolygon()
  {
    final RoomModelType model = this.emptyModel();

    final RoomPolyVertexType v0 = model.vertexCreate(Vector2I.of(0, 1));
    final RoomPolyVertexType v1 = model.vertexCreate(Vector2I.of(0, 0));
    final RoomPolyVertexType v2 = model.vertexCreate(Vector2I.of(1, 0));

    final RoomPolygonType poly = model.polygonCreateVV(v0, v1, v2);
    Assert.assertTrue(model.polygons().contains(poly));

    model.polygonDelete(poly.id());
    Assert.assertTrue(poly.deleted());
    Assert.assertTrue(v0.deleted());
    Assert.assertTrue(v1.deleted());
    Assert.assertTrue(v2.deleted());
    Assert.assertFalse(model.polygons().contains(poly));
    this.checkModel(model);
  }

  @Test
  public final void testDeletePolygonNonexistent()
  {
    final RoomModelType model = this.emptyModel();

    this.expected.expect(RoomModelExceptionPolygonNonexistent.class);
    model.polygonDelete(RoomPolygonID.of(0L));
  }

  @Test
  public final void testDeletePolygonJoined()
  {
    final RoomModelType model = this.emptyModel();

    final RoomPolyVertexType v0 = model.vertexCreate(Vector2I.of(0, 1));
    final RoomPolyVertexType v1 = model.vertexCreate(Vector2I.of(0, 0));
    final RoomPolyVertexType v2 = model.vertexCreate(Vector2I.of(1, 0));
    final RoomPolyVertexType v3 = model.vertexCreate(Vector2I.of(1, 1));

    final RoomPolygonType poly0 = model.polygonCreateVV(v0, v1, v2);
    final RoomPolygonType poly1 = model.polygonCreateVV(v0, v2, v3);
    this.checkModel(model);

    model.polygonDelete(poly1.id());
    Assert.assertFalse(v0.deleted());
    Assert.assertFalse(v1.deleted());
    Assert.assertFalse(v2.deleted());
    Assert.assertTrue(v3.deleted());
    Assert.assertFalse(poly0.deleted());
    Assert.assertTrue(poly1.deleted());
    Assert.assertTrue(model.polygons().contains(poly0));
    Assert.assertFalse(model.polygons().contains(poly1));
    this.checkModel(model);

    model.polygonDelete(poly0.id());
    Assert.assertTrue(v0.deleted());
    Assert.assertTrue(v1.deleted());
    Assert.assertTrue(v2.deleted());
    Assert.assertTrue(v3.deleted());
    Assert.assertTrue(poly0.deleted());
    Assert.assertTrue(poly1.deleted());
    Assert.assertFalse(model.polygons().contains(poly0));
    Assert.assertFalse(model.polygons().contains(poly1));
    this.checkModel(model);
  }
}
