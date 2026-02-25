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
public class OcpRouteTools {

    private final WebClient webClient;
    private final OcpProperties props;

    public OcpRouteTools(
            @Qualifier("ocpWebClient") WebClient webClient,
            OcpProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @ReactiveTool(name = "ocp_list_routes",
          description = "Elenca le route OpenShift in un namespace")
    @SuppressWarnings("unchecked")
    public Mono<List<Map<String, Object>>> listRoutes(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getRouteV1Base() + "/namespaces/" + ns + "/routes")
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
                        result.put("host", spec.getOrDefault("host", ""));
                        result.put("path", spec.getOrDefault("path", "/"));
                        Map<String, Object> to = (Map<String, Object>) spec.getOrDefault("to", Map.of());
                        result.put("serviceName", to.getOrDefault("name", ""));
                        Map<String, Object> tls = (Map<String, Object>) spec.getOrDefault("tls", Map.of());
                        result.put("tlsTermination", tls.getOrDefault("termination", "none"));
                        return result;
                    }).toList();
                })
                .onErrorResume(e -> Mono.just(List.of(Map.of("error", "Errore recupero route: " + e.getMessage()))));
    }

    @ReactiveTool(name = "ocp_get_route",
          description = "Recupera i dettagli di una route OpenShift")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getRoute(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace,
            @ToolParam(description = "Nome della route") String name) {
        String ns = props.resolveNamespace(namespace);
        return webClient.get()
                .uri(props.getRouteV1Base() + "/namespaces/" + ns + "/routes/" + name)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore recupero route " + name + ": " + e.getMessage())));
    }

    @ReactiveTool(name = "ocp_create_route",
          description = "Crea una route OpenShift per esporre un servizio")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> createRoute(
            @ToolParam(description = "Namespace (opzionale)", required = false) String namespace,
            @ToolParam(description = "Nome della route") String name,
            @ToolParam(description = "Nome del servizio da esporre") String serviceName,
            @ToolParam(description = "Porta del servizio target") int port,
            @ToolParam(description = "Hostname personalizzato (opzionale)", required = false) String hostname,
            @ToolParam(description = "Terminazione TLS: edge, passthrough, reencrypt (opzionale)", required = false) String tlsTermination) {
        String ns = props.resolveNamespace(namespace);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("apiVersion", "route.openshift.io/v1");
        body.put("kind", "Route");
        body.put("metadata", Map.of("name", name, "namespace", ns));

        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("to", Map.of("kind", "Service", "name", serviceName));
        spec.put("port", Map.of("targetPort", port));
        if (hostname != null && !hostname.isBlank()) {
            spec.put("host", hostname);
        }
        if (tlsTermination != null && !tlsTermination.isBlank()) {
            spec.put("tls", Map.of("termination", tlsTermination));
        }
        body.put("spec", spec);

        return webClient.post()
                .uri(props.getRouteV1Base() + "/namespaces/" + ns + "/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(r -> (Map<String, Object>) r)
                .onErrorResume(e -> Mono.just(Map.of("error", "Errore creazione route " + name + ": " + e.getMessage())));
    }
}
