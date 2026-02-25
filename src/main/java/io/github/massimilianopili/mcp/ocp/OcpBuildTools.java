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
public class OcpBuildTools {

    private final WebClient webClient;
    private final OcpProperties props;

    public OcpBuildTools(
            @Qualifier("ocpWebClient") WebClient webClient,
            OcpProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "ocp_list_buildconfigs",
          description = "Elenca le BuildConfig in un namespace OpenShift")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listBuildConfigs(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getBuildV1Base() + "/namespaces/" + ns + "/buildconfigs")
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
                        Map<String, Object> strategy = (Map<String, Object>) spec.getOrDefault("strategy", Map.of());
                        result.put("strategyType", strategy.getOrDefault("type", ""));
                        Map<String, Object> source = (Map<String, Object>) spec.getOrDefault("source", Map.of());
                        result.put("sourceType", source.getOrDefault("type", ""));
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero buildconfig: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_list_builds",
          description = "Elenca le Build (esecuzioni di build) in un namespace")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listBuilds(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getBuildV1Base() + "/namespaces/" + ns + "/builds")
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
                        result.put("startTimestamp", status.getOrDefault("startTimestamp", ""));
                        result.put("completionTimestamp", status.getOrDefault("completionTimestamp", ""));
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero build: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_trigger_build",
          description = "Avvia una nuova build da una BuildConfig (instantiate)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> triggerBuild(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace,
            @ToolParam(description = "Nome della BuildConfig") String buildConfigName) {
        String ns = props.resolveNamespace(namespace);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("apiVersion", "build.openshift.io/v1");
        body.put("kind", "BuildRequest");
        body.put("metadata", Map.of("name", buildConfigName));

        return webClient.post()
                .uri(props.getBuildV1Base() + "/namespaces/" + ns + "/buildconfigs/" + buildConfigName + "/instantiate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore trigger build " + buildConfigName + ": " + e.getMessage())));
    }
}
