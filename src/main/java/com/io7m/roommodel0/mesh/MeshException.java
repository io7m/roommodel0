package com.io7m.roommodel0.mesh;

public abstract class MeshException extends RuntimeException
{
  public MeshException(
    final String message)
  {
    super(message);
  }

  public MeshException(
    final String message,
    final Throwable cause)
  {
    super(message, cause);
  }

  public MeshException(
    final Throwable cause)
  {
    super(cause);
  }
}
