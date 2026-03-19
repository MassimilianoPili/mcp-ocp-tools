package io.github.massimilianopili.mcp.ocp;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
@ConditionalOnProperty(name = "mcp.ocp.token")
public class OcpNodeTools {

    private final WebClient webClient;
    private final OcpProperties props;

    public OcpNodeTools(
            @Qualifier("ocpWebClient") WebClient webClient,
            OcpProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "ocp_list_nodes",
          description = "Lists all nodes in the OpenShift cluster")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listNodes() {
        return webClient.get()
                .uri(props.getApiV1Base() + "/nodes")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("items")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                    return items.stream().map(item -> {
                        Map<String, Object> result = new LinkedHashMap<>();
                        Map<String, Object> metadata = (Map<String, Object>) item.getOrDefault("metadata", Map.of());
                        result.put("name", metadata.getOrDefault("name", ""));
                        Map<String, Object> labels = (Map<String, Object>) metadata.getOrDefault("labels", Map.of());
                        result.put("role", labels.containsKey("node-role.kubernetes.io/master") ? "master" :
                                labels.containsKey("node-role.kubernetes.io/worker") ? "worker" :
                                labels.containsKey("node-role.kubernetes.io/infra") ? "infra" : "unknown");
                        Map<String, Object> status = (Map<String, Object>) item.getOrDefault("status", Map.of());
                        List<Map<String, Object>> conditions = (List<Map<String, Object>>) status.getOrDefault("conditions", List.of());
                        conditions.stream()
                                .filter(c -> "Ready".equals(c.get("type")))
                                .findFirst()
                                .ifPresent(c -> result.put("ready", c.getOrDefault("status", "")));
                        Map<String, Object> nodeInfo = (Map<String, Object>) status.getOrDefault("nodeInfo", Map.of());
                        result.put("kubeletVersion", nodeInfo.getOrDefault("kubeletVersion", ""));
                        result.put("osImage", nodeInfo.getOrDefault("osImage", ""));
                        result.put("architecture", nodeInfo.getOrDefault("architecture", ""));
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero nodi: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_get_node",
          description = "Retrieves details of a cluster node")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getNode(
            @ToolParam(description = "Node name") String name) {
        return webClient.get()
                .uri(props.getApiV1Base() + "/nodes/" + name)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero nodo " + name + ": " + e.getMessage())));
    }

    @ReactiveTool(name = "ocp_get_node_status",
          description = "Retrieves detailed node status (conditions, capacity, allocatable resources)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getNodeStatus(
            @ToolParam(description = "Node name") String name) {
        return webClient.get()
                .uri(props.getApiV1Base() + "/nodes/" + name + "/status")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("name", name);
                    Map<String, Object> status = (Map<String, Object>) response.getOrDefault("status", Map.of());
                    result.put("conditions", status.getOrDefault("conditions", List.of()));
                    result.put("capacity", status.getOrDefault("capacity", Map.of()));
                    result.put("allocatable", status.getOrDefault("allocatable", Map.of()));
                    result.put("addresses", status.getOrDefault("addresses", List.of()));
                    return result;
                })
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero stato nodo " + name + ": " + e.getMessage())));
    }
}
