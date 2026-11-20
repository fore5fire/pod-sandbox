package dev.podsandbox.controller;

import io.fabric8.kubernetes.api.model.Pod;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Keeps started pods ready so that opening a session does not wait for a pod to start. */
public final class PodPool {

  private final SandboxPods pods;
  private final int targetSize;
  private final List<Pod> idle = new ArrayList<>();

  public PodPool(SandboxPods pods, int targetSize) {
    this.pods = pods;
    this.targetSize = targetSize;
  }

  public void start() {
    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::refill, 0, 5, TimeUnit.SECONDS);
  }

  public Pod take() throws InterruptedException {
    if (idle.isEmpty()) {
      return pods.launch();
    }
    return idle.remove(0);
  }

  public void giveBack(Pod pod) {
    idle.add(pod);
  }

  private void refill() {
    try {
      while (idle.size() < targetSize) {
        idle.add(pods.launch());
      }
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }
}
