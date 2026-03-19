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
public class OcpEventTools {

    private final WebClient webClient;
    private final OcpProperties props;

    public OcpEventTools(
            @Qualifier("ocpWebClient") WebClient webClient,
            OcpProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "ocp_list_events",
          description = "Lists recent events in a namespace")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listEvents(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getApiV1Base() + "/namespaces/" + ns + "/events")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("items")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                    return items.stream().map(item -> {
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("type", item.getOrDefault("type", ""));
                        result.put("reason", item.getOrDefault("reason", ""));
                        result.put("message", item.getOrDefault("message", ""));
                        result.put("count", item.getOrDefault("count", 0));
                        result.put("lastTimestamp", item.getOrDefault("lastTimestamp", ""));
                        Map<String, Object> involved = (Map<String, Object>) item.getOrDefault("involvedObject", Map.of());
                        result.put("objectKind", involved.getOrDefault("kind", ""));
                        result.put("objectName", involved.getOrDefault("name", ""));
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero eventi: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_list_events_for_resource",
          description = "Lists events related to a specific resource (pod, deployment, etc.)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listEventsForResource(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace,
            @ToolParam(description = "Resource name") String resourceName,
            @ToolParam(description = "Resource kind: Pod, Deployment, Service, etc. (optional)", required = false) String resourceKind) {
        String ns = props.resolveNamespace(namespace);
        StringBuilder uri = new StringBuilder()
                .append(props.getApiV1Base())
                .append("/namespaces/").append(ns)
                .append("/events?fieldSelector=involvedObject.name=").append(resourceName);
        if (resourceKind != null && !resourceKind.isBlank()) {
            uri.append(",involvedObject.kind=").append(resourceKind);
        }

        return webClient.get()
                .uri(uri.toString())
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("items")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                    return items.stream().map(item -> {
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("type", item.getOrDefault("type", ""));
                        result.put("reason", item.getOrDefault("reason", ""));
                        result.put("message", item.getOrDefault("message", ""));
                        result.put("count", item.getOrDefault("count", 0));
                        result.put("lastTimestamp", item.getOrDefault("lastTimestamp", ""));
                        result.put("firstTimestamp", item.getOrDefault("firstTimestamp", ""));
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero eventi per " + resourceName + ": " + e.getMessage()))));
    }
}
