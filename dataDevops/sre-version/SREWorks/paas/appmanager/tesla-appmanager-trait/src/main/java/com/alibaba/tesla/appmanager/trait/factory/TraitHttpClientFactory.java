package com.alibaba.tesla.appmanager.trait.factory;

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
 * Trait Http Client 工厂
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class TraitHttpClientFactory {

    /**
     * HttpClient (Normal)
     */
    private static volatile OkHttpClient httpClient;

    private TraitHttpClientFactory() {
    }

    /**
     * 获取一个普通的 Http Client (无代理)
     *
     * @return OkHttpClient
     */
    public static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (TraitHttpClientFactory.class) {
                if (httpClient == null) {
                    OkHttpClient.Builder builder = new OkHttpClient.Builder();
                    setBuilder(builder);
                    httpClient = builder.build();
                }
            }
        }
        return httpClient;
    }

    /**
     * 设置一些 OkHttpBuilder 的公共属性
     *
     * @param builder Builder
     */
    private static void setBuilder(OkHttpClient.Builder builder) {
        builder.connectTimeout(30, TimeUnit.SECONDS);
        builder.readTimeout(1200, TimeUnit.SECONDS);
        builder.writeTimeout(1200, TimeUnit.SECONDS);
        builder.callTimeout(1200, TimeUnit.SECONDS);
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
