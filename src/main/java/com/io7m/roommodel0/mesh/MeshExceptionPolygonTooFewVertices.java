package com.io7m.roommodel0.mesh;

public final class MeshExceptionPolygonTooFewVertices extends
  MeshException
{
  public MeshExceptionPolygonTooFewVertices(
    final String message)
  {
    super(message);
  }

  public MeshExceptionPolygonTooFewVertices(
    final String message,
    final Throwable cause)
  {
    super(message, cause);
  }

  public MeshExceptionPolygonTooFewVertices(
    final Throwable cause)
  {
    super(cause);
  }
}
