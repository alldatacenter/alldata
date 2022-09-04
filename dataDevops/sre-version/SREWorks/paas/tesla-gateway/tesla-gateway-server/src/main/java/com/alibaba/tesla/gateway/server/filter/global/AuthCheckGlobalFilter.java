package com.alibaba.tesla.gateway.server.filter.global;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.enums.TeslaRegion;
import com.alibaba.tesla.gateway.common.enums.AuthErrorTypeEnum;
import com.alibaba.tesla.gateway.domain.LoginUserInfo;
import com.alibaba.tesla.gateway.server.config.properties.TeslaGatewayProperties;
import com.alibaba.tesla.gateway.server.constants.AuthJwtConstants;
import com.alibaba.tesla.gateway.server.constants.AuthProxyConstants;
import com.alibaba.tesla.gateway.server.constants.GatewayConst;
import com.alibaba.tesla.gateway.server.constants.WebExchangeConst;
import com.alibaba.tesla.gateway.server.exceptions.TeslaAuthException;
import com.alibaba.tesla.gateway.server.exceptions.TeslaJwtException;
import com.alibaba.tesla.gateway.server.exceptions.TeslaUserAuthException;

import com.alibaba.tesla.gateway.server.cache.GatewayCache;
import com.alibaba.tesla.gateway.server.monitor.TeslaGatewayMetric;
import com.alibaba.tesla.gateway.server.repository.domain.RouteInfoDO;

import com.alibaba.tesla.gateway.server.util.AuthWhitelistCheckUtil;
import com.alibaba.tesla.gateway.server.util.TeslaJwtUtil;
import com.alibaba.tesla.gateway.server.util.UserAgentUtil;
import com.alibaba.tesla.web.properties.TeslaEnvProperties;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientFilterFunction;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Service
public class AuthCheckGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 调用权代服务校验header
     */
    private static final String GET_USER_PATH = "/auth/tesla/authHeader";

    private static final String HTTP = "http://";

    @Autowired
    private GatewayCache gatewayCache;

    @Autowired
    private TeslaGatewayProperties gatewayProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TeslaGatewayMetric teslaGatewayMetric;

    @Autowired
    private AuthWhitelistCheckUtil authWhitelistCheckUtil;

    @Autowired
    private TeslaEnvProperties teslaEnvProperties;

    @Autowired
    private TeslaGatewayProperties teslaGatewayProperties;

    @Resource
    private MetricsWebClientFilterFunction metricsWebClientFilterFunction;

    private WebClient webClient;


    @PostConstruct
    private void init() {
        if (this.teslaGatewayProperties.isEnableAuth()) {
            String authAddr = gatewayProperties.getAuthAddress();
            if (StringUtils.isNotBlank(authAddr) && !authAddr.startsWith(HTTP)) {
                authAddr = HTTP + authAddr;
            }

            webClient = WebClient.builder()
                .filter(metricsWebClientFilterFunction)
                .clientConnector(new ReactorClientHttpConnector(
                    HttpClient.from(
                        TcpClient.create().
                            option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                            .doOnConnected(connection ->
                                connection.addHandlerLast(new ReadTimeoutHandler(1))
                                    .addHandlerLast(new WriteTimeoutHandler(1))
                            ))))
                .baseUrl(authAddr)
                .build();
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = (Route)exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        Assert.notNull(route, "system error, routeInfo not be null, requestId=" + exchange.getRequest().getId());

        RouteInfoDO routeInfoDO = gatewayCache.getRouteInfoByRouteId(route.getId());
        if (routeInfoDO == null || (!routeInfoDO.isAuthLogin() || !routeInfoDO.isAuthHeader())
            || BooleanUtils.toBooleanDefaultIfNull(
            (Boolean)exchange.getAttributes().get(WebExchangeConst.TESLA_IS_FORWARD_ENV), Boolean.FALSE)) {
            return chain.filter(exchange);
        }

        //兼容，OXS ecs gateway不开鉴权
        if (!this.teslaGatewayProperties.isEnableAuth()) {
            return chain.filter(exchange);
        }

        //过路由不鉴权Url
        //过用户的忽略鉴权策略
        if (authWhitelistCheckUtil.allowNoAuth(route, exchange.getRequest().getPath().toString())) {
            log.info("not auth uri, requestId={}||routeId={}||path={}", exchange.getRequest().getId(),
                route.getId(), exchange.getRequest().getPath().toString());
            return chain.filter(exchange);
        }

        try {
            return this.authLogin(exchange, routeInfoDO)
               .onErrorContinue((throwable, o) -> {
                   if (throwable instanceof TeslaAuthException){
                       TeslaAuthException e = (TeslaAuthException) throwable;
                       //过白名单，不在白名单在直接拦截
                       if (!UserAgentUtil.isBrowserReq(exchange.getRequest())) {
                           if (authWhitelistCheckUtil.inWhitelist(routeInfoDO.getRouteId(), exchange.getRequest().getPath().toString())) {
                               return;
                           }
                       }
                   }
                   throw new TeslaAuthException(throwable.getLocalizedMessage());
               })
                .then(chain.filter(exchange));
        }catch (TeslaAuthException e) {
            TeslaRegion region = teslaEnvProperties.getRegion();

            //专有云直接拒绝
            if (!Objects.equals(TeslaRegion.INTERNAL, region)){
                throw e;
            }

            //过白名单，不在白名单在直接拦截
            if (!UserAgentUtil.isBrowserReq(exchange.getRequest())) {
                if (authWhitelistCheckUtil.inWhitelist(routeInfoDO.getRouteId(), exchange.getRequest().getPath().toString())) {
                    return chain.filter(exchange);
                }
            }
            throw e;
        }
    }

    @Override
    public int getOrder() {
        return GlobalFilterOrderManager.AUTH_CHECK_FILTER;
    }

    /**
     * 1、如果token不为空，则校验token 2、如果token为空，先验证cookie,然后验证tesla认证头信息
     *
     * @param exchange  {@link ServerHttpRequest}
     * @param routeInfo route info
     * @return void
     */
    private Mono<Void> authLogin(ServerWebExchange exchange, RouteInfoDO routeInfo) {
        long startTime = System.currentTimeMillis();
        try {
            ServerHttpRequest request = exchange.getRequest();
            if (log.isDebugEnabled()) {
                log.debug(
                    "actionName=startAuth||id={}||routeId={}||requestUrl={}||requestHostName={}||cookies"
                        + "={}",
                    request.getId(), routeInfo.getRouteId(), request.getURI().toString(),
                    request.getRemoteAddress().getHostName(), request.getCookies());
            }

            String token = request.getHeaders().getFirst(AuthProxyConstants.HEADER_NAME_TOKEN);
            if (StringUtils.isBlank(token)) {
                return verifyCookieAndTeslaHeaders(exchange, routeInfo).then();
            }
            Claims claims;
            try {
                claims = TeslaJwtUtil.verify(token, gatewayProperties.getJwtSecret());
            } catch (TeslaJwtException e) {
                log.warn("gatewayAuthCheck:verifyHeaderTokenFailed, requestId={}||errorMsg={}",
                    request.getId(), e.getLocalizedMessage(),e);
                throw new TeslaAuthException("header jwt verify failed. " + (e.getCause() != null ? e.getCause().getLocalizedMessage() : ""));
            }
            log.info(
                "gatewayAuthCheck:verifyHeaderJwtSuccess||id={}||expiration={}||appId={}||userId={}||loginName"
                    + "={}||aliyunPk={}||emailAddr={}",
                request.getId(),
                claims.getExpiration(),
                claims.get(AuthJwtConstants.JWT_APP_ID_CLAIM_KEY),
                claims.get(AuthJwtConstants.JWT_EMP_ID_CLAIM_KEY),
                claims.get(AuthJwtConstants.JWT_LOGIN_NAME_CLAIM_KEY),
                claims.get(AuthJwtConstants.JWT_ALIYUN_PK_CLAIM_KEY),
                claims.get(AuthJwtConstants.JWT_EMAIL_CLAIM_KEY));

            String empId;
            if (teslaEnvProperties.getRegion().equals(TeslaRegion.PRIVATE)) {
                empId = String.valueOf(claims.get(AuthJwtConstants.JWT_ALIYUN_PK_CLAIM_KEY));
            } else {
                empId = String.valueOf(claims.get(AuthJwtConstants.JWT_EMP_ID_CLAIM_KEY));
            }
            mockHeader(request, String.valueOf(claims.get(AuthJwtConstants.JWT_APP_ID_CLAIM_KEY)),
                String.valueOf(claims.get(AuthJwtConstants.JWT_APP_SECRET_CLAIM_KEY)),
                String.valueOf(claims.get(AuthJwtConstants.JWT_LOGIN_NAME_CLAIM_KEY)),
                null, empId, token,
                String.valueOf(claims.get(AuthJwtConstants.JWT_EMAIL_CLAIM_KEY)));
            this.addCheckResult(exchange, true);
            return Mono.empty();
        } finally {
            long authTime = System.currentTimeMillis() - startTime;
            exchange.getAttributes().put(WebExchangeConst.TESLA_AUTH_CHECK_TIME, authTime);
        }

    }

    /**
     * 验证cookie和tesla认证头信息
     *
     * @param exchange  {@link ServerHttpRequest}
     * @param routeInfo routeInfo
     * @return void
     */
    private Mono<Void> verifyCookieAndTeslaHeaders(ServerWebExchange exchange, RouteInfoDO routeInfo) {
        ServerHttpRequest request = exchange.getRequest();
        MultiValueMap<String, HttpCookie> cookies = request.getCookies();
        if (log.isDebugEnabled()) {
            log.debug("actionName=startVerifyCookie||requestId={}", request.getId());
        }

        if (cookies.containsKey(AuthProxyConstants.COOKIE_SSO_LOGIN_TOKEN)) {
            String teslaToken = cookies.getFirst(AuthProxyConstants.COOKIE_SSO_LOGIN_TOKEN).getValue();
            Claims claims = null;
            try {
                claims = TeslaJwtUtil.verify(teslaToken, gatewayProperties.getJwtSecret());
            } catch (Exception e) {
                log.warn("gatewayAuthCheck:verifyCookieTokenFailed, requestId={}||errorMsg={}", request.getId(),
                    e.getLocalizedMessage(),e);
            }
            if (claims != null) {
                String empId;
                if (teslaEnvProperties.getRegion().equals(TeslaRegion.PRIVATE)) {
                    empId = claims.get(AuthJwtConstants.JWT_ALIYUN_PK_CLAIM_KEY, String.class);
                    if (cookies.containsKey(AuthProxyConstants.COOKIE_LANG) &&
                        cookies.containsKey(AuthProxyConstants.COOKIE_COUNTRY)) {
                        String cookieLang = cookies.getFirst(AuthProxyConstants.COOKIE_LANG).getValue();
                        String cookieCountry = cookies.getFirst(AuthProxyConstants.COOKIE_COUNTRY).getValue();
                        request.mutate().header(AuthProxyConstants.HEADER_NAME_LOCAL,
                            new String[] {cookieLang + "_" + cookieCountry});
                    }
                } else {
                    empId = claims.get(AuthJwtConstants.JWT_EMP_ID_CLAIM_KEY, String.class);
                }
                String loginName = claims.get(AuthJwtConstants.JWT_LOGIN_NAME_CLAIM_KEY, String.class);
                String bucId = claims.get(AuthJwtConstants.JWT_BUC_ID_CLAIM_KEY, String.class);
                String email = claims.get(AuthJwtConstants.JWT_EMAIL_CLAIM_KEY, String.class);
                mockHeader(request, gatewayProperties.getTeslaAuthApp(), gatewayProperties.getTeslaAuthKey(), loginName,
                    null, empId, teslaToken, email);
                request.mutate().header(AuthProxyConstants.V1_HEADER_BUC_ID,
                    new String[] {bucId});
                log.info(
                    "gatewayAuthCheck:verifyBccCookieSuccess||requestId={}||empId={}||loginName={}||bucId={}||email={}",
                    request.getId(), empId, loginName, bucId, email);
                this.addCheckResult(exchange, true);
                return Mono.empty();
            }
            log.warn("gatewayAuthCheck:verifyBccCookieFailed||requestId={}||cookie={}", request.getId(),
                request.getCookies());
        }
        //临时兼容原auth-proxy鉴权逻辑，待auth-proxy服务启用后，删除此段逻辑
        if (cookies.containsKey(gatewayProperties.getAuthCookieName())) {
            String userInfoCookieValue = cookies.getFirst(gatewayProperties.getAuthCookieName()).getValue();
            String userInfo = parseUserInfoCookie(userInfoCookieValue);
            if (userInfo != null) {
                try {
                    AuthUser authUser = objectMapper.readValue(userInfo, AuthUser.class);
                    String userName = authUser.getUserName();
                    String empId = authUser.getEmpId();
                    String bucId = authUser.getBucUserId();
                    String email = authUser.getEmailAddr();
                    if (StringUtils.isBlank(userName) && !StringUtils.isBlank(authUser.getEmpId())) {
                        userName = authUser.getEmpId();
                    }
                    mockHeader(request, gatewayProperties.getTeslaAuthApp(), gatewayProperties.getTeslaAuthKey(),
                        userName, null, empId, null, email);
                    request.mutate().header(AuthProxyConstants.V1_HEADER_BUC_ID, new String[] {bucId});
                    log.info(
                        "gatewayAuthCheck:verifyAuthProxyCookieSuccess||requestId={}||empId={}||loginName={}||bucId"
                            + "={}||email={}",
                        request.getId(), empId, userName, bucId, email);
                    this.addCheckResult(exchange, true);
                    return Mono.empty();
                } catch (IOException e) {
                    log.trace("gatewayAuthCheck:parseAuthProxyCookieFailed.requestId=" + request.getId(), e);
                }
            }
            log.warn("gatewayAuthCheck:verifyAuthProxyCookieFailed||requestId={}||cookie={}", request.getId(),
                request.getCookies());
        }

        boolean isBrowserReq = UserAgentUtil.isBrowserReq(request);
        if (routeInfo.isAuthHeader() && !isBrowserReq) {
            return verifyTeslaHeaders(exchange, routeInfo);
        }
        log.warn(
            "gatewayAuthCheck:gatewayAuthCheckFailed||id={}||routeId={}||requestUrl={}||requestHostName={}||cookies={}",
            request.getId(), routeInfo.getRouteId(), request.getURI().toString(),
            request.getRemoteAddress().getHostName(), request.getCookies());

        throw new TeslaAuthException("Gateway:Unauthorized");
    }

    /**
     * 验证tesla认证头信息
     *
     * @param exchange {@link ServerHttpRequest}
     * @return void
     */
    private Mono<Void> verifyTeslaHeaders(ServerWebExchange exchange, RouteInfoDO routeInfo) {
        ServerHttpRequest request = exchange.getRequest();
        String xAuthApp = request.getHeaders().getFirst(AuthProxyConstants.HEADER_NAME_APP);
        String xAuthKey = request.getHeaders().getFirst(AuthProxyConstants.HEADER_NAME_KEY);
        String xAuthUser = request.getHeaders().getFirst(AuthProxyConstants.HEADER_NAME_USER);
        String xAuthPwd = request.getHeaders().getFirst(AuthProxyConstants.HEADER_NAME_PASSWD);
        String empId = request.getHeaders().getFirst(AuthProxyConstants.HEADER_NAME_EMP_ID);
        String requestPath = request.getPath().toString();
        String routeId = routeInfo.getRouteId();

        if (log.isDebugEnabled()) {
            log.debug("actionName=startVerifyHeader||requestId={}||headers={}", request.getId(),
                request.getHeaders().toString());
        }
        if (StringUtils.isEmpty(xAuthApp) || StringUtils.isEmpty(xAuthKey) || StringUtils.isEmpty(xAuthUser)
            || StringUtils.isEmpty(xAuthPwd)) {
            log.warn(
                "gatewayAuthCheck:verifyTeslaHeadersError!request header can't be "
                    + "empty||requestId={}||requestPath={}||routeId={}",
                request.getId(), requestPath, routeId);
            this.addCheckResult(exchange, false);

            TeslaAuthException authException = new TeslaAuthException(
                "Gateway:Unauthorized");
            authException.setErrorType("verifyTeslaHeadersError");
            throw authException;
        }

        //息从缓存中获取登录用户信息
        String cacheKey = this.buildUserInfoKey(xAuthUser, xAuthPwd, xAuthApp, xAuthKey);
        LoginUserInfo loginUserInfo = gatewayCache.getLoginUser(cacheKey);
        if (null != loginUserInfo) {
            //将tesla认证头信息写入请求头
            mockHeader(request, xAuthApp, xAuthKey, xAuthUser, xAuthPwd, loginUserInfo.getEmpId(), null, loginUserInfo.getEmail());
            this.addCheckResult(exchange, loginUserInfo.isCheckSuccess());
            log.info(
                "gatewayAuthCheck:verifyHeaderCacheSuccess||requestId={}||checkSuccess={}||empId={}",
                request.getId(), loginUserInfo.isCheckSuccess(), loginUserInfo.getEmpId());
            return Mono.empty();
        }

        //缓存中不存在登录用户信息则调用权代服务获取登录用户信息
        return webClient.get()
            .uri(GET_USER_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .header(AuthProxyConstants.HEADER_NAME_APP, xAuthApp)
            .header(AuthProxyConstants.HEADER_NAME_KEY, xAuthKey)
            .header(AuthProxyConstants.HEADER_NAME_USER, xAuthUser)
            .header(AuthProxyConstants.HEADER_NAME_PASSWD, xAuthPwd)
            .retrieve()
            .onStatus(httpStatus -> HttpStatus.OK.value() != httpStatus.value(), res -> {
                throw new TeslaUserAuthException(String
                    .format("gatewayAuthCheck:request tesla auth proxy failed, result=%s, requestId=%s", res.toString(),
                        request.getId()));
            })
            .bodyToMono(JSONObject.class)
            .map(res -> {
                int code = res.getInteger("code");
                log.info("actionName=requestAuthProxy||id={}||x-auth-app={}||x-auth-key={}||x-auth-user={}||result={}",
                    request.getId(), xAuthApp, xAuthKey, xAuthUser, res.toJSONString());
                if (code != HttpStatus.OK.value()) {
                    throw new TeslaUserAuthException(String
                        .format(
                            "gatewayAuthCheck:Check header failed from tesla auth proxy, result=%s, requestId=%s,"
                                + "headers=%s",
                            res.toString(), request.getId(), request.getHeaders()));
                }
                LoginUserInfo userInfo = JSONObject.parseObject(res.getJSONObject("data").toJSONString(),
                    LoginUserInfo.class);
                return userInfo;
            })
            .doOnNext(userInfo -> {
                if (!userInfo.isCheckSuccess()) {
                    //将tesla认证头信息写请求头
                    mockHeader(request, xAuthApp, xAuthKey, xAuthUser, xAuthPwd, empId, null, null);
                    this.addCheckResult(exchange, false);
                    log.warn(
                        "gatewayAuthCheck:verifyTeslaHeadersError!Check header "
                            + "error!||requestId={}||routeId={}||requestPath={}",
                        request.getId(), requestPath, routeId);
                    teslaGatewayMetric.headerCheckError(routeId, "",
                        AuthErrorTypeEnum.HEADER_ERROR, "");

                    TeslaAuthException authException = new TeslaAuthException(
                        "Gateway:Unauthorized, please check you auth headers.");
                    authException.setErrorType("verifyTeslaHeadersError");
                    throw authException;
                } else {
                    //放入缓存
                    gatewayCache.storeLoginUser(cacheKey, userInfo);
                    //将tesla认证头信息写请求头
                    this.addCheckResult(exchange, true);
                    mockHeader(request, xAuthApp, xAuthKey, xAuthUser, xAuthPwd, userInfo.getEmpId(), null, userInfo.getEmail());
                    log.info("gatewayAuthCheck:verifyTeslaHeadersSuccess||requestId={}||xAuthApp={}||xAuthKey"
                            + "={}||xAuthUser={}||empId={}", request.getId(),
                        xAuthApp, xAuthKey, xAuthUser, userInfo.getEmpId());
                }
            })
            .then();
    }

    /**
     * 标记校验成功与失败
     *
     * @param exchange {@link ServerWebExchange}
     * @param result   boolean
     */
    private void addCheckResult(ServerWebExchange exchange, boolean result) {
        if (Objects.equals(GatewayConst.ACCESS_ONCE,
            exchange.getRequest().getHeaders().getFirst(GatewayConst.TESLA_GATEWAY_ACCESS)) &&
            !exchange.getResponse().getHeaders().containsKey(GatewayConst.AUTH_CHECK_RESULT)) {
            exchange.getResponse().getHeaders().set(GatewayConst.AUTH_CHECK_RESULT,
                result ? GatewayConst.AUTH_CHECK_SUCCESS : GatewayConst.AUTH_CHECK_FAILED);
        }
    }

    /**
     * build redis key
     *
     * @param xAuthUser auth user
     * @param xAuthPwd  auth pwd
     * @return key
     */
    private String buildUserInfoKey(String xAuthUser, String xAuthPwd, String xAuthApp, String xAuthKey) {
        String md5Hex = DigestUtils
            .md5Hex(xAuthPwd + xAuthApp + xAuthKey).toUpperCase();
        return String.format("gateway_auth_check_%s_%s", xAuthUser, md5Hex);
    }


    private void mockHeader(ServerHttpRequest request, String xAuthApp, String xAuthKey, String xAuthUser,
        String xAuthPwd, String empId, String oauth2Token, String emailAddr) {
        if (log.isDebugEnabled()) {
            log.debug("mutateHeader:requestId={},xAuthApp={},xAuthKey={},xAuthUser={},empId={}", request.getId(),
                xAuthApp,
                xAuthKey, xAuthUser, empId);
        }
        if (!StringUtils.isBlank(xAuthApp)) {
            request.mutate().header(AuthProxyConstants.HEADER_NAME_APP, new String[] {xAuthApp});
        }
        if (!StringUtils.isBlank(xAuthKey)) {
            request.mutate().header(AuthProxyConstants.HEADER_NAME_KEY, new String[] {xAuthKey});
        }
        if (!StringUtils.isBlank(xAuthUser)) {
            request.mutate().header(AuthProxyConstants.HEADER_NAME_USER, new String[] {xAuthUser});
        }
        if (!StringUtils.isBlank(xAuthPwd)) {
            request.mutate().header(AuthProxyConstants.HEADER_NAME_PASSWD, new String[] {xAuthPwd});
        }
        if (!StringUtils.isBlank(empId)) {
            request.mutate().header(AuthProxyConstants.HEADER_NAME_USER_ID, new String[] {empId});
            request.mutate().header(AuthProxyConstants.HEADER_NAME_EMP_ID, new String[] {empId});
        }
        if (!StringUtils.isBlank(oauth2Token)) {
            request.mutate().header(AuthProxyConstants.HTTP_BASIC_AUTH_HEADER, new String[] {oauth2Token});
        }
        if (StringUtils.isNotBlank(emailAddr)) {
            request.mutate().header(AuthProxyConstants.V1_HEADER_EMAIL, new String[] {emailAddr});
        }
    }

    /**
     * 复写go的GetSecureCookie（）方法
     *
     * @param cookieValue
     * @return
     */
    private String parseUserInfoCookie(String cookieValue) {
        String secret = gatewayProperties.getAuthCookieKey();
        String[] vals = cookieValue.split("\\|", 3);
        if (vals.length != 3) {
            return null;
        }
        String vs = vals[0];
        String timestamp = vals[1];
        String sig = vals[2];
        byte[] hmac = new HmacUtils("HmacSHA1", secret.getBytes()).hmac(vs + timestamp);
        if (!Objects.equals(sig, Hex.encodeHexString(hmac))) {
            return null;
        }
        return new String(Base64.getUrlDecoder().decode(vs));
    }

    @Data
    private static class AuthUser {
        @JsonProperty("UserName")
        private String userName;
        @JsonProperty("UserNameCN")
        private String userNameCN;
        @JsonProperty("NickName")
        private String nickName;
        @JsonProperty("EmailAddr")
        private String emailAddr;
        @JsonProperty("EmpId")
        private String empId;
        @JsonProperty("BucUserId")
        private String bucUserId;
        @JsonProperty("LoginTime")
        private String loginTime;
    }

}
