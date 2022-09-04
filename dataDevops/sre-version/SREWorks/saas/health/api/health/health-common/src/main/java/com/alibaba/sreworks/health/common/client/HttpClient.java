package com.alibaba.sreworks.health.common.client;

import okhttp3.OkHttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP请求客户端
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/27 10:33
 */
public class HttpClient {
    /**
     * OkHttpClient
     */
    private static volatile OkHttpClient client;

    private HttpClient() {
    }

    /**
     * 获取一个普通的 Http Client (无代理)
     *
     * @return OkHttpClient
     */
    public static OkHttpClient getHttpClient() {
        if (client == null) {
            synchronized (HttpClient.class) {
                if (client == null) {
                    OkHttpClient.Builder builder = new OkHttpClient.Builder();
                    setBuilder(builder);
                    client = builder.build();
                }
            }
        }
        return client;
    }

    /**
     * 设置一些 OkHttpBuilder 的公共属性
     *
     * @param builder Builder
     */
    private static void setBuilder(OkHttpClient.Builder builder) {
        builder.connectTimeout(10, TimeUnit.SECONDS);
        builder.readTimeout(1200, TimeUnit.SECONDS);
        builder.writeTimeout(1200, TimeUnit.SECONDS);
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
