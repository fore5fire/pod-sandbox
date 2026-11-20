package dev.podsandbox.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * A small JSON-over-HTTP server. Routes are registered as a method and a path pattern such as {@code /v1/sessions/{id}/exec}. A handler's return value is
 * written as a 200 JSON body, an {@link HttpError} becomes its status, and any other exception becomes a 500.
 */
public final class JsonHttpServer {

  /** Handles one request. */
  public interface Handler {
    Object handle(Request request) throws Exception;
  }

  /** A request's path parameters and JSON body. */
  public record Request(Map<String, String> pathParams, JsonNode body) {

    public String param(String name) {
      return pathParams.get(name);
    }

    public String field(String name) {
      var value = body.get(name);
      if (value == null || value.isNull()) {
        throw new HttpError(400, "missing field: " + name);
      }
      return value.asText();
    }
  }

  private record Route(String method, String[] segments, Handler handler) {}

  private static final ObjectMapper JSON = new ObjectMapper();

  private final HttpServer server;
  private final List<Route> routes = new ArrayList<>();

  public JsonHttpServer(int port, Executor executor) throws IOException {
    server = HttpServer.create(new InetSocketAddress(port), 0);
    server.setExecutor(executor);
    server.createContext("/", this::dispatch);
  }

  public void route(String method, String pattern, Handler handler) {
    routes.add(new Route(method, pattern.split("/"), handler));
  }

  public void start() {
    server.start();
  }

  private void dispatch(HttpExchange exchange) throws IOException {
    try (exchange) {
      var segments = exchange.getRequestURI().getPath().split("/");
      for (var route : routes) {
        var params = match(route, exchange.getRequestMethod(), segments);
        if (params == null) {
          continue;
        }
        var bytes = exchange.getRequestBody().readAllBytes();
        var body = bytes.length == 0 ? JSON.createObjectNode() : JSON.readTree(bytes);
        try {
          respond(exchange, 200, route.handler().handle(new Request(params, body)));
        } catch (HttpError e) {
          respond(exchange, e.status(), Map.of("error", e.getMessage()));
        } catch (Exception e) {
          respond(exchange, 500, Map.of("error", String.valueOf(e.getMessage())));
        }
        return;
      }
      respond(exchange, 404, Map.of("error", "no such route"));
    }
  }

  private static Map<String, String> match(Route route, String method, String[] segments) {
    if (!route.method().equals(method) || route.segments().length != segments.length) {
      return null;
    }
    var params = new HashMap<String, String>();
    for (var i = 0; i < segments.length; i++) {
      var expected = route.segments()[i];
      if (expected.startsWith("{")) {
        params.put(expected.substring(1, expected.length() - 1), segments[i]);
      } else if (!expected.equals(segments[i])) {
        return null;
      }
    }
    return params;
  }

  private static void respond(HttpExchange exchange, int status, Object body) throws IOException {
    var bytes = JSON.writeValueAsBytes(body);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
  }
}
