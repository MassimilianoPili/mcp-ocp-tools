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
public class OcpProjectTools {

    private final WebClient webClient;
    private final OcpProperties props;

    public OcpProjectTools(
            @Qualifier("ocpWebClient") WebClient webClient,
            OcpProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "ocp_list_projects",
          description = "Lists all projects (namespaces) in the OpenShift cluster")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listProjects() {
        return webClient.get()
                .uri(props.getProjectV1Base() + "/projects")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    if (!response.containsKey("items")) return List.<Map<String, Object>>of();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                    return items.stream().map(item -> {
                        Map<String, Object> result = new LinkedHashMap<>();
                        Map<String, Object> metadata = (Map<String, Object>) item.getOrDefault("metadata", Map.of());
                        result.put("name", metadata.getOrDefault("name", ""));
                        Map<String, Object> annotations = (Map<String, Object>) metadata.getOrDefault("annotations", Map.of());
                        result.put("displayName", annotations.getOrDefault("openshift.io/display-name", ""));
                        result.put("description", annotations.getOrDefault("openshift.io/description", ""));
                        Map<String, Object> status = (Map<String, Object>) item.getOrDefault("status", Map.of());
                        result.put("phase", status.getOrDefault("phase", ""));
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero progetti: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_get_project",
          description = "Retrieves details of an OpenShift project")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getProject(
            @ToolParam(description = "Project name") String name) {
        return webClient.get()
                .uri(props.getProjectV1Base() + "/projects/" + name)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero progetto " + name + ": " + e.getMessage())));
    }

    @ReactiveTool(name = "ocp_create_project",
          description = "Creates a new OpenShift project (ProjectRequest)")
    public Mono<Map<String, Object>> createProject(
            @ToolParam(description = "Project name") String name,
            @ToolParam(description = "Display name", required = false) String displayName,
            @ToolParam(description = "Project description", required = false) String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("apiVersion", "project.openshift.io/v1");
        body.put("kind", "ProjectRequest");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", name);
        body.put("metadata", metadata);
        if (displayName != null && !displayName.isBlank()) {
            body.put("displayName", displayName);
        }
        if (description != null && !description.isBlank()) {
            body.put("description", description);
        }

        return webClient.post()
                .uri(props.getProjectV1Base() + "/projectrequests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione progetto " + name + ": " + e.getMessage())));
    }

    @ReactiveTool(name = "ocp_delete_project",
          description = "Deletes an OpenShift project")
    public Mono<Map<String, Object>> deleteProject(
            @ToolParam(description = "Name of the project to delete") String name) {
        return webClient.delete()
                .uri(props.getProjectV1Base() + "/projects/" + name)
                .retrieve()
                .toBodilessEntity()
                .map(r -> Map.<String, Object>of("status", r.getStatusCode().value(), "deleted", true, "project", name))
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore eliminazione progetto " + name + ": " + e.getMessage())));
    }
}
