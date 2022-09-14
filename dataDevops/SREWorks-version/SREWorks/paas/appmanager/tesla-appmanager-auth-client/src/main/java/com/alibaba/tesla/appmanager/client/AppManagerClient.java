package com.alibaba.tesla.appmanager.client;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.client.lib.OAuth2Client;
import com.alibaba.tesla.appmanager.client.lib.OAuth2Exception;
import com.alibaba.tesla.appmanager.client.lib.OAuth2Response;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

/**
 * AppManager Client
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class AppManagerClient {

    private static final Logger log = LoggerFactory.getLogger(AppManagerClient.class);

    /**
     * HTTP Client
     */
    private volatile OkHttpClient internalHttpClient;

    /**
     * 当前 Access Token
     */
    private volatile String internalAuthToken;

    /**
     * 过期时间 (s)
     */
    private volatile Long internalExpiresAt;

    /**
     * Timeout Constants
     */
    private static final int CONNECT_TIMEOUT = 10;
    private static final int READ_TIMEOUT = 60;
    private static final int WRITE_TIMEOUT = 60;
    private static final int CALL_TIMEOUT = 60;

    /**
     * 容忍错误时间
     */
    private static final int TOLERATION_SECONDS = 300;

    private String endpoint;
    private String username;
    private String password;
    private String clientId;
    private String clientSecret;

    public AppManagerClient(String endpoint, String username, String password, String clientId, String clientSecret) {
        this.endpoint = endpoint;
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * 获取一个普通的 Http Client
     *
     * @return OkHttpClient
     */
    private OkHttpClient getHttpClient() {
        if (internalHttpClient == null) {
            synchronized (AppManagerClient.class) {
                if (internalHttpClient == null) {
                    OkHttpClient.Builder builder = new OkHttpClient.Builder();
                    setBuilder(builder);
                    internalHttpClient = builder.build();
                }
            }
        }
        return internalHttpClient;
    }

    /**
     * 检测当前 access token 是否有效
     *
     * @return true or false
     */
    private boolean validAccessToken() {
        return internalExpiresAt != null && System.currentTimeMillis() <= internalExpiresAt - TOLERATION_SECONDS * 1000;
    }

    /**
     * 刷新 oauth2 token (password grant_type)
     *
     * @param force 是否强制刷新
     */
    public void refreshAccessToken(boolean force) {
        if (!force && validAccessToken()) {
            return;
        }

        OkHttpClient httpClient = getHttpClient();
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)
            || StringUtils.isEmpty(clientId) || StringUtils.isEmpty(clientSecret)) {
            return;
        }

        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        String authUrl = String.format("%s/oauth/token", endpoint);
        OAuth2Client oauth2Client = new OAuth2Client
            .Builder(username, password, clientId, clientSecret, authUrl)
            .okHttpClient(httpClient)
            .scope("all")
            .build();
        OAuth2Response authResponse;
        try {
            authResponse = oauth2Client.requestAccessToken();
        } catch (IOException e) {
            log.error("cannot get auth token|exception={}", ExceptionUtils.getStackTrace(e));
            return;
        }
        if (!authResponse.isSuccessful()) {
            log.error("cannot get auth token|response={}", authResponse.getBody());
            return;
        }
        internalAuthToken = authResponse.getAccessToken();
        internalExpiresAt = authResponse.getExpiresAt();
        log.info("action=appmanagerRefreshAccessToken|token={}|expiresAt={}", internalAuthToken, internalExpiresAt);
    }

    /**
     * 通过 okhttp 发送 HTTP 请求，工具代码
     *
     * @param requestBuilder 请求内容 Builder
     * @return 返回 body 的 JSONObject
     */
    public Response sendRequestSimple(Request.Builder requestBuilder) throws IOException {
        OkHttpClient httpClient = getHttpClient();
        refreshAccessToken(false);
        Request request;
        if (!StringUtils.isEmpty(internalAuthToken)) {
            request = requestBuilder.header("Authorization", "Bearer " + internalAuthToken).build();
        } else {
            request = requestBuilder.header("X-EmpId", "SYSTEM").build();
        }
        return httpClient.newCall(request).execute();
    }

    /**
     * 通过 okhttp 发送 HTTP 请求，工具代码
     *
     * @param requestBuilder 请求内容 Builder
     * @return 返回 body 的 JSONObject
     */
    public JSONObject sendRequest(Request.Builder requestBuilder) throws IOException {
        OkHttpClient httpClient = getHttpClient();
        refreshAccessToken(false);
        Request request;
        if (!StringUtils.isEmpty(internalAuthToken)) {
            request = requestBuilder.header("Authorization", "Bearer " + internalAuthToken).build();
        } else {
            request = requestBuilder.header("X-EmpId", "SYSTEM").build();
        }
        Response response = httpClient.newCall(request).execute();
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            throw new OAuth2Exception("cannot sync to external environment, null response");
        }
        String bodyStr = responseBody.string();
        if (response.code() != 200) {
            throw new OAuth2Exception(String.format("send request failed, http status not 200|response=%s", bodyStr));
        }
        JSONObject body;
        try {
            body = JSONObject.parseObject(bodyStr);
        } catch (Exception e) {
            throw new OAuth2Exception(String.format("send request failed, response not json|response=%s", bodyStr));
        }
        int code = body.getIntValue("code");
        if (code != 200) {
            throw new OAuth2Exception(String.format("send request failed, response code not 200|response=%s", bodyStr));
        }
        return body;
    }

    /**
     * 设置一些 OkHttpBuilder 的公共属性
     *
     * @param builder Builder
     */
    private void setBuilder(OkHttpClient.Builder builder) {
        builder.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS);
        builder.readTimeout(READ_TIMEOUT, TimeUnit.SECONDS);
        builder.writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS);
        builder.callTimeout(CALL_TIMEOUT, TimeUnit.SECONDS);
        builder.sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager)TRUST_ALL_CERTS[0]);
        builder.hostnameVerifier((hostname, session) -> true);
    }

    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[] {
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
                return new java.security.cert.X509Certificate[] {};
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
