package com.alibaba.tesla.gateway.server.filter.factory;

import com.alibaba.tesla.gateway.api.GatewayManagerConfigService;
import com.alibaba.tesla.gateway.server.exceptions.TeslaForbiddenException;
import com.alibaba.tesla.gateway.server.util.TeslaHostUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.LinkedHashSet;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.util.StringUtils.isEmpty;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Component
public class SwaggerHeaderFilter extends AbstractGatewayFilterFactory implements Ordered {

    private static final String X_FORWARDED_PREFIX_HEADER = "X-Forwarded-Prefix";

    private static final String REFERER_HEADER = "Referer";

    private static final String DOC_URI = "/v2/api-docs";

    @Autowired
    private GatewayManagerConfigService gatewayManagerConfigService;

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            LinkedHashSet<URI> originalUris = exchange
                .getAttribute(GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
            URI requestUri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);

            if (originalUris != null && requestUri != null) {

                originalUris.stream().forEach(originalUri -> {
                    if (originalUri != null && originalUri.getPath() != null) {
                        String originalUriPath = stripTrailingSlash(originalUri);
                        String requestUriPath = stripTrailingSlash(requestUri);

                        updateRequest(request, originalUri, originalUriPath,
                            requestUriPath);

                    }
                });
            }
            String path = request.getURI().getPath();

            if (!StringUtils.endsWithIgnoreCase(path, DOC_URI)) {
                return chain.filter(exchange);
            }

            if (!request.getHeaders().containsKey("x-empId") ||
                !this.gatewayManagerConfigService.isDocAdminUser(request.getHeaders().getFirst("x-empId"))) {
                throw new TeslaForbiddenException("not allow access api dock. please contact admin");
            }

            String refer = request.getHeaders().getFirst(REFERER_HEADER);
            if (!TeslaHostUtil.inTeslaHost(refer)) {
                return chain.filter(exchange);
            }

            String prefix = request.getHeaders().getFirst(X_FORWARDED_PREFIX_HEADER);
            prefix = "/gateway" + (prefix == null ? "" : prefix);
            request.mutate().header(X_FORWARDED_PREFIX_HEADER, new String[] {prefix});
            return chain.filter(exchange);
        };
    }

    private String stripTrailingSlash(URI uri) {
        if (uri.getPath().endsWith("/")) {
            return uri.getPath().substring(0, uri.getPath().length() - 1);
        }
        else {
            return uri.getPath();
        }
    }

    private void updateRequest(ServerHttpRequest httpRequest, URI originalUri,
                               String originalUriPath, String requestUriPath) {
        String prefix;
        if (requestUriPath != null && (originalUriPath.endsWith(requestUriPath))) {
            prefix = substringBeforeLast(originalUriPath, requestUriPath);
            if (prefix != null && prefix.length() > 0
                && prefix.length() <= originalUri.getPath().length()) {
                httpRequest.mutate().header(X_FORWARDED_PREFIX_HEADER, new String[] {prefix});
            }
        }
    }

    private static String substringBeforeLast(String str, String separator) {
        if (isEmpty(str) || isEmpty(separator)) {
            return str;
        }
        int pos = str.lastIndexOf(separator);
        if (pos == -1) {
            return str;
        }
        return str.substring(0, pos);
    }


    @Override
    public int getOrder() {
        return 20;
    }

}
