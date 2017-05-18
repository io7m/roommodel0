package com.io7m.roommodel0.mesh;

public final class MeshExceptionPolygonOutsideBounds extends
  MeshException
{
  public MeshExceptionPolygonOutsideBounds(
    final String message)
  {
    super(message);
  }

  public MeshExceptionPolygonOutsideBounds(
    final String message,
    final Throwable cause)
  {
    super(message, cause);
  }

  public MeshExceptionPolygonOutsideBounds(
    final Throwable cause)
  {
    super(cause);
  }
}
