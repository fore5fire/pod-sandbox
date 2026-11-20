package dev.podsandbox.http;

/** Thrown by a handler to answer with a specific HTTP status. */
public final class HttpError extends RuntimeException {

  private final int status;

  public HttpError(int status, String message) {
    super(message);
    this.status = status;
  }

  public int status() {
    return status;
  }
}
