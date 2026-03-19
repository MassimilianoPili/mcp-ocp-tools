package io.github.massimilianopili.mcp.ocp;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
@ConditionalOnProperty(name = "mcp.ocp.token")
public class OcpClusterTools {

    private final WebClient webClient;
    private final OcpProperties props;

    public OcpClusterTools(
            @Qualifier("ocpWebClient") WebClient webClient,
            OcpProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "ocp_get_cluster_version",
          description = "Retrieves the OpenShift cluster version")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getClusterVersion() {
        return webClient.get()
                .uri(props.getConfigV1Base() + "/clusterversions/version")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    Map<String, Object> status = (Map<String, Object>) response.getOrDefault("status", Map.of());
                    Map<String, Object> desired = (Map<String, Object>) status.getOrDefault("desired", Map.of());
                    result.put("version", desired.getOrDefault("version", ""));
                    result.put("image", desired.getOrDefault("image", ""));
                    result.put("channel", status.getOrDefault("channel", ""));
                    List<Map<String, Object>> conditions = (List<Map<String, Object>>) status.getOrDefault("conditions", List.of());
                    result.put("conditions", conditions.stream().map(c -> {
                        Map<String, Object> cond = new LinkedHashMap<>();
                        cond.put("type", c.getOrDefault("type", ""));
                        cond.put("status", c.getOrDefault("status", ""));
                        cond.put("message", c.getOrDefault("message", ""));
                        return cond;
                    }).toList());
                    return result;
                })
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero versione cluster: " + e.getMessage())));
    }

    @ReactiveTool(name = "ocp_list_cluster_operators",
          description = "Lists OpenShift cluster operators with their status")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listClusterOperators() {
        return webClient.get()
                .uri(props.getConfigV1Base() + "/clusteroperators")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("items")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                    return items.stream().map(item -> {
                        Map<String, Object> result = new LinkedHashMap<>();
                        Map<String, Object> metadata = (Map<String, Object>) item.getOrDefault("metadata", Map.of());
                        result.put("name", metadata.getOrDefault("name", ""));
                        Map<String, Object> status = (Map<String, Object>) item.getOrDefault("status", Map.of());
                        List<Map<String, Object>> conditions = (List<Map<String, Object>>) status.getOrDefault("conditions", List.of());
                        Map<String, String> condMap = new LinkedHashMap<>();
                        for (Map<String, Object> c : conditions) {
                            condMap.put((String) c.getOrDefault("type", ""), (String) c.getOrDefault("status", ""));
                        }
                        result.put("available", condMap.getOrDefault("Available", ""));
                        result.put("progressing", condMap.getOrDefault("Progressing", ""));
                        result.put("degraded", condMap.getOrDefault("Degraded", ""));
                        List<Map<String, Object>> versions = (List<Map<String, Object>>) status.getOrDefault("versions", List.of());
                        if (!versions.isEmpty()) {
                            result.put("version", versions.get(0).getOrDefault("version", ""));
                        }
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero cluster operators: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_check_api_health",
          description = "Checks if the OpenShift cluster API server is reachable")
    public Mono<Map<String, Object>> checkApiHealth() {
        String baseUrl = props.getServer();
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return webClient.get()
                .uri(baseUrl + "/healthz")
                .retrieve()
                .bodyToMono(String.class)
                .map(body -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("status", "ok".equals(body.trim()) ? "healthy" : body.trim());
                    result.put("server", props.getServer());
                    return result;
                })
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore health check API: " + e.getMessage(), "server", String.valueOf(props.getServer()))));
    }
}
