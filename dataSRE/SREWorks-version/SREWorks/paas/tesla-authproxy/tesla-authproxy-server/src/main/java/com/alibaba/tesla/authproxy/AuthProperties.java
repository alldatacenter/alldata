package com.alibaba.tesla.authproxy;

import com.alibaba.tesla.common.base.enums.TeslaRegion;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * <p>Description:  用于获取权限代理服务的配置属性值 <／p>
 * <p>Copyright: alibaba (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017/5/13 下午4:19
 */
@Component
@Data
@ConfigurationProperties(prefix = "tesla")
public class AuthProperties {

    /**
     * 公共属性
     */
    private String defaultLanguage;
    private String availableLanguages;
    private String allowOrigin;
    private String exclusionUrl;
    private String authPolicy;
    private String loginPolicy;
    private String loginUrl;
    private String aasDirectLoginUrl;
    private String logoutUrl;
    private String callbackUrl;
    private String proxyServerAddress;
    private String networkProtocol;

    /**
     * aas+oam认证方式属性
     */
    private String popEndpointName;
    private String popRegionId;
    private String popOamDomain;
    private String popAasDomain;
    private String popAasKey;
    private String popAasSecret;
    private String popOamKey;
    private String popOamSecret;

    private String aasKey;
    private String aasSecret;
    private String aasGetAkUrl;
    private String aasLoadTicketUrl;
    private String aasInnerEndpoint;
    private String oamAccountResourceName;
    private String aasDefaultBid;
    private Integer aasPasswordExpireMonths;
    private Integer loginSmsExpireSeconds;
    private String oamAdminRole;
    private String oamRoles;
    private String cookieDomain;
    private String aasSuperUser;
    private String schedulerEngine;
    private String zookeeperHost;
    private String zookeeperNamespace;
    private String environment;
    private TeslaRegion teslaSwitch;

    /**
     * OAuth2
     */
    private String oauth2JwtSecret;
    /**
     * 超级token权限账号，delimiter为‘，’
     */
    private String tokenAdmin;

    /**
     * appId
     */
    private String defaultAppId;

    private String adminLoginName;

    /**
     * 权限前缀
     */
    private String permissionPrefix = "26842:bcc:";

    /**
     * sso+acl认证方式属性
     */
    @Deprecated
    private String ssoTokenUserUrl;
    @Deprecated
    private String ssoAppCode;
    private String ssoServerUrl;

    /**
     * 是否使用应用默认角色，true-使用默认角色 false-使用OAM角色。
     * 注：大小专环境没有OAM使用此模式
     */
    private boolean defaultRole = false;
    /**
     * 是否开启登录验证，使用对内场景SSO+ACL的认证模式
     */
    private boolean loginEnable = false;

    /**
     * 上下文路径，使用SSO时需要的参数
     */
    private String contentPath;

    /**
     * 权代理服务域名配置
     */
    private String authproxyDomain;

    /**
     * 加密密钥
     */
    private String encryptKey;

    /**
     * OAuth2 的用户身份鉴权 URL
     */
    private String oauth2UserAuthorizationUri;

    /**
     * OAuth2 的用户身份信息获取 URL
     */
    private String oauth2UserInfoUri;

    /**
     * OAuth2 的 Token 刷新 URL
     */
    private String oauth2AccessTokenUri;

    /**
     * OAuth2 Client ID
     */
    private String oauth2ClientId;

    /**
     * OAuth2 Client Secret
     */
    private String oauth2ClientSecret;

    /**
     * OAuth2 Redirect URI
     */
    private String oauth2RedirectUri;

    /**
     * 管理用户列表
     */
    private String adminUsers;

    /**
     * BUC相关参数
     */
    private String appName;
    private String appCode;
    private String clientKey;
    private String serverUrl;
    private String backUrlDefault;
    private String clientVersion;
    private String contextPath;
    private String userCookieName;
    private String teslaLoginUrl;
    private int heartbeatInterval = 10;
    private int cookieDuration = 24;
}
