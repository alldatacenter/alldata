package com.alibaba.tesla.gateway.server.event;

import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author tandong.td@alibaba-inc.com
 */
@Component
public class RouteRefreshEvent extends ApplicationEvent {
    private List<RouteInfoDO> routeInfos;

    public RouteRefreshEvent(List<RouteInfoDO> routeInfos) {
        super(routeInfos);
        this.routeInfos = routeInfos;
    }

    public List<RouteInfoDO> getRouteConfig() {
        return routeInfos;
    }
}
