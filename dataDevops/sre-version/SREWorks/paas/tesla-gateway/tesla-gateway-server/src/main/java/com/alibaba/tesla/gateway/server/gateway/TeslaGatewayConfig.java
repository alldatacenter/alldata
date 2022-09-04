package com.alibaba.tesla.gateway.server.gateway;

import com.alibaba.tesla.gateway.server.cache.GatewayCache;
import com.alibaba.tesla.web.properties.TeslaEnvProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.ProxyProvider;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;

import static org.springframework.cloud.gateway.config.HttpClientProperties.Pool.PoolType.DISABLED;
import static org.springframework.cloud.gateway.config.HttpClientProperties.Pool.PoolType.FIXED;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@SuppressWarnings("all")
@Slf4j
@Configuration
public class TeslaGatewayConfig {

    @Value("${tesla.config.gateway.httpclient.maxIdleTime:60}")
    private Long maxIdleTime;

    @Autowired
    private TeslaEnvProperties envProperties;


    @Bean(name = "teslaRouteDefinitionRouteLocator")
    public RouteLocator routeDefinitionRouteLocator(GatewayProperties properties,
                                                    List<GatewayFilterFactory> GatewayFilters,
                                                    List<RoutePredicateFactory> predicates,
                                                    RouteDefinitionLocator routeDefinitionLocator,
                                                    @Qualifier("webFluxConversionService")
                                                        ConversionService conversionService) {
        log.info("load tesla route define routelocator");
        return new TeslaRouteDefinitionRouteLocator(routeDefinitionLocator, predicates,
            GatewayFilters, properties, conversionService);
    }

    @Bean(name = "teslaFilteringWebHandler")
    public TeslaFilteringWebHandler filteringWebHandler(List<GlobalFilter> globalFilters, GatewayCache gatewayCache,
                                                        @Autowired TeslaEnvProperties envProperties) {
        return new TeslaFilteringWebHandler(globalFilters, gatewayCache, envProperties);
    }

    @Bean(name = "teslaRoutePredicateHandlerMapping")
    public RoutePredicateHandlerMapping routePredicateHandlerMapping(
        TeslaFilteringWebHandler webHandler, RouteLocator routeLocator,
        GlobalCorsProperties globalCorsProperties, Environment environment) {
        return new RoutePredicateHandlerMapping(webHandler, routeLocator,
            globalCorsProperties, environment);
    }

    @Bean
    public HttpClient gatewayHttpClient(HttpClientProperties properties) {
        log.info("create web http client, poolType={}, maxIdleTime={}", properties.getPool().getType(), this.maxIdleTime);

        // configure pool resources
        HttpClientProperties.Pool pool = properties.getPool();

        ConnectionProvider connectionProvider;
        if (pool.getType() == DISABLED) {
            connectionProvider = ConnectionProvider.newConnection();
        }
        else if (pool.getType() == FIXED) {
            connectionProvider = ConnectionProvider.fixed(pool.getName(),
                pool.getMaxConnections(), pool.getAcquireTimeout(), Duration.ofSeconds(maxIdleTime));
        }
        else {
            connectionProvider = ConnectionProvider.elastic(pool.getName(), Duration.ofSeconds(maxIdleTime));
        }

        HttpClient httpClient = HttpClient.create(connectionProvider)
            .tcpConfiguration(tcpClient -> {

                if (properties.getConnectTimeout() != null) {
                    tcpClient = tcpClient.option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        properties.getConnectTimeout());
                }

                // configure proxy if proxy host is set.
                HttpClientProperties.Proxy proxy = properties.getProxy();

                if (StringUtils.hasText(proxy.getHost())) {

                    tcpClient = tcpClient.proxy(proxySpec -> {
                        ProxyProvider.Builder builder = proxySpec
                            .type(ProxyProvider.Proxy.HTTP)
                            .host(proxy.getHost());

                        PropertyMapper map = PropertyMapper.get();

                        map.from(proxy::getPort).whenNonNull().to(builder::port);
                        map.from(proxy::getUsername).whenHasText()
                            .to(builder::username);
                        map.from(proxy::getPassword).whenHasText()
                            .to(password -> builder.password(s -> password));
                        map.from(proxy::getNonProxyHostsPattern).whenHasText()
                            .to(builder::nonProxyHosts);
                    });
                }
                return tcpClient;
            });

        HttpClientProperties.Ssl ssl = properties.getSsl();
        if ((ssl.getKeyStore() != null && ssl.getKeyStore().length() > 0)
            || ssl.getTrustedX509CertificatesForTrustManager().length > 0
            || ssl.isUseInsecureTrustManager()) {
            httpClient = httpClient.secure(sslContextSpec -> {
                // configure ssl
                SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();

                X509Certificate[] trustedX509Certificates = ssl
                    .getTrustedX509CertificatesForTrustManager();
                if (trustedX509Certificates.length > 0) {
                    sslContextBuilder = sslContextBuilder
                        .trustManager(trustedX509Certificates);
                }
                else if (ssl.isUseInsecureTrustManager()) {
                    sslContextBuilder = sslContextBuilder
                        .trustManager(InsecureTrustManagerFactory.INSTANCE);
                }

                try {
                    sslContextBuilder = sslContextBuilder
                        .keyManager(ssl.getKeyManagerFactory());
                }
                catch (Exception e) {
                    log.error("create http client error",e);
                }

                sslContextSpec.sslContext(sslContextBuilder)
                    .defaultConfiguration(ssl.getDefaultConfigurationType())
                    .handshakeTimeout(ssl.getHandshakeTimeout())
                    .closeNotifyFlushTimeout(ssl.getCloseNotifyFlushTimeout())
                    .closeNotifyReadTimeout(ssl.getCloseNotifyReadTimeout());
            });
        }

        if (properties.isWiretap()) {
            httpClient = httpClient.wiretap(true);
        }

        return httpClient;
    }
}
