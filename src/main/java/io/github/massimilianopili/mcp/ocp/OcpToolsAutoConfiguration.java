package io.github.massimilianopili.mcp.ocp;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnProperty(name = "mcp.ocp.token")
@EnableConfigurationProperties(OcpProperties.class)
@Import({OcpConfig.class,
         OcpProjectTools.class, OcpPodTools.class,
         OcpDeploymentTools.class, OcpServiceTools.class,
         OcpRouteTools.class, OcpConfigMapTools.class,
         OcpSecretTools.class, OcpEventTools.class,
         OcpNodeTools.class, OcpBuildTools.class,
         OcpImageStreamTools.class, OcpPvcTools.class,
         OcpStatefulSetTools.class, OcpJobTools.class,
         OcpClusterTools.class, OcpResourceQuotaTools.class})
public class OcpToolsAutoConfiguration {
    // Tool registrati automaticamente da ReactiveToolAutoConfiguration di spring-ai-reactive-tools
}
