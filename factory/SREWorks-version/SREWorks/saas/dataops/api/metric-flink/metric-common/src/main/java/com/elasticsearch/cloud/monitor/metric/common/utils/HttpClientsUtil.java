package com.elasticsearch.cloud.monitor.metric.common.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

/**
 * @author xiaoping
 * @date 2019/12/20
 */
public class HttpClientsUtil {
    private static final Log log = LogFactory.getLog(HttpClientsUtil.class);
    static final int timeOut = 60 * 1000;

    private static void config(HttpRequestBase httpRequestBase) {
        // 设置Header等
        // httpRequestBase.setHeader("User-Agent", "Mozilla/5.0");
        // httpRequestBase
        // .setHeader("Accept",
        // "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        // httpRequestBase.setHeader("Accept-Language",
        // "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");// "en-US,en;q=0.5");
        // httpRequestBase.setHeader("Accept-Charset",
        // "ISO-8859-1,utf-8,gbk,gb2312;q=0.7,*;q=0.7");

        // 配置请求的超时设置
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(timeOut)
            .setConnectTimeout(timeOut).setSocketTimeout(timeOut).build();
        httpRequestBase.setConfig(requestConfig);
    }

    /**
     * 创建HttpClient对象
     *
     * 200, 40, 100
     *
     * @param maxTotal    最大连接数
     * @param maxPerRoute
     * @param maxRoute
     * @return
     */
    public static CloseableHttpClient createHttpClient(int maxTotal, int maxPerRoute, int maxRoute, String endpoint) {
        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory
            .getSocketFactory();
        LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory
            .getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder
            .<ConnectionSocketFactory>create().register("http", plainsf)
            .register("https", sslsf).build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(
            registry);
        // 将最大连接数增加
        cm.setMaxTotal(maxTotal);
        // 将每个路由基础的连接增加
        cm.setDefaultMaxPerRoute(maxPerRoute);
        HttpHost httpHost =   HttpHost.create(endpoint);
        log.info(httpHost.toHostString());
        // 将目标主机的最大连接数增加
        cm.setMaxPerRoute(new HttpRoute(httpHost), maxRoute);

        // 请求重试处理
        HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException exception,
                int executionCount, HttpContext context) {
                if (executionCount >= 5) {// 如果已经重试了5次，就放弃
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
                    return false;
                }
                if (exception instanceof InterruptedIOException) {// 超时
                    return false;
                }
                if (exception instanceof UnknownHostException) {// 目标服务器不可达
                    return false;
                }
                if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
                    return false;
                }
                if (exception instanceof SSLException) {// SSL握手异常
                    return false;
                }

                HttpClientContext clientContext = HttpClientContext
                    .adapt(context);
                HttpRequest request = clientContext.getRequest();
                // 如果请求是幂等的，就再次尝试
                if (!(request instanceof HttpEntityEnclosingRequest)) {
                    return true;
                }
                return false;
            }
        };

        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(cm)
            .setRetryHandler(httpRequestRetryHandler).build();

        return httpClient;
    }

    /**
     * POST请求URL获取内容
     *
     * @return
     * @throws IOException
     */
    public static String post(CloseableHttpClient client, String url, String body) throws IOException {
        HttpPost httppost = new HttpPost(url);
        config(httppost);
        httppost.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        CloseableHttpResponse response = null;
        try {
            response = client.execute(httppost, HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, "utf-8");
            EntityUtils.consume(entity);
            return result;
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                if (response != null) { response.close(); }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * GET请求URL获取内容
     *
     * @param url
     * @return
     */
    public static String get(CloseableHttpClient client, String url) {
        HttpGet httpget = new HttpGet(url);
        config(httpget);
        CloseableHttpResponse response = null;
        try {
            response = client.execute(httpget, HttpClientContext.create());
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, "utf-8");
            EntityUtils.consume(entity);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) { response.close(); }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}

