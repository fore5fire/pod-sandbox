package dev.podsandbox.executor;

import dev.podsandbox.Config;
import dev.podsandbox.http.JsonHttpServer;
import java.util.concurrent.Executors;

/** Starts the server that runs code inside a sandbox pod. */
public final class ExecutorMain {

  private ExecutorMain() {}

  public static void main(String[] args) throws Exception {
    var runner = new PythonRunner();
    var server = new JsonHttpServer(Config.integer("PORT", 8080), Executors.newCachedThreadPool());
    server.route("POST", "/exec", request -> runner.run(request.field("code")));
    server.start();
  }
}
