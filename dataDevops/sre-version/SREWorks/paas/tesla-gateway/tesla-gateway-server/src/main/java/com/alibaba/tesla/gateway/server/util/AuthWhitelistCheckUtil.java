package com.alibaba.tesla.gateway.server.util;

import com.alibaba.tesla.gateway.server.cache.GatewayCache;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.NumberUtils;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 校验是否在白名单
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Component
public class AuthWhitelistCheckUtil {

    private static AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    private GatewayCache gatewayCache;

    public boolean inWhitelist(String routeId, String uri){
        List<String> whiteList = this.gatewayCache.getAuthWhiteListByRouteId(routeId);
        if(CollectionUtils.isEmpty(whiteList) || StringUtils.isEmpty(uri)){
            return false;
        }
        for (String whiteUri : whiteList) {
            Boolean res = StringUtils.isNotBlank(whiteUri) && pathMatcher.match(whiteUri, uri);
            if(res){
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否匹配上，如果匹配上则无须鉴权
     * @param routeInfoDO route info
     * @param route gateway route
     * @param path path
     * @return ignore result
     */
    public boolean allowNoAuth(Route route,  String path){
        if (route == null){
            return false;
        }

        if (CollectionUtils.isEmpty(route.getMetadata())) {
            return false;
        }

        Object authIgnorePaths = route.getMetadata().get(RouteMetaDataKeyConstants.AUTH_IGNORE_PATHS);
        if (authIgnorePaths instanceof List) {
            List<String> authIgnorePathList = (List<String>)authIgnorePaths;
            Object stripPrefix = route.getMetadata().get(RouteMetaDataKeyConstants.STRIP_PREFIX);
            if (stripPrefix == null ){
                return false;
            }

            if (!StringUtils.isNumeric((String) stripPrefix)) {
                return false;
            }

            int stripPrefixLenth = Integer.parseInt((String)stripPrefix);

            String realPath = "/"
                + Arrays.stream(org.springframework.util.StringUtils.tokenizeToStringArray(path, "/"))
                .skip(stripPrefixLenth).collect(Collectors.joining("/"));
            final String finalPath = realPath + (realPath.length() > 1 && path.endsWith("/") ? "/" : "");
            return authIgnorePathList.stream().filter(ignorPath -> pathMatcher.match(ignorPath, finalPath)).findFirst().isPresent();
        }
        return false;
    }
}
