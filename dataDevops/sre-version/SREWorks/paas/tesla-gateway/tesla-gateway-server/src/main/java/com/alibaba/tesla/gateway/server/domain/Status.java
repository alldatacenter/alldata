package com.alibaba.tesla.gateway.server.domain;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import lombok.Data;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author tandong.td@alibaba-inc.com
 */
@Repository
@Data
public class Status {
    private String name;
    private String uptime;
    private List<RouteInfoDO> routeConfig;
    private FilterConfig filterConfig;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastRouteFlush;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastFilterFlush;
}
