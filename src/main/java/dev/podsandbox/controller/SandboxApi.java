package dev.podsandbox.controller;

import dev.podsandbox.http.JsonHttpServer;
import dev.podsandbox.http.JsonHttpServer.Request;
import io.fabric8.kubernetes.api.model.Pod;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The controller's HTTP API. */
public final class SandboxApi {

  private static final Logger LOG = LoggerFactory.getLogger(SandboxApi.class);

  private final SessionRegistry sessions;
  private final PodPool pool;
  private final ExecutorClient executor;
  private final Map<String, Pod> podsBySession = new HashMap<>();

  public SandboxApi(SessionRegistry sessions, PodPool pool, ExecutorClient executor) {
    this.sessions = sessions;
    this.pool = pool;
    this.executor = executor;
  }

  public void register(JsonHttpServer server) {
    server.route("POST", "/v1/sessions", this::openSession);
    server.route("POST", "/v1/sessions/{id}/exec", this::exec);
    server.route("DELETE", "/v1/sessions/{id}", this::closeSession);
    server.route("POST", "/v1/run", this::run);
    server.route("GET", "/healthz", request -> Map.of("status", "ok"));
  }

  private Object openSession(Request request) throws Exception {
    return Map.of("session_id", open(request.field("tenant")).id());
  }

  private Object exec(Request request) {
    try {
      var session = sessions.get(request.param("id"));
      return executor.exec(session, request.field("code"));
    } catch (Exception e) {
      LOG.error("exec failed", e);
      return Map.of("stdout", "", "stderr", String.valueOf(e.getMessage()), "exit_code", 1);
    }
  }

  private Object closeSession(Request request) {
    close(request.param("id"));
    return Map.of();
  }

  private Object run(Request request) throws Exception {
    var session = open(request.field("tenant"));
    var result = executor.exec(session, request.field("code"));
    close(session.id());
    return result;
  }

  private Session open(String tenant) throws InterruptedException {
    var pod = pool.take();
    var session = sessions.open(tenant, pod.getMetadata().getName(), pod.getStatus().getPodIP());
    podsBySession.put(session.id(), pod);
    return session;
  }

  private void close(String sessionId) {
    sessions.close(sessionId);
    pool.giveBack(podsBySession.remove(sessionId));
  }
}
