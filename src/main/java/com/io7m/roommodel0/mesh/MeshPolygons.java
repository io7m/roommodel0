/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.roommodel0.mesh;

import com.io7m.jregions.core.unparameterized.areas.AreaI;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2D;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.jtensors.core.unparameterized.vectors.Vectors2D;
import com.io7m.junreachable.UnreachableCodeException;

import java.util.List;

public final class MeshPolygons
{
  private MeshPolygons()
  {
    throw new UnreachableCodeException();
  }

  private static double crossProductLength(
    final double Ax,
    final double Ay,
    final double Bx,
    final double By,
    final double Cx,
    final double Cy)
  {
    final double BAx = Ax - Bx;
    final double BAy = Ay - By;
    final double BCx = Cx - Bx;
    final double BCy = Cy - By;

    return (BAx * BCy - BAy * BCx);
  }

  static boolean isConvex(
    final List<Vector2I> vertices)
  {
    boolean got_negative = false;
    boolean got_positive = false;
    final int num_points = vertices.size();

    for (int point_a = 0; point_a < num_points; point_a++) {
      final int point_b = (point_a + 1) % num_points;
      final int point_c = (point_b + 1) % num_points;

      final double cross_product =
        crossProductLength(
          (double) vertices.get(point_a).x(),
          (double) vertices.get(point_a).y(),
          (double) vertices.get(point_b).x(),
          (double) vertices.get(point_b).y(),
          (double) vertices.get(point_c).x(),
          (double) vertices.get(point_c).y());
      if (cross_product < 0.0) {
        got_negative = true;
      } else if (cross_product > 0.0) {
        got_positive = true;
      }
      if (got_negative && got_positive) {
        return false;
      }
    }

    return true;
  }

  static Vector2D normal(
    final Vector2D vv0,
    final Vector2D vv1)
  {
    final Vector2D edge = Vectors2D.subtract(vv0, vv1);
    final Vector2D perp = Vector2D.of(edge.y(), -edge.x());
    return Vectors2D.normalize(perp);
  }

  static Vector2D normal(
    final Vector2I vv0,
    final Vector2I vv1)
  {
    return normal(toVector2D(vv0), toVector2D(vv1));
  }

  private static Vector2D toVector2D(
    final Vector2I v)
  {
    return Vector2D.of((double) v.x(), (double) v.y());
  }

  static Vector2D barycenter(
    final List<Vector2I> vertices)
  {
    double x = 0.0;
    double y = 0.0;
    final int size = vertices.size();
    for (int index = 0; index < size; ++index) {
      final Vector2I p = vertices.get(index);
      x = x + (double) p.x();
      y = y + (double) p.y();
    }
    return Vector2D.of(x / (double) size, y / (double) size);
  }

  static AreaI bounds(
    final List<Vector2I> vertices)
  {
    final int size = vertices.size();
    int x_min = Integer.MAX_VALUE;
    int y_min = Integer.MAX_VALUE;
    int x_max = Integer.MIN_VALUE;
    int y_max = Integer.MIN_VALUE;
    for (int index = 0; index < size; ++index) {
      final Vector2I p = vertices.get(index);
      x_min = Math.min(p.x(), x_min);
      y_min = Math.min(p.y(), y_min);
      x_max = Math.max(p.x(), x_max);
      y_max = Math.max(p.y(), y_max);
    }
    return AreaI.of(x_min, x_max, y_min, y_max);
  }

  static boolean containsPoint(
    final List<Vector2I> points,
    final Vector2I point)
  {
    boolean result = false;
    int i;
    int j;
    for (i = 0, j = points.size() - 1; i < points.size(); j = i++) {
      final Vector2I p0 = points.get(i);
      final Vector2I p1 = points.get(j);
      if (((p0.y() > point.y()) != (p1.y() > point.y()))) {
        final int p1x_p0x_delta = p1.x() - p0.x();
        final int p1y_p0y_delta = p1.y() - p0.y();
        final int py_p0y_delta = point.y() - p0.y();
        if ((point.x() < (((p1x_p0x_delta * py_p0y_delta) / p1y_p0y_delta) + p0.x()))) {
          result = !result;
        }
      }
    }
    return result;
  }

  static boolean isClockwiseOrder(
    final List<Vector2I> vertices)
  {
    long sum = 0L;
    for (int index = 0; index < vertices.size(); index++) {
      final Vector2I v1 = vertices.get(index);
      final Vector2I v2 = vertices.get((index + 1) % vertices.size());
      final long xd = Math.subtractExact((long) v2.x(), (long) v1.x());
      final long yd = Math.addExact((long) v2.y(), (long) v1.y());
      sum = Math.addExact(sum, Math.multiplyExact(xd, yd));
    }
    return sum > 0L;
  }
}
