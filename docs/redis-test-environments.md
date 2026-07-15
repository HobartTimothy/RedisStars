# Local Redis test environments

These examples are development-only and intentionally run without Redis authentication or TLS. Bind published ports to loopback and never expose them to an untrusted network. Docker Compose V2 is assumed (`docker compose`).

The application accepts:

- Standalone: one host/port and an optional database.
- Sentinel: one or more Sentinel nodes plus the monitored master name.
- Cluster: one or more seed nodes; Redis Cluster requires database `0`.

The current UI editor only creates Standalone profiles. Sentinel and Cluster profiles are supported by the domain, persistence, and Lettuce adapter, but must currently be supplied through code or persisted profile data.

## Standalone

Start:

```powershell
docker run --rm --name redisstars-standalone -p 127.0.0.1:6379:6379 redis:7-alpine
```

Use host `127.0.0.1`, port `6379`, database `0`. Stop with `Ctrl+C`, or from another terminal:

```powershell
docker stop redisstars-standalone
```

The `redis-jvm` integration tests also create disposable `redis:7-alpine` containers through Testcontainers. They are skipped by JUnit assumptions when Docker is unavailable.

## Sentinel

Save the following outside the repository as `compose.sentinel.yml`:

```yaml
services:
  master:
    image: redis:7-alpine
    ports:
      - "127.0.0.1:6379:6379"

  replica:
    image: redis:7-alpine
    command: ["redis-server", "--replicaof", "master", "6379"]
    depends_on: [master]

  sentinel:
    image: redis:7-alpine
    depends_on: [master, replica]
    extra_hosts:
      - "host.docker.internal:host-gateway"
    ports:
      - "127.0.0.1:26379:26379"
    command:
      - sh
      - -c
      - |
        printf '%s\n' \
          'port 26379' \
          'sentinel monitor mymaster host.docker.internal 6379 1' \
          'sentinel down-after-milliseconds mymaster 5000' \
          'sentinel failover-timeout mymaster 10000' \
          > /tmp/sentinel.conf
        exec redis-sentinel /tmp/sentinel.conf
```

Run and inspect:

```powershell
docker compose -f compose.sentinel.yml up -d
docker compose -f compose.sentinel.yml exec sentinel redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster
```

Use Sentinel node `127.0.0.1:26379`, master name `mymaster`, database `0`. Docker Desktop resolves `host.docker.internal`; on Linux, Compose's `host-gateway` mapping requires a recent Docker Engine. Tear down with:

```powershell
docker compose -f compose.sentinel.yml down -v
```

## Cluster

Cluster clients follow topology-advertised node addresses. A container cluster that advertises internal names is not reachable from the host-run Desktop app, even when seed ports are published. For a reliable host-side test, every advertised address and port must be reachable from the host.

One simple option on Linux or WSL with working Docker host networking is this development-only Compose file. Save it outside the repository as `compose.cluster.yml`:

```yaml
services:
  node-7000:
    image: redis:7-alpine
    network_mode: host
    command: ["redis-server", "--port", "7000", "--cluster-enabled", "yes", "--cluster-config-file", "/tmp/nodes-7000.conf", "--cluster-node-timeout", "5000", "--appendonly", "no", "--protected-mode", "no", "--cluster-announce-ip", "127.0.0.1", "--cluster-announce-port", "7000", "--cluster-announce-bus-port", "17000"]
  node-7001:
    image: redis:7-alpine
    network_mode: host
    command: ["redis-server", "--port", "7001", "--cluster-enabled", "yes", "--cluster-config-file", "/tmp/nodes-7001.conf", "--cluster-node-timeout", "5000", "--appendonly", "no", "--protected-mode", "no", "--cluster-announce-ip", "127.0.0.1", "--cluster-announce-port", "7001", "--cluster-announce-bus-port", "17001"]
  node-7002:
    image: redis:7-alpine
    network_mode: host
    command: ["redis-server", "--port", "7002", "--cluster-enabled", "yes", "--cluster-config-file", "/tmp/nodes-7002.conf", "--cluster-node-timeout", "5000", "--appendonly", "no", "--protected-mode", "no", "--cluster-announce-ip", "127.0.0.1", "--cluster-announce-port", "7002", "--cluster-announce-bus-port", "17002"]
  create:
    image: redis:7-alpine
    network_mode: host
    depends_on: [node-7000, node-7001, node-7002]
    restart: "no"
    entrypoint: ["sh", "-c"]
    command:
      - |
        until redis-cli -p 7000 ping; do sleep 1; done
        redis-cli --cluster create 127.0.0.1:7000 127.0.0.1:7001 127.0.0.1:7002 --cluster-replicas 0 --cluster-yes
```

Run and verify:

```powershell
docker compose -f compose.cluster.yml up -d node-7000 node-7001 node-7002
docker compose -f compose.cluster.yml run --rm create
docker run --rm --network host redis:7-alpine redis-cli -c -p 7000 cluster info
```

Use seeds `127.0.0.1:7000`, `127.0.0.1:7001`, and `127.0.0.1:7002`, database `0`. Tear down with:

```powershell
docker compose -f compose.cluster.yml down -v
```

Docker Desktop host-network behavior varies by version and configuration. On Windows, a native/WSL Redis cluster or a Compose topology configured with externally reachable announced addresses may be more predictable. Do not treat successful seed connection alone as a cluster test: exercise keys in multiple hash slots so topology routing and multi-node scanning are covered.
