package dev.podsandbox.controller;

/** An open sandbox session and the pod that serves it. */
public final class Session {

  private final String id;
  private final String tenant;
  private final String podName;
  private final String podIp;
  private long lastUsedMillis;

  public Session(String id, String tenant, String podName, String podIp) {
    this.id = id;
    this.tenant = tenant;
    this.podName = podName;
    this.podIp = podIp;
    this.lastUsedMillis = System.currentTimeMillis();
  }

  public String id() {
    return id;
  }

  public String tenant() {
    return tenant;
  }

  public String podName() {
    return podName;
  }

  public String podIp() {
    return podIp;
  }

  public long lastUsedMillis() {
    return lastUsedMillis;
  }

  public void touch() {
    lastUsedMillis = System.currentTimeMillis();
  }
}
