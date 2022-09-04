package com.alibaba.tesla.gateway.server.util;

import com.alibaba.tesla.gateway.server.constants.GatewayConst;
import com.alibaba.tesla.web.properties.TeslaEnvProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Objects;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */

@Component
public class TeslaServerRequestUtil {

    @Autowired
    private TeslaEnvProperties envProperties;

    public String getXEnv(ServerRequest request){
        if(request == null){
            return envProperties.getEnv().name();
        }
        String env = request.headers().asHttpHeaders().getFirst(GatewayConst.X_ENV_NAME);
        return StringUtils.isNotBlank(env) ? env : envProperties.getEnv().name();

    }


    public String getXEnv(ServerHttpRequest request){
        if(request == null){
            return envProperties.getEnv().name();
        }
        String env = request.getHeaders().getFirst(GatewayConst.X_ENV_NAME);
        return StringUtils.isNotBlank(env) ? env : envProperties.getEnv().name();
    }


    /**
     * 获取机器的真实IP
     * 有问题
     * todo
     * @param request {@link ServerHttpRequest}
     * @return real host ip
     */
    public String getRealHostIp(ServerHttpRequest request){
        String ip = request.getHeaders().getFirst("x-forwarded-for");
        if (ip == null || ip.length() == 0 || StringUtils.equalsIgnoreCase(GatewayConst.UNKNOWN, ip)) {
            ip = request.getHeaders().getFirst("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || StringUtils.equalsIgnoreCase(GatewayConst.UNKNOWN, ip)) {
            ip = request.getHeaders().getFirst("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || StringUtils.equalsIgnoreCase(GatewayConst.UNKNOWN, ip)) {
            ip = request.getHeaders().getFirst("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || StringUtils.equalsIgnoreCase(GatewayConst.UNKNOWN, ip)) {
            ip = request.getHeaders().getFirst("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || StringUtils.equalsIgnoreCase(GatewayConst.UNKNOWN, ip)) {
            if(request.getRemoteAddress() != null){
                ip = request.getRemoteAddress().getAddress().getHostAddress();
            }else {
                ip = GatewayConst.UNKNOWN;
            }
        }
        return ip;
    }
}
