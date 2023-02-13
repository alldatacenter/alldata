package com.alibaba.tesla.authproxy.api.util;

import okhttp3.ConnectionPool;
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
 * 权代服务 Http 工具类
 */
public class AuthProxyHttpUtil {

    /**
     * 连接池大小
     */
    private static final int POOL_SIZE = 30;

    /**
     * 连接池连接保留时间 (s)
     */
    private static final long KEEP_ALIVE_DURATION = 30L;

    /**
     * 初始化 okhttp client
     */
    public static OkHttpClient initHttpClient() {
        ConnectionPool pool = new ConnectionPool(POOL_SIZE, KEEP_ALIVE_DURATION, TimeUnit.MINUTES);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager)TRUST_ALL_CERTS[0]);
        builder.hostnameVerifier((hostname, session) -> true);
        builder.connectionPool(pool);
        return builder.build();
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
