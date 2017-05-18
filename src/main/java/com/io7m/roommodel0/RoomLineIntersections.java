package com.io7m.roommodel0;

import com.io7m.jtensors.core.unparameterized.vectors.Vector2D;
import com.io7m.jtensors.core.unparameterized.vectors.Vector2I;
import com.io7m.junreachable.UnreachableCodeException;

import java.util.Optional;

import static com.io7m.jnull.NullCheck.notNull;

public final class RoomLineIntersections
{
  private RoomLineIntersections()
  {
    throw new UnreachableCodeException();
  }

  public static boolean intersects(
    final Vector2I line0_p0,
    final Vector2I line0_p1,
    final Vector2I line1_p0,
    final Vector2I line1_p1)
  {
    notNull(line0_p0, "Line0 Point 0");
    notNull(line0_p1, "Line0 Point 1");
    notNull(line1_p0, "Line1 Point 0");
    notNull(line1_p1, "Line1 Point 1");

    return intersects(
      (double) line0_p0.x(),
      (double) line0_p0.y(),
      (double) line0_p1.x(),
      (double) line0_p1.y(),
      (double) line1_p0.x(),
      (double) line1_p0.y(),
      (double) line1_p1.x(),
      (double) line1_p1.y());
  }

  public static boolean intersects(
    final Vector2D line0_p0,
    final Vector2D line0_p1,
    final Vector2D line1_p0,
    final Vector2D line1_p1)
  {
    notNull(line0_p0, "Line0 Point 0");
    notNull(line0_p1, "Line0 Point 1");
    notNull(line1_p0, "Line1 Point 0");
    notNull(line1_p1, "Line1 Point 1");

    return intersects(
      line0_p0.x(),
      line0_p0.y(),
      line0_p1.x(),
      line0_p1.y(),
      line1_p0.x(),
      line1_p0.y(),
      line1_p1.x(),
      line1_p1.y());
  }

  public static boolean intersects(
    final double line0_x0,
    final double line0_y0,
    final double line0_x1,
    final double line0_y1,
    final double line1_x0,
    final double line1_y0,
    final double line1_x1,
    final double line1_y1)
  {
    final double line0_delta_x = line0_x1 - line0_x0;
    final double line0_delta_y = line0_y1 - line0_y0;
    final double line1_delta_x = line1_x1 - line1_x0;
    final double line1_delta_y = line1_y1 - line1_y0;

    final double denom =
      (line1_delta_y * line0_delta_x) - (line1_delta_x * line0_delta_y);

    if (denom == 0.0) {
      return false;
    }

    final double line0_1_y0_delta = line0_y0 - line1_y0;
    final double line0_1_x0_delta = line0_x0 - line1_x0;

    final double a_numer =
      (line1_delta_x * line0_1_y0_delta) - (line1_delta_y * line0_1_x0_delta);
    final double u_a = a_numer / denom;
    final double b_numer =
      (line0_delta_x * line0_1_y0_delta) - (line0_delta_y * line0_1_x0_delta);
    final double u_b = b_numer / denom;

    return u_a >= 0.0 && u_a <= 1.0 && u_b >= 0.0 && u_b <= 1.0;
  }

  public static Optional<Vector2D> intersection(
    final Vector2D line0_p0,
    final Vector2D line0_p1,
    final Vector2D line1_p0,
    final Vector2D line1_p1)
  {
    notNull(line0_p0, "Line0 Point 0");
    notNull(line0_p1, "Line0 Point 1");
    notNull(line1_p0, "Line1 Point 0");
    notNull(line1_p1, "Line1 Point 1");

    return intersection(
      line0_p0.x(),
      line0_p0.y(),
      line0_p1.x(),
      line0_p1.y(),
      line1_p0.x(),
      line1_p0.y(),
      line1_p1.x(),
      line1_p1.y());
  }

  public static Optional<Vector2D> intersection(
    final Vector2I line0_p0,
    final Vector2I line0_p1,
    final Vector2I line1_p0,
    final Vector2I line1_p1)
  {
    notNull(line0_p0, "Line0 Point 0");
    notNull(line0_p1, "Line0 Point 1");
    notNull(line1_p0, "Line1 Point 0");
    notNull(line1_p1, "Line1 Point 1");

    return intersection(
      (double) line0_p0.x(),
      (double) line0_p0.y(),
      (double) line0_p1.x(),
      (double) line0_p1.y(),
      (double) line1_p0.x(),
      (double) line1_p0.y(),
      (double) line1_p1.x(),
      (double) line1_p1.y());
  }

  public static Optional<Vector2D> intersection(
    final double line0_x0,
    final double line0_y0,
    final double line0_x1,
    final double line0_y1,
    final double line1_x0,
    final double line1_y0,
    final double line1_x1,
    final double line1_y1)
  {
    final double line1_delta_y = line1_y1 - line1_y0;
    final double line0_delta_x = line0_x1 - line0_x0;
    final double line1_delta_x = line1_x1 - line1_x0;
    final double line0_delta_y = line0_y1 - line0_y0;

    final double denom =
      line1_delta_y * line0_delta_x - line1_delta_x * line0_delta_y;

    if (denom == 0.0) {
      return Optional.empty();
    }

    final double line0_1_y0_delta = line0_y0 - line1_y0;
    final double line0_1_x0_delta = line0_x0 - line1_x0;

    final double a_numer =
      (line1_delta_x * line0_1_y0_delta) - (line1_delta_y * line0_1_x0_delta);
    final double u_a = a_numer / denom;
    final double b_numer =
      (line0_delta_x * line0_1_y0_delta) - (line0_delta_y * line0_1_x0_delta);
    final double u_b = b_numer / denom;

    if (u_a >= 0.0 && u_a <= 1.0 && u_b >= 0.0 && u_b <= 1.0) {
      final double inter_x = line0_x0 + (u_a * line0_delta_x);
      final double inter_y = line0_y0 + (u_a * line0_delta_y);
      return Optional.of(Vector2D.of(inter_x, inter_y));
    }

    return Optional.empty();
  }
}
