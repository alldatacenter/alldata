package com.alibaba.tesla.gateway.server.util;

import com.alibaba.tesla.gateway.common.enums.ServerTypeEnum;
import com.alibaba.tesla.gateway.domain.dto.RouteInfoDTO;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class RouteIdGenerator {
    private static String ROUTE_ID_TEMP = "%s-%s-%s";
    private RouteIdGenerator() {
    }

    public static String gen(RouteInfoDTO routeInfoDTO){
        String app = StringUtils.isNotBlank(routeInfoDTO.getAppId()) ? routeInfoDTO.getAppId() : "unknown";
        String serverType = ServerTypeEnum.valueOf(routeInfoDTO.getServerType()).name();
        return String.format(ROUTE_ID_TEMP, app, serverType, UUID.randomUUID().toString());

    }
}
