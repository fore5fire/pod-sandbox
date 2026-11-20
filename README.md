# pod-sandbox

A service that runs untrusted Python snippets for a multi-tenant SaaS platform. Each workload runs in its own Kubernetes pod.

## What the service has to do

The platform's customers (tenants) submit Python code: data transforms, small scripts written by their own end users, code produced by LLM agents. The platform's backend services call pod-sandbox to run that code.

Requirements:

1. **Isolation.** The code is untrusted. One tenant's code must never read or affect another tenant's code, files or results, or the platform itself.
2. **Sessions.** A caller can open a session, run several snippets in it, and see files written by earlier snippets. Sessions end when the caller closes them or after 10 idle minutes.
3. **One-shot runs.** A caller can run a single snippet without managing a session.
4. **Load.** Peak is a few hundred executions per second across about a thousand open sessions. Most snippets finish in under a second; some run for a minute.
5. **Availability.** The service is deployed several times a day with rolling updates and must survive the loss of any single pod or node without losing sessions that are not on that node.
6. **Cost.** Sandbox pods run on a dedicated node pool that should not hold idle capacity for long.

## API

| Method | Path | Body | Result |
| --- | --- | --- | --- |
| POST | `/v1/sessions` | `{"tenant": "acme"}` | `{"session_id": "..."}` |
| POST | `/v1/sessions/{id}/exec` | `{"code": "print(1)"}` | `{"stdout": "...", "stderr": "...", "exit_code": 0}` |
| DELETE | `/v1/sessions/{id}` | | `{}` |
| POST | `/v1/run` | `{"tenant": "acme", "code": "print(1)"}` | same as exec |

## Layout

| Path | What it is |
| --- | --- |
| `src/main/java/dev/podsandbox/controller` | The controller: session API, pod lifecycle, routing to pods |
| `src/main/java/dev/podsandbox/executor` | The server that runs inside each sandbox pod |
| `deploy/` | Kubernetes manifests |
| `docs/DESIGN.md` | Design notes |
| `src/main/java/dev/podsandbox/http`, `Config.java`, `pom.xml`, `docker/` | Boilerplate: a small JSON-over-HTTP server, environment config, build and images |

## Build

```
mvn package
docker build -f docker/controller.Dockerfile -t pod-sandbox-controller .
docker build -f docker/executor.Dockerfile -t pod-sandbox-executor .
```
