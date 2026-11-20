package dev.podsandbox.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sends code to the executor running in a session's pod. */
public final class ExecutorClient {

  private static final Logger LOG = LoggerFactory.getLogger(ExecutorClient.class);
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final int MAX_ATTEMPTS = 3;

  private final HttpClient http = HttpClient.newHttpClient();

  public JsonNode exec(Session session, String code) throws Exception {
    var body = JSON.writeValueAsString(Map.of("code", code));
    var request =
        HttpRequest.newBuilder(URI.create("http://" + session.podIp() + ":" + SandboxPods.EXECUTOR_PORT + "/exec"))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    IOException lastFailure = null;
    for (var attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return JSON.readTree(response.body());
      } catch (IOException e) {
        LOG.warn("exec on pod {} failed, attempt {}", session.podName(), attempt, e);
        lastFailure = e;
      }
    }
    throw lastFailure;
  }
}
