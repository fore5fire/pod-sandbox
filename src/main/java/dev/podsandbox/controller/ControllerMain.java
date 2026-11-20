package dev.podsandbox.controller;

import dev.podsandbox.Config;
import dev.podsandbox.http.JsonHttpServer;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.util.concurrent.Executors;

/** Starts the sandbox controller. */
public final class ControllerMain {

  private ControllerMain() {}

  public static void main(String[] args) throws Exception {
    var k8s = new KubernetesClientBuilder().build();
    var pods = new SandboxPods(k8s, Config.string("SANDBOX_NAMESPACE", "sandbox"), Config.string("EXECUTOR_IMAGE", "pod-sandbox-executor:latest"));
    var pool = new PodPool(pods, Config.integer("POOL_SIZE", 20));
    var sessions = new SessionRegistry();

    pool.start();
    new IdleReaper(sessions, pods, Config.integer("IDLE_TIMEOUT_MINUTES", 10)).start();

    var server = new JsonHttpServer(Config.integer("PORT", 8080), Executors.newCachedThreadPool());
    new SandboxApi(sessions, pool, new ExecutorClient()).register(server);
    server.start();
  }
}
