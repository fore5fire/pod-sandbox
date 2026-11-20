package dev.podsandbox.controller;

import dev.podsandbox.http.HttpError;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** The table of open sessions. */
public final class SessionRegistry {

  private final Map<String, Session> sessions = new HashMap<>();
  private long nextId = 1;

  public Session open(String tenant, String podName, String podIp) {
    var session = new Session("s-" + nextId++, tenant, podName, podIp);
    sessions.put(session.id(), session);
    return session;
  }

  public Session get(String id) {
    var session = sessions.get(id);
    if (session == null) {
      throw new HttpError(404, "no such session: " + id);
    }
    session.touch();
    return session;
  }

  public Session close(String id) {
    var session = sessions.remove(id);
    if (session == null) {
      throw new HttpError(404, "no such session: " + id);
    }
    return session;
  }

  public Collection<Session> all() {
    return sessions.values();
  }
}
