package io.github.massimilianopili.mcp.ocp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp.ocp")
public class OcpProperties {

    private String server;
    private String token;
    private boolean skipTlsVerify = false;
    private String namespace;

    public String getServer() { return server; }
    public void setServer(String server) { this.server = server; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public boolean isSkipTlsVerify() { return skipTlsVerify; }
    public void setSkipTlsVerify(boolean skipTlsVerify) { this.skipTlsVerify = skipTlsVerify; }

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }

    /** Base URL Kubernetes core API: {server}/api/v1 */
    public String getApiV1Base() {
        return stripTrailingSlash(server) + "/api/v1";
    }

    /** Base URL Kubernetes Apps API: {server}/apis/apps/v1 */
    public String getAppsV1Base() {
        return stripTrailingSlash(server) + "/apis/apps/v1";
    }

    /** Base URL Kubernetes Batch API: {server}/apis/batch/v1 */
    public String getBatchV1Base() {
        return stripTrailingSlash(server) + "/apis/batch/v1";
    }

    /** Base URL OpenShift Route API: {server}/apis/route.openshift.io/v1 */
    public String getRouteV1Base() {
        return stripTrailingSlash(server) + "/apis/route.openshift.io/v1";
    }

    /** Base URL OpenShift Build API: {server}/apis/build.openshift.io/v1 */
    public String getBuildV1Base() {
        return stripTrailingSlash(server) + "/apis/build.openshift.io/v1";
    }

    /** Base URL OpenShift Image API: {server}/apis/image.openshift.io/v1 */
    public String getImageV1Base() {
        return stripTrailingSlash(server) + "/apis/image.openshift.io/v1";
    }

    /** Base URL OpenShift Project API: {server}/apis/project.openshift.io/v1 */
    public String getProjectV1Base() {
        return stripTrailingSlash(server) + "/apis/project.openshift.io/v1";
    }

    /** Base URL Config API (ClusterVersion, ClusterOperators): {server}/apis/config.openshift.io/v1 */
    public String getConfigV1Base() {
        return stripTrailingSlash(server) + "/apis/config.openshift.io/v1";
    }

    /** Risolve il namespace effettivo: il parametro ha priorita', poi il default da config, infine "default" */
    public String resolveNamespace(String nsParam) {
        if (nsParam != null && !nsParam.isBlank()) return nsParam;
        if (namespace != null && !namespace.isBlank()) return namespace;
        return "default";
    }

    private String stripTrailingSlash(String url) {
        return (url != null && url.endsWith("/")) ? url.substring(0, url.length() - 1) : url;
    }
}
