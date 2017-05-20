package com.io7m.roommodel0.tests.mesh;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.roommodel0.mesh.MeshPolygons;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

public final class MeshPolygonsTest
{
  @Test
  public void testAreaOf_0()
  {
    final ArrayList<Vector2I> vs0 = new ArrayList<>(5);
    vs0.add(Vector2I.of(0, 4));
    vs0.add(Vector2I.of(0, 0));
    vs0.add(Vector2I.of(4, 0));
    vs0.add(Vector2I.of(4, 4));
    vs0.add(Vector2I.of(2, 5));

    final ArrayList<Vector2I> vs1 = new ArrayList<>(vs0);
    Collections.reverse(vs1);

    final long area1 = MeshPolygons.area(vs1);
    Assert.assertEquals(18L, area1);
    final long area0 = MeshPolygons.area(vs0);
    Assert.assertEquals(-18L, area0);
  }
}
