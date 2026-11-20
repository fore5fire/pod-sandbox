package dev.podsandbox;

/** Reads settings from environment variables. */
public final class Config {

  private Config() {}

  public static String string(String name, String fallback) {
    var value = System.getenv(name);
    return value == null || value.isEmpty() ? fallback : value;
  }

  public static int integer(String name, int fallback) {
    var value = System.getenv(name);
    return value == null || value.isEmpty() ? fallback : Integer.parseInt(value);
  }
}
