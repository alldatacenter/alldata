package com.alibaba.tesla.gateway.server.util;

import com.alibaba.tesla.gateway.common.enums.RouteTypeEnum;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 检查路由是否合法
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
public class GatewayRouteCheckUtil {

    /**
     * 检查合法性，如果不合法，则抛错
     * 如果host为空，并且非法URL都返回false，不让插入到路由
     * @param infoDO {@link RouteInfoDO}
     * @return
     */
    public static Boolean checkLegitimate(RouteInfoDO infoDO) {
        String url = infoDO.getUrl();
        if(StringUtils.isBlank(url)){
            return false;
        }
        Boolean result = Boolean.FALSE;
        try {
            URI uri = new URI(url);
            if(StringUtils.isBlank(uri.getHost())){
                return false;
            }
        } catch (URISyntaxException e) {
            log.error("url is illegal, error=", e);
            return false;
        }
        RouteTypeEnum routeType = RouteTypeEnum.toTypeEnum(infoDO.getRouteType());

        switch (routeType){
            case PATH:
                result = checkByPath(infoDO);
                break;
            case HOST:
                result = checkByHost(infoDO);
                break;
            default:
                log.error("not support this type, routeType={}", routeType);
        }
        return result;
    }

    private static Boolean checkByHost(RouteInfoDO infoDO) {
        if(StringUtils.isBlank(infoDO.getHost())){
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private static Boolean checkByPath(RouteInfoDO infoDO){
        if(StringUtils.isBlank(infoDO.getPath())){
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }
}
