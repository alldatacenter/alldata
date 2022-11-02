package com.alibaba.tesla.action.utils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.action.constant.TeslaResult;
import com.alibaba.tesla.action.common.TeslaResultBuilder;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 封装OKHttp发送Http请求
 * @author tandong.td@alibaba-inc.com
 */
public class TeslaOKHttpClient {

    static Logger LOG = LoggerFactory.getLogger(TeslaOKHttpClient.class);

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    private static class OkHttpClientHolder {
        private static final ConnectionPool pool = new ConnectionPool(2048, 60L, TimeUnit.SECONDS);
        private static final OkHttpClient INSTANCE = new OkHttpClient.Builder()
                .connectTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .writeTimeout(300, TimeUnit.SECONDS).connectionPool(pool)
                .build();
    }

    private static class ClientBuilderHolder {
        private static final ConnectionPool pool = new ConnectionPool(2048, 60L, TimeUnit.SECONDS);
        private static OkHttpClient.Builder INSTANCE = new OkHttpClient.Builder()
        .connectTimeout(300, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .writeTimeout(300, TimeUnit.SECONDS)
        .connectionPool(pool);
    }

    public static final OkHttpClient getOkHttpClient() {
        return OkHttpClientHolder.INSTANCE;
    }

    /**
     * 获取HTTP请求客户端
     * @param cookies 传入cookie
     * @return
     */
    public static final OkHttpClient getOkHttpClient(Map<String, String> cookies){
        OkHttpClient.Builder builder = ClientBuilderHolder.INSTANCE;
        if(null != cookies && cookies.size() > 0) {
            builder.cookieJar(new CookieJar(){
                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    ArrayList<Cookie> cookieList = new ArrayList<>();
                    Cookie.Builder cookieBuilder = new Cookie.Builder().hostOnlyDomain(url.host());
                    for (Map.Entry<String, String> entry : cookies.entrySet()) {
                        cookieBuilder.name(entry.getKey()).value(entry.getValue());
                        Cookie cookie = cookieBuilder.build();
                        cookieList.add(cookie);
                    }
                    return cookieList;
                }
            });
        }
        return builder.build();
    }

    public static final OkHttpClient getOkHttpClient(javax.servlet.http.Cookie[] cookies){
        OkHttpClient.Builder builder = ClientBuilderHolder.INSTANCE;
        if(null != cookies && cookies.length > 0) {
            builder.cookieJar(new CookieJar(){
                private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    cookieStore.put(url.host(), cookies);
                }
                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    ArrayList<Cookie> cookieList = new ArrayList<>();
                    Cookie.Builder cookieBuilder = new Cookie.Builder();
                    for(javax.servlet.http.Cookie c : cookies){
                        if(StringUtils.isEmpty(c.getName()) || StringUtils.isEmpty(c.getValue())){
                            continue;
                        }
                        cookieBuilder.name(c.getName());
                        cookieBuilder.value(c.getValue());
                        if(!StringUtils.isEmpty(c.getDomain())){
                            cookieBuilder.domain(c.getDomain());
                        }
                        if(!StringUtils.isEmpty(c.getPath())){
                            cookieBuilder.path(c.getPath());
                        }
                        if(c.getMaxAge() > 0){
                            cookieBuilder.expiresAt(System.currentTimeMillis() + c.getMaxAge()*1000);
                        }
                        if(c.isHttpOnly() && !StringUtils.isEmpty(c.getDomain())){
                            cookieBuilder.hostOnlyDomain(c.getDomain());
                        }
                        Cookie cookie = cookieBuilder.build();
                        cookieList.add(cookie);
                    }
                    return cookieList;
                }
            });
        }
        return builder.build();
    }

    /**
     * 使用Tesla认证请求头发送GET请求，返回JSON数据格式
     * @param url
     * @param app
     * @param accessKey
     * @param user
     * @param empId
     * @param password
     * @return
     */
    public static JSONObject getJsonWithHeader(String url, String app, String accessKey, String user, String empId, String password){
        Request request = null;
        Response response = null;
        try{
            OkHttpClient client = TeslaOKHttpClient.getOkHttpClient();

            request = new Request.Builder().url(url).
                    header("Connection","close").
                    header("x-auth-app", app).
                    header("x-auth-key", accessKey).
                    header("x-auth-user", user).
                    header("x-auth-passwd", password).
                    header("x-empid", empId).build();
            response = client.newCall(request).execute();
            return getResult(response);
        }catch(Exception e){
            LOG.error("请求错误:{}", url, e);
            return getExceptionJson(e);
        }finally {
            if(null != response){
                response.close();
            }
        }
    }

    public static TeslaResult post(String url, Object message, Map<String, String> heders) throws Exception {
        Request request = null;
        Request.Builder builder = new Request.Builder().url(url)
                .post(RequestBody.create(MEDIA_TYPE_JSON, JSONObject.toJSONString(message)));
        if(null != heders && heders.size() > 0){
            for(Map.Entry<String, String> entry : heders.entrySet()){
                builder.header(entry.getKey(), entry.getValue());
            }
        }
        request = builder.build();
        Response response = TeslaOKHttpClient.getOkHttpClient().newCall(request).execute();
        return getTeslaResult(response);
    }

    /**
     * 使用Tesla认证请求头发送POST请求，返回JSON数据格式
     * @param url
     * @param app
     * @param accessKey
     * @param user
     * @param empId
     * @param password
     * @return
     */
    public static JSONObject postJsonWithHeader(String url, Object message, String app, String accessKey, String user, String empId, String password){
        Request request = null;
        Response response = null;
        try{
            request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(MEDIA_TYPE_JSON, JSONObject.toJSONString(message)))
                    .header("x-auth-app", app).
                            header("x-auth-key", accessKey).
                            header("x-auth-user", user).
                            header("x-auth-passwd", password).
                            header("x-empid", empId).build();
            response = TeslaOKHttpClient.getOkHttpClient().newCall(request).execute();
            return getResult(response);
        }catch(Exception e){
            LOG.error("请求错误:{}", url, e);
            return getExceptionJson(e);
        }finally {
            if(null != response){
                response.close();
            }
        }
    }

    /**
     * 指定请求参数
     * @param method 类型
     * @param url
     * @param message
     * @param app
     * @param accessKey
     * @param user
     * @param empId
     * @param password
     * @return
     */
    public static JSONObject requestWithHeader(HttpMethod method, String url, Object message, String app, String accessKey, String user, String empId, String password){
        Request request = null;
        Response response = null;
        try{
            Request.Builder builder = new Request.Builder().url(url);
            switch(method) {
                case GET:
                    break;
                case PUT:
                    builder = builder.put(RequestBody.create(MEDIA_TYPE_JSON, JSONObject.toJSONString(message)));
                    break;
                case POST:
                    builder = builder.post(RequestBody.create(MEDIA_TYPE_JSON, JSONObject.toJSONString(message)));
                    break;
                case DELETE:
                    builder = builder.delete(RequestBody.create(MEDIA_TYPE_JSON, JSONObject.toJSONString(message)));
                    break;
                default:
                    throw new Exception("not support method");
            }
            request = builder.header("Connection","close").
                    header("x-auth-app", app).
                    header("x-auth-key", accessKey).
                    header("x-auth-user", user).
                    header("x-auth-passwd", password).
                    header("x-empid", empId).build();
            response = TeslaOKHttpClient.getOkHttpClient().newCall(request).execute();
            return getResult(response);
        }catch(Exception e){
            LOG.error("请求错误:{}", url, e);
            return getExceptionJson(e);
        }finally {
            if(null != response){
                response.close();
            }
        }
    }

    public static TeslaResult request(HttpMethod method, String url, Object message, String app, String accessKey, String user, String empId, String password){
        Request request = null;
        Response response = null;
        try{
            Request.Builder builder = new Request.Builder().url(url);
            switch(method) {
                case GET:
                    break;
                case PUT:
                    builder = builder.put(RequestBody.create(MEDIA_TYPE_JSON, JSONObject.toJSONString(message)));
                    break;
                case POST:
                    builder = builder.post(RequestBody.create(MEDIA_TYPE_JSON, JSONObject.toJSONString(message)));
                    break;
                case DELETE:
                    builder = builder.delete(RequestBody.create(MEDIA_TYPE_JSON, JSONObject.toJSONString(message)));
                    break;
                default:
                    throw new Exception("not support method");
            }
            request = builder.header("Connection","close").
                    header("x-auth-app", app).
                    header("x-auth-key", accessKey).
                    header("x-auth-user", user).
                    header("x-auth-passwd", password).
                    header("x-empid", empId).build();
            response = TeslaOKHttpClient.getOkHttpClient().newCall(request).execute();
            return getResponse(response);
        }catch(Exception e){
            LOG.error("请求错误:{}", url, e);
            return TeslaResultBuilder.failureResult(response.code(), null, response.message());
        }finally {
            if(null != response){
                response.close();
            }
        }
    }

    /**
     * 自定义请求类型和reader参数
     * @param method
     * @param url
     * @param message
     * @param headers
     * @return
     */
    public static JSONObject requestWithHeader(HttpMethod method,
                                               String url,
                                               Object message,
                                               Map<String, Object> headers){
        Request request = null;
        Response response = null;
        try{
            Request.Builder builder = new Request.Builder().url(url);
            switch(method) {
                case GET:
                    break;
                case PUT:
                    builder = builder.put(RequestBody.create(MEDIA_TYPE_JSON, JSONObject.toJSONString(message)));
                    break;
                case POST:
                    builder = builder.post(RequestBody.create(MEDIA_TYPE_JSON, JSONObject.toJSONString(message)));
                    break;
                case DELETE:
                    builder = builder.delete(RequestBody.create(MEDIA_TYPE_JSON, JSONObject.toJSONString(message)));
                    break;
                default:
                    throw new Exception("not support method");
            }
            builder = builder.header("Connection","close");
            for(Map.Entry entry : headers.entrySet()){
                if(null == entry.getKey()){
                    continue;
                }
                builder.header(entry.getKey().toString(), null == entry.getValue()?"":entry.getValue().toString());
            }
            request = builder.build();
            response = TeslaOKHttpClient.getOkHttpClient().newCall(request).execute();
            return getResult(response);
        }catch(Exception e){
            LOG.error("请求错误:{}", url, e);
            return getExceptionJson(e);
        }finally {
            if(null != response){
                response.close();
            }
        }
    }

    /**
     * 发送get请求，带header参数和cookie
     * @param url
     * @param headers
     * @param cookies
     * @return
     * @throws Exception
     */
    public static TeslaResult get(String url, Map<String, String> headers, Map<String, String> cookies) throws Exception {
        OkHttpClient client = TeslaOKHttpClient.getOkHttpClient(cookies);
        return getWithHeader(client, url, headers);
    }

    public static TeslaResult get(String url, Map<String, String> headers, javax.servlet.http.Cookie[] cookies) throws Exception {
        OkHttpClient client = TeslaOKHttpClient.getOkHttpClient(cookies);
        return getWithHeader(client, url, headers);
    }

    private static TeslaResult getWithHeader(OkHttpClient client, String url, Map<String, String> headers) throws Exception {
        Request.Builder builder = new Request.Builder().url(url);
        if(null != headers && headers.size() > 0 ){
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(),entry.getValue());
            }
        }
        Request request = builder.build();
        Response response = client.newCall(request).execute();
        return getTeslaResult(response);
    }

    private static TeslaResult getTeslaResult(Response response) throws Exception {
        if (response.isSuccessful()) {

                byte[] resBytes = readStream(response.body().byteStream());
            JSONObject responseObj = JSONObject.parseObject(new String(resBytes));

            TeslaResult teslaResult = new TeslaResult();
            teslaResult.setCode(Integer.valueOf(responseObj.get("code").toString()));
            teslaResult.setMessage(responseObj.get("message").toString());
            teslaResult.setData(responseObj.get("data"));
            response.close();
            return teslaResult;
        } else {
            return TeslaResultBuilder.failureResult(response.code(), null, response.message());
        }
    }

    private static TeslaResult getResponse(Response response) throws Exception {
        if (response.isSuccessful()) {

            byte[] resBytes = readStream(response.body().byteStream());
            TeslaResult teslaResult = new TeslaResult();
            teslaResult.setCode(TeslaResult.SUCCESS);
            teslaResult.setMessage("SUCCESS");
            teslaResult.setData(new String(resBytes));
            response.close();
            return teslaResult;
        } else {
            return TeslaResultBuilder.failureResult(response.code(), null, response.message());
        }
    }

    /**
     * 解析HTTP请求响应结果
     * @param response
     * @return
     */
    private static JSONObject getResult(Response response){
        if (response.isSuccessful()) {
            TeslaResult teslaResult = new TeslaResult();
            teslaResult.setCode(response.code());
            byte[] resBytes = null;
            try {
                resBytes = readStream(response.body().byteStream());
                teslaResult.setData(JSONObject.parseObject(new String(resBytes)));
            } catch (Exception e) {
                return getExceptionJson(e);
            }
            teslaResult.setMessage("SUCCESS");
            JSONObject result = JSONObject.parseObject(JSONObject.toJSONString(teslaResult));
            return result;
        } else {
            return getExceptionJson(new IOException("Unexpected code " + response));
        }
    }

    /**
     * 包装请求异常JSON返回
     * @param e
     * @return
     */
    private static JSONObject getExceptionJson(Exception e){
        String errorMsg = e.getMessage();
        if(null != e.getCause()){
            errorMsg = e.getCause().getMessage();
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", 500);
        jsonObject.put("data", new JSONObject());
        jsonObject.put("message", errorMsg);
        return jsonObject;
    }

    public static byte[] readStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        while ((len = inStream.read(buffer)) != -1) {
            outSteam.write(buffer, 0, len);
        }
        outSteam.close();
        inStream.close();
        return outSteam.toByteArray();
    }
}
