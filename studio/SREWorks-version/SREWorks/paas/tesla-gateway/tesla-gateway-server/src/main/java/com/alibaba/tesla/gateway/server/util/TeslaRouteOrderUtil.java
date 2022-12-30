package com.alibaba.tesla.gateway.server.util;

import com.alibaba.tesla.gateway.common.enums.ServerTypeEnum;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import org.springframework.util.Assert;

/**
 * ref: https://yuque.antfin-inc.com/bdsre/tesla-gateway/standard
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class TeslaRouteOrderUtil {
    private TeslaRouteOrderUtil() {
    }

    public static void setDefaultOrder(RouteInfoDO routeInfoDO){
        Assert.notNull(routeInfoDO, "routeInfoDO is null");
        ServerTypeEnum typeEnum = ServerTypeEnum.valueOf(routeInfoDO.getServerType());
        switch (typeEnum){
            case APP:
                routeInfoDO.setOrder(5000);
                break;
            case FAAS:
                routeInfoDO.setOrder(2500);
                break;
            case PAAS:
                routeInfoDO.setOrder(500);
                break;
            case TEST:
                routeInfoDO.setOrder(10000);
                break;
            default:
                throw new RuntimeException("not support this type, type=" + typeEnum);
        }
    }




}
