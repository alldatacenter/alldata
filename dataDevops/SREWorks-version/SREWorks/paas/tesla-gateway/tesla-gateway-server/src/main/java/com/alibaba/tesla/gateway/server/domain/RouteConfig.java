package com.alibaba.tesla.gateway.server.domain;

import com.alibaba.tesla.gateway.domain.dto.RouteInfoDTO;
import com.alibaba.tesla.gateway.domain.req.RouteInfo;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import lombok.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author tandong.td@alibaba-inc.com
 */
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Repository
@Data
public class RouteConfig {
    public static final String DEFAULT_VERSION = "V1.0";
    public static final String CONFIG_NAME = "route_config";

    private String version = "1.0";

    private String name = "route_config";

    private String env;

    private List<RouteInfoDTO> routes;
}
