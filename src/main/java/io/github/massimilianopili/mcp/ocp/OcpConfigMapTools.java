package io.github.massimilianopili.mcp.ocp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class OcpConfigMapTools {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final OcpProperties props;

    public OcpConfigMapTools(
            @Qualifier("ocpWebClient") WebClient webClient,
            OcpProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "ocp_list_configmaps",
          description = "Lists ConfigMaps in a namespace")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listConfigMaps(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getApiV1Base() + "/namespaces/" + ns + "/configmaps")
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
                        result.put("creationTimestamp", metadata.getOrDefault("creationTimestamp", ""));
                        Map<String, Object> data = (Map<String, Object>) item.getOrDefault("data", Map.of());
                        result.put("keys", new ArrayList<>(data.keySet()));
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero configmap: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_get_configmap",
          description = "Retrieves a ConfigMap with its data contents")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getConfigMap(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace,
            @ToolParam(description = "ConfigMap name") String name) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getApiV1Base() + "/namespaces/" + ns + "/configmaps/" + name)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero configmap " + name + ": " + e.getMessage())));
    }

    @ReactiveTool(name = "ocp_create_configmap",
          description = "Creates a new ConfigMap with the specified JSON data")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createConfigMap(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace,
            @ToolParam(description = "ConfigMap name") String name,
            @ToolParam(description = "Data as JSON, e.g.: {\"key1\":\"value1\",\"key2\":\"value2\"}") String dataJson) {
        String ns = props.resolveNamespace(namespace);
        return Mono.defer(() -> {
            Map<String, String> data;
            try {
                data = MAPPER.readValue(dataJson, new TypeReference<>() {});
            } catch (Exception ex) {
                return Mono.just(Map.<String, Object>of("error", "JSON dati non valido: " + ex.getMessage()));
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("apiVersion", "v1");
            body.put("kind", "ConfigMap");
            body.put("metadata", Map.of("name", name, "namespace", ns));
            body.put("data", data);

            return webClient.post()
                    .uri(props.getApiV1Base() + "/namespaces/" + ns + "/configmaps")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(r -> (Map<String, Object>) r);
        })
        .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione configmap " + name + ": " + e.getMessage())));
    }

    @ReactiveTool(name = "ocp_update_configmap",
          description = "Updates the data of an existing ConfigMap")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> updateConfigMap(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace,
            @ToolParam(description = "ConfigMap name") String name,
            @ToolParam(description = "New data as JSON") String dataJson) {
        String ns = props.resolveNamespace(namespace);
        return Mono.defer(() -> {
            Map<String, String> data;
            try {
                data = MAPPER.readValue(dataJson, new TypeReference<>() {});
            } catch (Exception ex) {
                return Mono.just(Map.<String, Object>of("error", "JSON dati non valido: " + ex.getMessage()));
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("apiVersion", "v1");
            body.put("kind", "ConfigMap");
            body.put("metadata", Map.of("name", name, "namespace", ns));
            body.put("data", data);

            return webClient.put()
                    .uri(props.getApiV1Base() + "/namespaces/" + ns + "/configmaps/" + name)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(r -> (Map<String, Object>) r);
        })
        .onErrorResume(e -> Mono.just(Map.of("error", "Errore aggiornamento configmap " + name + ": " + e.getMessage())));
    }

    @ReactiveTool(name = "ocp_delete_configmap",
          description = "Deletes a ConfigMap")
    public Mono<Map<String, Object>> deleteConfigMap(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace,
            @ToolParam(description = "Name of the ConfigMap to delete") String name) {
        String ns = props.resolveNamespace(namespace);
        return webClient.delete()
                .uri(props.getApiV1Base() + "/namespaces/" + ns + "/configmaps/" + name)
                .retrieve()
                .toBodilessEntity()
                .map(r -> Map.<String, Object>of("status", r.getStatusCode().value(), "deleted", true, "configmap", name, "namespace", ns))
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore eliminazione configmap " + name + ": " + e.getMessage())));
    }
}
