# MCP OpenShift Tools

Spring Boot starter che fornisce tool MCP per la gestione di cluster OpenShift Container Platform 4 (progetti, pod, deployment, route, secret, build, node, ecc.) via Kubernetes e OpenShift REST API. Pubblicato su Maven Central come `io.github.massimilianopili:mcp-ocp-tools`.

## Build

```bash
# Build
/opt/maven/bin/mvn clean compile

# Install locale (senza GPG)
/opt/maven/bin/mvn clean install -Dgpg.skip=true

# Deploy su Maven Central
/opt/maven/bin/mvn clean deploy
```

Java 17+ richiesto. Maven: `/opt/maven/bin/mvn`.

## Struttura Progetto

```
src/main/java/io/github/massimilianopili/mcp/ocp/
├── OcpProperties.java              # @ConfigurationProperties(prefix = "mcp.ocp")
├── OcpConfig.java                  # WebClient bean (Bearer token + TLS skip opzionale)
├── OcpToolsAutoConfiguration.java  # Spring Boot auto-config
├── OcpProjectTools.java            # @ReactiveTool: progetti OpenShift
├── OcpPodTools.java                # @ReactiveTool: pod, log
├── OcpDeploymentTools.java         # @ReactiveTool: deployment, scale, restart
├── OcpServiceTools.java            # @ReactiveTool: service K8s
├── OcpRouteTools.java              # @ReactiveTool: route OpenShift (TLS)
├── OcpConfigMapTools.java          # @ReactiveTool: CRUD ConfigMap
├── OcpSecretTools.java             # @ReactiveTool: metadata secret (valori MAI esposti)
├── OcpEventTools.java              # @ReactiveTool: eventi, troubleshooting
├── OcpNodeTools.java               # @ReactiveTool: nodi cluster
├── OcpBuildTools.java              # @ReactiveTool: BuildConfig, trigger build
├── OcpImageStreamTools.java        # @ReactiveTool: ImageStream
├── OcpPvcTools.java                # @ReactiveTool: PersistentVolumeClaim
├── OcpStatefulSetTools.java        # @ReactiveTool: StatefulSet, scale
├── OcpJobTools.java                # @ReactiveTool: Job, CronJob
├── OcpClusterTools.java            # @ReactiveTool: versione cluster, operator, health
└── OcpResourceQuotaTools.java      # @ReactiveTool: quote risorse

src/main/resources/META-INF/spring/
└── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## Tool (49 totali)

### OcpProjectTools (4)
- `ocp_list_projects` — Lista progetti (namespace) OpenShift
- `ocp_get_project` — Dettaglio progetto
- `ocp_create_project` — Crea progetto (displayName, description opzionali)
- `ocp_delete_project` — Elimina progetto

### OcpPodTools (5)
- `ocp_list_pods` — Pod nel namespace
- `ocp_list_all_pods` — Pod in tutti i namespace
- `ocp_get_pod` — Dettaglio pod
- `ocp_get_pod_logs` — Log pod (timeout 60s, default 100 righe, container opzionale)
- `ocp_delete_pod` — Elimina pod

### OcpDeploymentTools (4)
- `ocp_list_deployments` — Deployment nel namespace
- `ocp_get_deployment` — Dettaglio deployment
- `ocp_scale_deployment` — Scala repliche (PUT `/scale`)
- `ocp_restart_deployment` — Rollout restart (PATCH con annotation `kubectl.kubernetes.io/restartedAt`)

### OcpServiceTools (2)
- `ocp_list_services` — Service K8s nel namespace
- `ocp_get_service` — Dettaglio service (type, clusterIP, ports, selectors)

### OcpRouteTools (3)
- `ocp_list_routes` — Route nel namespace
- `ocp_get_route` — Dettaglio route (host, path, TLS)
- `ocp_create_route` — Crea route (TLS: edge/passthrough/reencrypt opzionale)

### OcpConfigMapTools (5)
- `ocp_list_configmaps` — ConfigMap nel namespace
- `ocp_get_configmap` — Dettaglio ConfigMap con dati
- `ocp_create_configmap` — Crea ConfigMap (JSON data)
- `ocp_update_configmap` — Aggiorna dati ConfigMap
- `ocp_delete_configmap` — Elimina ConfigMap

### OcpSecretTools (2)
- `ocp_list_secrets` — Lista secret (solo metadata, valori MAI esposti)
- `ocp_get_secret_metadata` — Metadata secret: tipo, chiavi, annotation, label

### OcpEventTools (2)
- `ocp_list_events` — Eventi recenti nel namespace
- `ocp_list_events_for_resource` — Eventi filtrati per risorsa (fieldSelector)

### OcpNodeTools (3)
- `ocp_list_nodes` — Nodi cluster (role: master/worker/infra)
- `ocp_get_node` — Dettaglio nodo
- `ocp_get_node_status` — Status nodo (conditions, capacity, allocatable)

### OcpBuildTools (3)
- `ocp_list_buildconfigs` — BuildConfig nel namespace
- `ocp_list_builds` — Build eseguiti
- `ocp_trigger_build` — Avvia build da BuildConfig

### OcpImageStreamTools (2)
- `ocp_list_imagestreams` — ImageStream nel namespace
- `ocp_get_imagestream` — Dettaglio ImageStream (tag, registry)

### OcpPvcTools (2)
- `ocp_list_pvcs` — PVC nel namespace
- `ocp_get_pvc` — Dettaglio PVC (storage, accessModes, phase)

### OcpStatefulSetTools (3)
- `ocp_list_statefulsets` — StatefulSet nel namespace
- `ocp_get_statefulset` — Dettaglio StatefulSet
- `ocp_scale_statefulset` — Scala replice (PUT `/scale`)

### OcpJobTools (3)
- `ocp_list_jobs` — Job nel namespace
- `ocp_list_cronjobs` — CronJob nel namespace
- `ocp_get_job` — Dettaglio job (active, succeeded, failed)

### OcpClusterTools (3)
- `ocp_get_cluster_version` — Versione cluster e condizioni upgrade
- `ocp_list_cluster_operators` — Stato operator (available/progressing/degraded)
- `ocp_check_api_health` — Health check su `/healthz`

### OcpResourceQuotaTools (2)
- `ocp_list_resource_quotas` — ResourceQuota nel namespace
- `ocp_get_resource_quota` — Limiti e utilizzo risorse

## Pattern Chiave

- **@ReactiveTool** (spring-ai-reactive-tools): tutti i tool restituiscono `Mono<T>`.
- **Attivazione**: `@ConditionalOnProperty(name = "mcp.ocp.token")`.
- **Namespace cascading**: parametro → config property → "default".
- **API multiple**: Core (`/api/v1`), Apps (`/apis/apps/v1`), Batch (`/apis/batch/v1`), OpenShift-specific (Route, Build, Image, Project, Config).
- **Sicurezza secret**: i valori dei secret non vengono MAI esposti, solo metadata.
- **WebClient**: Bearer token auth, skip TLS opzionale (10MB buffer).
- **Jackson ObjectMapper**: per parsing dati ConfigMap/Secret JSON.

## Configurazione

```properties
# Obbligatoria — abilita tutti i tool OCP
MCP_OCP_TOKEN=sha256~xxxxxxxxxxxxxxxxxxxx       # oc whoami -t

# Opzionali
MCP_OCP_SERVER=https://api.openshift.example.com:6443
MCP_OCP_SKIP_TLS_VERIFY=false                   # default: false
MCP_OCP_NAMESPACE=my-project                    # default: "default"
```

## Dipendenze

- Spring Boot 3.4.1 (spring-boot-autoconfigure, spring-boot-starter-webflux)
- Spring AI 1.0.0 (spring-ai-model)
- spring-ai-reactive-tools 0.2.0

## Maven Central

- GroupId: `io.github.massimilianopili`
- Plugin: `central-publishing-maven-plugin` v0.7.0
- Credenziali: Central Portal token in `~/.m2/settings.xml` (server id: `central`)
