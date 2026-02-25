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
public class OcpPvcTools {

    private final WebClient webClient;
    private final OcpProperties props;

    public OcpPvcTools(
            @Qualifier("ocpWebClient") WebClient webClient,
            OcpProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "ocp_list_pvcs",
          description = "Elenca i PersistentVolumeClaim in un namespace")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listPvcs(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getApiV1Base() + "/namespaces/" + ns + "/persistentvolumeclaims")
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
                        result.put("storageClassName", spec.getOrDefault("storageClassName", ""));
                        result.put("accessModes", spec.getOrDefault("accessModes", List.of()));
                        Map<String, Object> resources = (Map<String, Object>) spec.getOrDefault("resources", Map.of());
                        Map<String, Object> requests = (Map<String, Object>) resources.getOrDefault("requests", Map.of());
                        result.put("storage", requests.getOrDefault("storage", ""));
                        Map<String, Object> status = (Map<String, Object>) item.getOrDefault("status", Map.of());
                        result.put("phase", status.getOrDefault("phase", ""));
                        result.put("volumeName", spec.getOrDefault("volumeName", ""));
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero PVC: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_get_pvc",
          description = "Recupera i dettagli di un PersistentVolumeClaim")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getPvc(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace,
            @ToolParam(description = "Nome del PVC") String name) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getApiV1Base() + "/namespaces/" + ns + "/persistentvolumeclaims/" + name)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero PVC " + name + ": " + e.getMessage())));
    }
}
