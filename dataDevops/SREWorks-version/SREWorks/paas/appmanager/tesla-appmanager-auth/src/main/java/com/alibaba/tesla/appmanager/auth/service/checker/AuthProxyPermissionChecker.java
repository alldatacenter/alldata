package com.alibaba.tesla.appmanager.auth.service.checker;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.autoconfig.AuthProperties;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.NetworkUtil;
import com.alibaba.tesla.appmanager.domain.req.permission.CheckPermissionReq;
import com.alibaba.tesla.appmanager.domain.res.permission.CheckPermissionRes;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 权代服务 Permission Checker
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "appmanager.auth.auth-engine", havingValue = "authproxy")
public class AuthProxyPermissionChecker implements PermissionChecker {

    private static final String DEFAULT_TENANT = "alibaba";
    private static final String DEFAULT_APP_ID = "__all__";

    private final OkHttpClient client;
    private final AuthProperties authProperties;

    public AuthProxyPermissionChecker(AuthProperties authProperties) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        setBuilder(clientBuilder);
        this.client = clientBuilder.build();
        this.authProperties = authProperties;
    }

    /**
     * 检查权限列表
     *
     * @param request 权限检查请求
     * @return Response
     */
    @Override
    public CheckPermissionRes checkPermissions(CheckPermissionReq request) {
        return checkPermissions(
                request.getOperator(),
                request.getCheckPermissions(),
                request.getDefaultPermissions(),
                request.getAsRole());
    }

    /**
     * 检查权限列表
     *
     * @param empId              用户 ID
     * @param checkPermissions   权限点列表
     * @param defaultPermissions 默认持有权限列表
     * @param asRole             角色扮演
     * @return Response
     */
    private CheckPermissionRes checkPermissions(String empId, List<String> checkPermissions,
                                                List<String> defaultPermissions, String asRole) {
        // 如果不存在 authproxy endpoint，那么默认直接放行
        String endpoint = this.authProperties.getAuthproxyEndpoint();
        if (StringUtils.isEmpty(endpoint)) {
            log.info("cannot find authproxy endpoint, skip|empId={}|checkPermissions={}",
                    empId, JSONArray.toJSONString(checkPermissions));
            return CheckPermissionRes.builder()
                    .permissions(checkPermissions)
                    .build();
        }

        String urlPrefix = NetworkUtil.concatenateStr(endpoint, String.format("users/empid::%s/permissions", empId));
        JSONObject body = new JSONObject();
        body.put("checkPermissions", checkPermissions);
        body.put("defaultPermissions", defaultPermissions);
        if (StringUtils.isNotEmpty(asRole)) {
            body.put("asRole", asRole);
        }
        Request.Builder requestBuilder = new Request.Builder()
                .url(Objects.requireNonNull(HttpUrl.parse(urlPrefix)).newBuilder().build())
                .header("X-Biz-Tenant", AuthProxyPermissionChecker.DEFAULT_TENANT)
                .header("X-Biz-App", AuthProxyPermissionChecker.DEFAULT_APP_ID)
                .header("X-EmpId", empId)
                .post(RequestBody.create(body.toJSONString(), MediaType.parse("application/json")));
        JSONObject response;
        try {
            response = NetworkUtil.sendRequest(this.client, requestBuilder, "");
        } catch (IOException e) {
            throw new AppException(AppErrorCode.NETWORK_ERROR,
                    String.format("invalid response from authproxy service|exception=%s", e.getMessage()));
        }
        log.debug("send request to authproxy|body={}|response={}", body.toJSONString(), response.toJSONString());
        JSONObject data = response.getJSONObject("data");
        JSONArray permissions = data.getJSONArray("permissions");
        return CheckPermissionRes.builder()
                .permissions(permissions.toJavaList(String.class))
                .build();
    }

    /**
     * 设置一些 OkHttpBuilder 的公共属性
     *
     * @param builder Builder
     */
    private static void setBuilder(OkHttpClient.Builder builder) {
        builder.callTimeout(10, TimeUnit.SECONDS);
        builder.sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager) TRUST_ALL_CERTS[0]);
        builder.hostnameVerifier((hostname, session) -> true);
    }

    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }
    };

    private static final SSLContext TRUST_ALL_SSL_CONTEXT;

    static {
        try {
            TRUST_ALL_SSL_CONTEXT = SSLContext.getInstance("SSL");
            TRUST_ALL_SSL_CONTEXT.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private static final SSLSocketFactory trustAllSslSocketFactory = TRUST_ALL_SSL_CONTEXT.getSocketFactory();
}
