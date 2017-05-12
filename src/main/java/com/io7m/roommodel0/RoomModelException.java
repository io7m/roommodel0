package com.io7m.roommodel0;

public abstract class RoomModelException extends RuntimeException
{
  public RoomModelException(
    final String message)
  {
    super(message);
  }

  public RoomModelException(
    final String message,
    final Throwable cause)
  {
    super(message, cause);
  }

  public RoomModelException(
    final Throwable cause)
  {
    super(cause);
  }
}
