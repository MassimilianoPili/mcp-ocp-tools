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
public class OcpSecretTools {

    private final WebClient webClient;
    private final OcpProperties props;

    public OcpSecretTools(
            @Qualifier("ocpWebClient") WebClient webClient,
            OcpProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "ocp_list_secrets",
          description = "Lists Secrets in a namespace (metadata only, values are not exposed for security)")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listSecrets(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getApiV1Base() + "/namespaces/" + ns + "/secrets")
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
                        result.put("type", item.getOrDefault("type", ""));
                        result.put("creationTimestamp", metadata.getOrDefault("creationTimestamp", ""));
                        Map<String, Object> data = (Map<String, Object>) item.getOrDefault("data", Map.of());
                        result.put("keys", new ArrayList<>(data.keySet()));
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero secret: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_get_secret_metadata",
          description = "Retrieves metadata of a Secret (type, keys, annotations). Values are NOT exposed.")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getSecretMetadata(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace,
            @ToolParam(description = "Secret name") String name) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getApiV1Base() + "/namespaces/" + ns + "/secrets/" + name)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    Map<String, Object> metadata = (Map<String, Object>) response.getOrDefault("metadata", Map.of());
                    result.put("name", metadata.getOrDefault("name", ""));
                    result.put("namespace", metadata.getOrDefault("namespace", ""));
                    result.put("type", response.getOrDefault("type", ""));
                    result.put("creationTimestamp", metadata.getOrDefault("creationTimestamp", ""));
                    result.put("annotations", metadata.getOrDefault("annotations", Map.of()));
                    result.put("labels", metadata.getOrDefault("labels", Map.of()));
                    Map<String, Object> data = (Map<String, Object>) response.getOrDefault("data", Map.of());
                    result.put("keys", new ArrayList<>(data.keySet()));
                    result.put("note", "I valori dei secret non vengono esposti per sicurezza");
                    return result;
                })
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero metadata secret " + name + ": " + e.getMessage())));
    }
}
