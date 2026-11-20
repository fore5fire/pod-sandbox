package dev.podsandbox.controller;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Closes sessions that have not been used for the idle timeout and deletes their pods. */
public final class IdleReaper {

  private static final Logger LOG = LoggerFactory.getLogger(IdleReaper.class);

  private final SessionRegistry sessions;
  private final SandboxPods pods;
  private final long idleTimeoutMillis;

  public IdleReaper(SessionRegistry sessions, SandboxPods pods, int idleTimeoutMinutes) {
    this.sessions = sessions;
    this.pods = pods;
    this.idleTimeoutMillis = TimeUnit.MINUTES.toMillis(idleTimeoutMinutes);
  }

  public void start() {
    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::reap, 1, 1, TimeUnit.MINUTES);
  }

  private void reap() {
    var now = System.currentTimeMillis();
    for (var session : sessions.all()) {
      if (now - session.lastUsedMillis() > idleTimeoutMillis) {
        LOG.info("session {} idle, deleting pod {}", session.id(), session.podName());
        pods.delete(session.podName());
        sessions.close(session.id());
      }
    }
  }
}
