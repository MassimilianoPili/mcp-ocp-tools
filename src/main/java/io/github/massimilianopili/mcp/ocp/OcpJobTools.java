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
public class OcpJobTools {

    private final WebClient webClient;
    private final OcpProperties props;

    public OcpJobTools(
            @Qualifier("ocpWebClient") WebClient webClient,
            OcpProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "ocp_list_jobs",
          description = "Elenca i Job in un namespace")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listJobs(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getBatchV1Base() + "/namespaces/" + ns + "/jobs")
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
                        result.put("active", status.getOrDefault("active", 0));
                        result.put("succeeded", status.getOrDefault("succeeded", 0));
                        result.put("failed", status.getOrDefault("failed", 0));
                        result.put("startTime", status.getOrDefault("startTime", ""));
                        result.put("completionTime", status.getOrDefault("completionTime", ""));
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero job: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_list_cronjobs",
          description = "Elenca i CronJob in un namespace")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listCronJobs(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getBatchV1Base() + "/namespaces/" + ns + "/cronjobs")
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
                        result.put("schedule", spec.getOrDefault("schedule", ""));
                        result.put("suspend", spec.getOrDefault("suspend", false));
                        Map<String, Object> status = (Map<String, Object>) item.getOrDefault("status", Map.of());
                        result.put("lastScheduleTime", status.getOrDefault("lastScheduleTime", ""));
                        List<Map<String, Object>> activeJobs = (List<Map<String, Object>>) status.getOrDefault("active", List.of());
                        result.put("activeJobs", activeJobs.size());
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero cronjob: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_get_job",
          description = "Recupera i dettagli di un Job")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getJob(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace,
            @ToolParam(description = "Nome del Job") String name) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getBatchV1Base() + "/namespaces/" + ns + "/jobs/" + name)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero job " + name + ": " + e.getMessage())));
    }
}
