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
public class OcpPodTools {

    private final WebClient webClient;
    private final OcpProperties props;

    public OcpPodTools(
            @Qualifier("ocpWebClient") WebClient webClient,
            OcpProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "ocp_list_pods",
          description = "Elenca i pod in un namespace OpenShift")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listPods(
            @ToolParam(description = "Namespace (opzionale, usa il default da config)", required = false) String namespace) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getApiV1Base() + "/namespaces/" + ns + "/pods")
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
                        Map<String, Object> status = (Map<String, Object>) item.getOrDefault("status", Map.of());
                        result.put("phase", status.getOrDefault("phase", ""));
                        result.put("podIP", status.getOrDefault("podIP", ""));
                        result.put("startTime", status.getOrDefault("startTime", ""));
                        Map<String, Object> spec = (Map<String, Object>) item.getOrDefault("spec", Map.of());
                        result.put("nodeName", spec.getOrDefault("nodeName", ""));
                        List<Map<String, Object>> containers = (List<Map<String, Object>>) spec.getOrDefault("containers", List.of());
                        result.put("containers", containers.stream().map(c -> c.getOrDefault("name", "")).toList());
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero pod: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_list_all_pods",
          description = "Elenca tutti i pod in tutti i namespace del cluster")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listAllPods() {
        return webClient.get()
                .uri(props.getApiV1Base() + "/pods")
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
                        Map<String, Object> status = (Map<String, Object>) item.getOrDefault("status", Map.of());
                        result.put("phase", status.getOrDefault("phase", ""));
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero pod globale: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_get_pod",
          description = "Recupera i dettagli di un pod specifico")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getPod(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace,
            @ToolParam(description = "Nome del pod") String name) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getApiV1Base() + "/namespaces/" + ns + "/pods/" + name)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero pod " + name + ": " + e.getMessage())));
    }

    @ReactiveTool(name = "ocp_get_pod_logs",
          description = "Recupera i log di un pod (o di un container specifico)",
          timeoutMs = 60000)
    public Mono<String> getPodLogs(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace,
            @ToolParam(description = "Nome del pod") String name,
            @ToolParam(description = "Nome del container (opzionale, per pod multi-container)", required = false) String container,
            @ToolParam(description = "Numero di righe dalla fine (default: 100)", required = false) Integer tailLines) {
        String ns = props.resolveNamespace(namespace);
        int tail = (tailLines != null && tailLines > 0) ? tailLines : 100;
        StringBuilder uri = new StringBuilder()
                .append(props.getApiV1Base())
                .append("/namespaces/").append(ns)
                .append("/pods/").append(name)
                .append("/log?tailLines=").append(tail);
        if (container != null && !container.isBlank()) {
            uri.append("&container=").append(container);
        }

        return webClient.get()
                .uri(uri.toString())
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.just("Errore recupero log pod " + name + ": " + e.getMessage()));
    }

    @ReactiveTool(name = "ocp_delete_pod",
          description = "Elimina un pod dal namespace specificato")
    public Mono<Map<String, Object>> deletePod(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace,
            @ToolParam(description = "Nome del pod da eliminare") String name) {
        String ns = props.resolveNamespace(namespace);
        return webClient.delete()
                .uri(props.getApiV1Base() + "/namespaces/" + ns + "/pods/" + name)
                .retrieve()
                .toBodilessEntity()
                .map(r -> Map.<String, Object>of("status", r.getStatusCode().value(), "deleted", true, "pod", name, "namespace", ns))
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore eliminazione pod " + name + ": " + e.getMessage())));
    }
}
