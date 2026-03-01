# MCP OpenShift Tools

Spring Boot starter providing 49 MCP tools for OpenShift Container Platform 4. Covers projects, pods, deployments, routes, secrets, builds, nodes, and more via the Kubernetes and OpenShift REST APIs.

## Installation

```xml
<dependency>
    <groupId>io.github.massimilianopili</groupId>
    <artifactId>mcp-ocp-tools</artifactId>
    <version>0.1.0</version>
</dependency>
```

Requires Java 21+, Spring AI 1.0.0+, and [spring-ai-reactive-tools](https://github.com/MassimilianoPili/spring-ai-reactive-tools) 0.3.0+.

## Tools (49)

| Class | Count | Description |
|-------|-------|-------------|
| `OcpProjectTools` | 4 | List, get, create, delete projects |
| `OcpPodTools` | 5 | List pods, get details, logs, delete |
| `OcpDeploymentTools` | 4 | List, get, scale, rollout restart |
| `OcpServiceTools` | 2 | List and inspect K8s services |
| `OcpRouteTools` | 3 | List, get, create routes (TLS support) |
| `OcpConfigMapTools` | 5 | Full CRUD for ConfigMaps |
| `OcpSecretTools` | 2 | List and inspect secret metadata (values never exposed) |
| `OcpEventTools` | 2 | Namespace events and resource-filtered events |
| `OcpNodeTools` | 3 | List nodes, details, status |
| `OcpBuildTools` | 3 | BuildConfigs, builds, trigger build |
| `OcpImageStreamTools` | 2 | List and inspect ImageStreams |
| `OcpPvcTools` | 2 | List and inspect PVCs |
| `OcpStatefulSetTools` | 3 | List, get, scale StatefulSets |
| `OcpJobTools` | 3 | Jobs and CronJobs |
| `OcpClusterTools` | 3 | Cluster version, operators, health check |
| `OcpResourceQuotaTools` | 2 | Resource quota limits and usage |

## Configuration

```properties
# Required — enables all OCP tools
MCP_OCP_TOKEN=sha256~xxxxxxxxxxxxxxxxxxxx       # oc whoami -t

# Optional
MCP_OCP_SERVER=https://api.openshift.example.com:6443
MCP_OCP_SKIP_TLS_VERIFY=false                   # default: false
MCP_OCP_NAMESPACE=my-project                    # default: "default"
```

## How It Works

- Uses `@ReactiveTool` ([spring-ai-reactive-tools](https://github.com/MassimilianoPili/spring-ai-reactive-tools)) for async `Mono<T>` methods
- Auto-configured via `OcpToolsAutoConfiguration` with `@ConditionalOnProperty(name = "mcp.ocp.token")`
- Namespace cascading: tool parameter > config property > "default"
- Secret values are never exposed — only metadata is returned
- Multiple K8s/OCP API groups: Core, Apps, Batch, Route, Build, Image, Project, Config

## Requirements

- Java 21+
- Spring Boot 3.4+ with WebFlux
- Spring AI 1.0.0+
- spring-ai-reactive-tools 0.3.0+

## License

[MIT License](LICENSE)
