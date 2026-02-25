package io.github.massimilianopili.mcp.ocp;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

@Configuration
@ConditionalOnProperty(name = "mcp.ocp.token")
public class OcpConfig {

    private static final Logger log = LoggerFactory.getLogger(OcpConfig.class);

    @Bean(name = "ocpWebClient")
    public WebClient ocpWebClient(OcpProperties props) throws SSLException {
        WebClient.Builder builder = WebClient.builder()
                .defaultHeader("Authorization", "Bearer " + props.getToken())
                .defaultHeader("Accept", "application/json")
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                        .build());

        if (props.isSkipTlsVerify()) {
            log.info("OCP WebClient: TLS verification disabilitata");
            SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            HttpClient httpClient = HttpClient.create()
                    .secure(spec -> spec.sslContext(sslContext));
            builder.clientConnector(new ReactorClientHttpConnector(httpClient));
        }

        return builder.build();
    }
}
