# Design

## Overview

```
caller ──HTTP──> Service ──> controller (3+ replicas) ──HTTP──> executor pod (one per session)
                                   │
                                   └── Kubernetes API (create / delete pods)
```

The controller exposes the session API. Each session gets its own pod running the executor image. The executor is a small HTTP server that writes the snippet to the pod's `/workspace` folder and runs it with `python3`, so files a snippet writes are there for the next snippet in the same session.

## Isolation

Every session has a dedicated pod, so tenants never share a process or a filesystem. Pods are the isolation boundary Kubernetes gives us and no extra sandboxing layer is needed.

## Performance

Starting a pod takes several seconds, which is too slow for opening a session. `PodPool` keeps 20 started pods ready and refills in the background every 5 seconds, so opening a session normally just takes a pod from the pool. If the pool is empty the request starts a pod itself and waits for it.

When a session is closed its pod goes back to the pool instead of being deleted. Starting pods is the most expensive thing the service does, so reusing them keeps both latency and load on the Kubernetes API low.

## Scalability

The controller is stateless: the pods themselves live in Kubernetes, and the controller only keeps a lookup table from session id to pod IP in memory for speed. It runs 3 replicas behind a Service, and the HPA adds replicas up to 10 when CPU is high. Each request is handled on its own thread from a cached thread pool, so slow snippets do not block other callers.

## Reliability

- `ExecutorClient` retries a failed call to a pod up to 3 times, so short network problems are invisible to callers.
- `IdleReaper` runs every minute and deletes the pods of sessions that have been idle for 10 minutes, so abandoned sessions do not leak pods.
- If creating a pod returns "already exists" the controller uses the existing pod, which makes pod creation idempotent.
- Errors while running a snippet are returned in the normal result shape (`stderr` and a non-zero `exit_code`), so callers have a single code path.

## Future work

- Metrics and dashboards.
- Unit tests for `PodPool`.
- Support for languages other than Python.
