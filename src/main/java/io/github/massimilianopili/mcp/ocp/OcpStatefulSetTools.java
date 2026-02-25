package io.github.massimilianopili.mcp.ocp;

import io.github.massimilianopili.ai.reactive.annotation.ReactiveTool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
@ConditionalOnProperty(name = "mcp.ocp.token")
public class OcpStatefulSetTools {

    private final WebClient webClient;
    private final OcpProperties props;

    public OcpStatefulSetTools(
            @Qualifier("ocpWebClient") WebClient webClient,
            OcpProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "ocp_list_statefulsets",
          description = "Elenca gli StatefulSet in un namespace")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listStatefulSets(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getAppsV1Base() + "/namespaces/" + ns + "/statefulsets")
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
                        result.put("serviceName", spec.getOrDefault("serviceName", ""));
                        Map<String, Object> status = (Map<String, Object>) item.getOrDefault("status", Map.of());
                        result.put("readyReplicas", status.getOrDefault("readyReplicas", 0));
                        result.put("currentReplicas", status.getOrDefault("currentReplicas", 0));
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero statefulset: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_get_statefulset",
          description = "Recupera i dettagli di uno StatefulSet")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getStatefulSet(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace,
            @ToolParam(description = "Nome dello StatefulSet") String name) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getAppsV1Base() + "/namespaces/" + ns + "/statefulsets/" + name)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero statefulset " + name + ": " + e.getMessage())));
    }

    @ReactiveTool(name = "ocp_scale_statefulset",
          description = "Scala uno StatefulSet al numero di repliche indicato")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> scaleStatefulSet(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace,
            @ToolParam(description = "Nome dello StatefulSet") String name,
            @ToolParam(description = "Numero di repliche desiderate") int replicas) {
        String ns = props.resolveNamespace(namespace);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("apiVersion", "autoscaling/v1");
        body.put("kind", "Scale");
        body.put("metadata", Map.of("name", name, "namespace", ns));
        body.put("spec", Map.of("replicas", replicas));

        return webClient.put()
                .uri(props.getAppsV1Base() + "/namespaces/" + ns + "/statefulsets/" + name + "/scale")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("statefulset", name);
                    result.put("namespace", ns);
                    result.put("replicas", replicas);
                    result.put("status", "scaled");
                    return (Map<String, Object>) result;
                })
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore scaling statefulset " + name + ": " + e.getMessage())));
    }
}
