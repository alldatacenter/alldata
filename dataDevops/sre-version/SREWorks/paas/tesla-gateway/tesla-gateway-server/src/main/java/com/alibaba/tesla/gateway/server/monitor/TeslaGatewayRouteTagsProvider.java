package com.alibaba.tesla.gateway.server.monitor;

import com.alibaba.tesla.gateway.server.util.TeslaServerRequestUtil;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.support.tagsprovider.GatewayTagsProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;


/**
 * tesla gateway metric
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Component("teslaGatewayRouteTagsProvider")
public class TeslaGatewayRouteTagsProvider implements GatewayTagsProvider {

    @Autowired
    private TeslaServerRequestUtil teslaServerRequestUtil;


    @Override
    public Tags apply(ServerWebExchange exchange) {
        return Tags.of("x-env", teslaServerRequestUtil.getXEnv(exchange.getRequest()));
    }
}
