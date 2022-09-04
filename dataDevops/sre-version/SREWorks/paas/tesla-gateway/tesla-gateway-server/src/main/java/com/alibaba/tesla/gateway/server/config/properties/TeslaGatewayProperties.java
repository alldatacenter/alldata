package com.alibaba.tesla.gateway.server.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author tony.ly@alibaba-inc.com
 */
@ConfigurationProperties(prefix = "tesla.config.gateway")
@Data
public class TeslaGatewayProperties {

    /**
     * header校验严格模式
     */
    private boolean seriousMode;
    /**
     * 原auth_proxy的cookie校验
     */
    private String authCookieKey;
    private String authCookieName;
    /**
     * JWT secret
     */
    private String jwtSecret;

    /**
     * tesla app/key
     */
    private String teslaAuthApp;
    private String teslaAuthKey;

    /**
     * 调用权代服务的请求地址
     */
    private String authAddress;

    private String adminToken;

    /**
     * diamond存储配置的dataId
     */
    private String storeDiamondDataId;

    /**
     * diamond存储配置的group
     */
    private String storeDiamondGroup;

    /**
     * 白名单， diamond dataId
     */
    private String authWhitelistDiamondDataId;

    /**
     * 白名单 diamond group
     */
    private String authWhitelistDiamondGroup;

    /**
     * nacos存储配置的dataid
     */
    private String storeNacosDataId;

    /**
     * nacos存储配置的group
     */
    private String storeNacosGroup;

    private String defaultRouteDataId = "abm-paas-gateway.default-route";

    private String defaultRouteGroup = "ABM-PAAS-SYSTEM";

    /**
     * nacos存储配置的namespace
     */
    private String storeNacosNamespace;

    /**
     * nacos存储的地址
     */
    private String storeNacosAddr;

    /**
     * 启用跨域
     */
    private boolean enabledCros = true;

    /**
     * 允许跨域的域名，多个用逗号分隔
     */
    private String allowCrosDomain;

    /**
     * 鉴权重定向跳转
     */
    private String authRedirectUrl = "https://tesla.alibaba-inc.com/api-proxy/auth/login";

    /**
     * 是否启用doc文档
     */
    private boolean enableDoc = true;

    /**
     * 启用鉴权
     */
    private boolean enableAuth = true;

    /**
     * 重新加载配置
     */
    private boolean reloadConfig = true;

    /**
     * 启用switch view
     */
    private boolean enableSwitchView = true;

    /**
     * 是否启用阿里云用户登录
     */
    private boolean enabledAliyunUserLogin;

    /**
     * 阿里云 access key
     */
    private String aliyunAccessKey;

    /**
     * 阿里云access secret
     */
    private String aliyunAccessSecret;

}
