package com.alibaba.tesla.action.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.tesla.action.utils.TeslaOKHttpClient;
import com.alibaba.tesla.action.constant.TeslaResult;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.OkHttpClient.Builder;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
public class ApiService {

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * 信任所有上下文
     */
    private static final SSLContext trustAllSslContext;

    private static final TrustManager[] trustAllCerts = new TrustManager[] {
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

    static {
        try {
            trustAllSslContext = SSLContext.getInstance("SSL");
            trustAllSslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
    private static final SSLSocketFactory trustAllSslSocketFactory = trustAllSslContext.getSocketFactory();

    private static class ClientBuilderHolder {
        private static final ConnectionPool pool;
        private static Builder INSTANCE;

        private ClientBuilderHolder() {
        }

        static {
            pool = new ConnectionPool(128, 60L, TimeUnit.SECONDS);
            INSTANCE = (new Builder()).connectTimeout(30L, TimeUnit.SECONDS).readTimeout(30L, TimeUnit.SECONDS).retryOnConnectionFailure(true).writeTimeout(30L, TimeUnit.SECONDS).connectionPool(pool);
            INSTANCE.sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            INSTANCE.hostnameVerifier((hostName, session) -> true);
        }
    }

    public  OkHttpClient getOkHttpClient(final Cookie[] cookies) {
        Builder builder = ClientBuilderHolder.INSTANCE;

        if (null != cookies && cookies.length > 0) {
            builder.cookieJar(new CookieJar() {
                private final HashMap<String, List<okhttp3.Cookie>> cookieStore = new HashMap();

                @Override
                public void saveFromResponse(HttpUrl url, List<okhttp3.Cookie> cookiesx) {
                    this.cookieStore.put(url.host(), cookiesx);
                }

                @Override
                public List<okhttp3.Cookie> loadForRequest(HttpUrl url) {
                    ArrayList<okhttp3.Cookie> cookieList = new ArrayList();
                    okhttp3.Cookie.Builder cookieBuilder = new okhttp3.Cookie.Builder();
                    Cookie[] var4 = cookies;
                    int var5 = var4.length;
                    //log.info("cookie length: {}", var5);

                    for(int var6 = 0; var6 < var5; ++var6) {
                        Cookie c = var4[var6];
                        if (!StringUtils.isEmpty(c.getName()) && !StringUtils.isEmpty(c.getValue())) {
                            cookieBuilder.name(c.getName());
                            cookieBuilder.value(c.getValue());
                            //log.info("----get cookie name: {}, value: {}", c.getName(), c.getValue());


                            if (c.getDomain() != null && !StringUtils.isEmpty(c.getDomain())) {
                                log.info("------null domain: {}", c.getDomain());
                                cookieBuilder.domain(c.getDomain());
                            }
                            else {
                                cookieBuilder.domain("alibaba-inc.com");
                            }

                            if (c.getPath() != null && !StringUtils.isEmpty(c.getPath())) {
                                cookieBuilder.path(c.getPath());
                            }

                            if (c.getMaxAge() > 0) {
                                cookieBuilder.expiresAt(System.currentTimeMillis() + (long)(c.getMaxAge() * 1000));
                            }

                            if (c.isHttpOnly() && c.getDomain() !=null && !StringUtils.isEmpty(c.getDomain())) {
                                cookieBuilder.hostOnlyDomain(c.getDomain());
                            }

                            okhttp3.Cookie cookie = cookieBuilder.build();
                            cookieList.add(cookie);
                        }
                    }

                    return cookieList;
                }
            });
        }

        return builder.build();
    }


    private static JSONObject getExceptionJson(Exception e) {
        String errorMsg = e.getMessage();
        if (null != e.getCause()) {
            errorMsg = e.getCause().getMessage();
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", 500);
        jsonObject.put("data", new JSONObject());
        jsonObject.put("message", errorMsg);
        return jsonObject;
    }

    private static JSONObject getResult(Response response) {
        if (response.isSuccessful()) {
            TeslaResult teslaResult = new TeslaResult();
            teslaResult.setCode(response.code());
            Object var2 = null;

            try {
                byte[] resBytes = TeslaOKHttpClient.readStream(response.body().byteStream());
                teslaResult.setData(JSONObject.parseObject(new String(resBytes)));
            } catch (Exception var4) {
                return getExceptionJson(var4);
            }

            teslaResult.setMessage("SUCCESS");
            JSONObject result = JSONObject.parseObject(JSONObject.toJSONString(teslaResult));
            return result;
        } else {
            return getExceptionJson(new IOException("Unexpected code " + response));
        }
    }


    public JSONObject call(HttpMethod method, String url, Map<String, String> headers, Object message,
        Cookie[] cookies) {
        OkHttpClient client = getOkHttpClient(cookies);


        Response response = null;

        JSONObject var7;
        try {
            okhttp3.Request.Builder builder = (new okhttp3.Request.Builder()).url(url);
            log.info("[APiService] get api message: {}, get api url: {}, get api method: {}", message, url, method);
            Date curTime = new Date();
            switch (method) {
                case GET:
                    break;
                case PUT:
                    builder = builder.put(RequestBody.create(MEDIA_TYPE_JSON, JSONObject.toJSONString(message, SerializerFeature.WriteMapNullValue)));
                    break;
                case POST:
                    builder = builder.post(RequestBody.create(MEDIA_TYPE_JSON, JSONObject.toJSONString(message, SerializerFeature.WriteMapNullValue)));
                    break;
                case DELETE:
                    builder = builder.delete(RequestBody.create(MEDIA_TYPE_JSON, JSONObject.toJSONString(message, SerializerFeature.WriteMapNullValue)));
                    break;
                default:
                    throw new Exception("not support method");
            }

            builder = builder.header("Connection", "close");
            Iterator var14 = headers.entrySet().iterator();

            while (var14.hasNext()) {
                Entry entry = (Entry)var14.next();
                if (null != entry.getKey()) {
                    builder.header(entry.getKey().toString(),
                        null == entry.getValue() ? "" : entry.getValue().toString());
                }
            }

            Request request = builder.build();
            response = client.newCall(request).execute();
            Date newTime = new Date();
            Long millsecondCost = newTime.getTime() - curTime.getTime();
            log.error("[ApiService] method: {}, call api {} cost {} millseconds, get response:  {}", method, url, millsecondCost, response);
            var7 = getResult(response);
            log.error("[Api Service] method: {}, call api {} get response body: {}, cost {} millseconds", method, url, var7, millsecondCost);
            return var7;
        } catch (Exception var12) {
            log.error("请求错误:{}", url, var12);
            var7 = getExceptionJson(var12);
        } finally {
            if (null != response) {
                response.close();
            }

        }

        return var7;

    }
}
