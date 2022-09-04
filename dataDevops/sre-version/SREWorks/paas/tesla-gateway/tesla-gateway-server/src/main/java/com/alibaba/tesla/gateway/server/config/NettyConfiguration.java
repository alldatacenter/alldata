package com.alibaba.tesla.gateway.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Component
public class NettyConfiguration implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {

    @Value("${server.max-initial-line-length:65536}")
    private int maxInitialLingLength;

    @Override
    public void customize(NettyReactiveWebServerFactory factory) {
        factory.addServerCustomizers(
            httpServer -> httpServer.httpRequestDecoder(
                httpRequestDecoderSpec -> httpRequestDecoderSpec.maxInitialLineLength(maxInitialLingLength)
            )
        );
    }
}
