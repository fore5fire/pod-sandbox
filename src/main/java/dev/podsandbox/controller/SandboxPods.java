package dev.podsandbox.controller;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates and deletes the pods that sandboxes run in. */
public final class SandboxPods {

  public static final int EXECUTOR_PORT = 8080;

  private static final Logger LOG = LoggerFactory.getLogger(SandboxPods.class);
  private static final int START_TIMEOUT_SECONDS = 60;

  private final KubernetesClient k8s;
  private final String namespace;
  private final String image;
  private final AtomicInteger podCounter = new AtomicInteger();

  public SandboxPods(KubernetesClient k8s, String namespace, String image) {
    this.k8s = k8s;
    this.namespace = namespace;
    this.image = image;
  }

  public Pod launch() throws InterruptedException {
    var name = "sandbox-" + podCounter.incrementAndGet();
    var pod =
        new PodBuilder()
            .withNewMetadata()
            .withName(name)
            .addToLabels("app", "sandbox")
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName("executor")
            .withImage(image)
            .addNewPort()
            .withContainerPort(EXECUTOR_PORT)
            .endPort()
            .endContainer()
            .endSpec()
            .build();
    try {
      k8s.pods().inNamespace(namespace).resource(pod).create();
    } catch (KubernetesClientException e) {
      if (e.getCode() != 409) {
        throw e;
      }
      LOG.info("pod {} already exists, using it", name);
    }
    return waitUntilRunning(name);
  }

  public void delete(String name) {
    try {
      k8s.pods().inNamespace(namespace).withName(name).delete();
    } catch (KubernetesClientException e) {
      LOG.warn("could not delete pod {}", name, e);
    }
  }

  private Pod waitUntilRunning(String name) throws InterruptedException {
    for (var waited = 0; waited < START_TIMEOUT_SECONDS; waited++) {
      var pod = k8s.pods().inNamespace(namespace).withName(name).get();
      if (pod != null && "Running".equals(pod.getStatus().getPhase())) {
        return pod;
      }
      Thread.sleep(1000);
    }
    throw new IllegalStateException("pod " + name + " did not start in " + START_TIMEOUT_SECONDS + "s");
  }
}
