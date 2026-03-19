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
public class OcpImageStreamTools {

    private final WebClient webClient;
    private final OcpProperties props;

    public OcpImageStreamTools(
            @Qualifier("ocpWebClient") WebClient webClient,
            OcpProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "ocp_list_imagestreams",
          description = "Lists ImageStreams in an OpenShift namespace")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listImageStreams(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getImageV1Base() + "/namespaces/" + ns + "/imagestreams")
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
                        result.put("dockerImageRepository", status.getOrDefault("dockerImageRepository", ""));
                        List<Map<String, Object>> tags = (List<Map<String, Object>>) status.getOrDefault("tags", List.of());
                        result.put("tags", tags.stream().map(t -> t.getOrDefault("tag", "")).toList());
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero imagestream: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_get_imagestream",
          description = "Retrieves details of an ImageStream (tags, associated images)")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getImageStream(
            @ToolParam(description = "Namespace (optional)", required = false) String namespace,
            @ToolParam(description = "ImageStream name") String name) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getImageV1Base() + "/namespaces/" + ns + "/imagestreams/" + name)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero imagestream " + name + ": " + e.getMessage())));
    }
}
