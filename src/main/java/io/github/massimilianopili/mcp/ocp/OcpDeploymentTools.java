package io.github.massimilianopili.mcp.ocp;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

@Service
@ConditionalOnProperty(name = "mcp.ocp.token")
public class OcpDeploymentTools {

    private final WebClient webClient;
    private final OcpProperties props;

    public OcpDeploymentTools(
            @Qualifier("ocpWebClient") WebClient webClient,
            OcpProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "ocp_list_deployments",
          description = "Lists deployments in an OpenShift namespace")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listDeployments(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getAppsV1Base() + "/namespaces/" + ns + "/deployments")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("items")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                    return items.stream().map(item -> {
                        Map<String, Object> result = new LinkedHashMap<>();
                        Map<String, Object> metadata = (Map<String, Object>) item.getOrDefault("metadata", Map.of());
                        result.put("name", metadata.getOrDefault("name", ""));
                        result.put("namespace", metadata.getOrDefault("namespace", ""));
                        Map<String, Object> spec = (Map<String, Object>) item.getOrDefault("spec", Map.of());
                        result.put("replicas", spec.getOrDefault("replicas", 0));
                        Map<String, Object> status = (Map<String, Object>) item.getOrDefault("status", Map.of());
                        result.put("readyReplicas", status.getOrDefault("readyReplicas", 0));
                        result.put("availableReplicas", status.getOrDefault("availableReplicas", 0));
                        result.put("updatedReplicas", status.getOrDefault("updatedReplicas", 0));
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero deployment: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_get_deployment",
          description = "Retrieves details of a deployment")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getDeployment(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace,
            @ToolParam(description = "Deployment name") String name) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getAppsV1Base() + "/namespaces/" + ns + "/deployments/" + name)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero deployment " + name + ": " + e.getMessage())));
    }

    @ReactiveTool(name = "ocp_scale_deployment",
          description = "Scales a deployment to the specified number of replicas")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> scaleDeployment(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace,
            @ToolParam(description = "Deployment name") String name,
            @ToolParam(description = "Desired number of replicas") int replicas) {
        String ns = props.resolveNamespace(namespace);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("apiVersion", "autoscaling/v1");
        body.put("kind", "Scale");
        body.put("metadata", Map.of("name", name, "namespace", ns));
        body.put("spec", Map.of("replicas", replicas));

        return webClient.put()
                .uri(props.getAppsV1Base() + "/namespaces/" + ns + "/deployments/" + name + "/scale")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("deployment", name);
                    result.put("namespace", ns);
                    result.put("replicas", replicas);
                    result.put("status", "scaled");
                    return (Map<String, Object>) result;
                })
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore scaling deployment " + name + ": " + e.getMessage())));
    }

    @ReactiveTool(name = "ocp_restart_deployment",
          description = "Performs a rollout restart of a deployment (updates the restartedAt annotation)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> restartDeployment(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace,
            @ToolParam(description = "Deployment name") String name) {
        String ns = props.resolveNamespace(namespace);
        String patchBody = "{\"spec\":{\"template\":{\"metadata\":{\"annotations\":" +
                "{\"kubectl.kubernetes.io/restartedAt\":\"" + Instant.now().toString() + "\"}}}}}";

        return webClient.patch()
                .uri(props.getAppsV1Base() + "/namespaces/" + ns + "/deployments/" + name)
                .contentType(MediaType.valueOf("application/strategic-merge-patch+json"))
                .bodyValue(patchBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("deployment", name);
                    result.put("namespace", ns);
                    result.put("status", "restart triggered");
                    return (Map<String, Object>) result;
                })
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore restart deployment " + name + ": " + e.getMessage())));
    }
}
