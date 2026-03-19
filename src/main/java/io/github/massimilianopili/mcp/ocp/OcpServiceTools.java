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
public class OcpServiceTools {

    private final WebClient webClient;
    private final OcpProperties props;

    public OcpServiceTools(
            @Qualifier("ocpWebClient") WebClient webClient,
            OcpProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "ocp_list_services",
          description = "Lists Kubernetes services in a namespace")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listServices(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getApiV1Base() + "/namespaces/" + ns + "/services")
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
                        result.put("type", spec.getOrDefault("type", ""));
                        result.put("clusterIP", spec.getOrDefault("clusterIP", ""));
                        result.put("ports", spec.getOrDefault("ports", List.of()));
                        result.put("selector", spec.getOrDefault("selector", Map.of()));
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero servizi: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_get_service",
          description = "Retrieves details of a Kubernetes service")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getService(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace,
            @ToolParam(description = "Service name") String name) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getApiV1Base() + "/namespaces/" + ns + "/services/" + name)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero servizio " + name + ": " + e.getMessage())));
    }
}
