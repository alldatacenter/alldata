package com.alibaba.tesla.authproxy.util;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.common.base.TeslaResult;
import com.alibaba.tesla.common.utils.TeslaResultBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发送HTTP请求工具类
 *
 * @author tandong.td@alibaba-inc.com
 *
 * Update by tandng.td 2017/6/29
 * Created by fangpei on 17/2/27.
 */
public class TeslaHttpClient {

    static Logger LOG = LoggerFactory.getLogger(TeslaHttpClient.class);

    static CloseableHttpClient httpClient;

    static {
        SSLContextBuilder builder = new SSLContextBuilder();
        SSLConnectionSocketFactory sslsf = null;
        try {
            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            }).build();
            sslsf = new SSLConnectionSocketFactory(builder.build());
            httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
            httpClient.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000);
            httpClient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);
        } catch (Exception e) {
            LOG.error("SSLConnectionSocketFactory init fail,use default");
            httpClient = HttpClients.createDefault();
        }

    }

    public static JSONObject getJsonWithHeader(String url, String app, String accessKey, String user, String empId, String password){
        try{
            URI uri = new URIBuilder(url).build();
            HttpGet get = new HttpGet(uri);
            get.setHeader("x-auth-app", app);
            get.setHeader("x-auth-key", accessKey);
            get.setHeader("x-auth-user", user);
            get.setHeader("x-empid", empId);
            get.setHeader("x-auth-passwd", password);

            if (LOG.isDebugEnabled()){
                LOG.debug("Send get request:{}, 请求头参数:x-auth-app:{},x-auth-key:{},x-auth-user:{}", url, app, accessKey, user);
            }
            HttpResponse response = TeslaHttpClient.httpRequest(get);

            TeslaResult teslaResult = new TeslaResult();
            teslaResult.setCode(response.getStatusLine().getStatusCode());
            teslaResult.setData(JSONObject.parseObject(EntityUtils.toString(response.getEntity(), "utf-8")));
            teslaResult.setMessage("SUCCESS");
            JSONObject result = JSONObject.parseObject(JSONObject.toJSONString(teslaResult));
            return result;
        }catch(Exception e){
            LOG.error("请求错误", e);
            return getExceptionJson(e);
        }
    }

    /***
     * 发送GET请求，自带Tesla要求的请求头参数
     * @param url
     * @param app
     * @param accessKey
     * @param user
     * @param password
     * @return
     */
    public static TeslaResult getWithHeader(String url, String app, String accessKey, String user, String password){
        try{
            URI uri = new URIBuilder(url).build();
            HttpGet get = new HttpGet(uri);
            get.setHeader("x-auth-app", app);
            get.setHeader("x-auth-key", accessKey);
            get.setHeader("x-auth-user", user);
            get.setHeader("x-auth-passwd", password);

            if (LOG.isDebugEnabled()){
                LOG.debug("Send get request:{}, 请求头参数:x-auth-app:{},x-auth-key:{},x-auth-user:{}", url, app, accessKey, user);
            }
            String retJson = TeslaHttpClient.request(get);
            TeslaResult result = JSONObject.parseObject(retJson, TeslaResult.class);
            return result;
        }catch(Exception e){
            LOG.error("请求错误", e);
            String errorMsg = e.getMessage();
            if(null != e.getCause()){
                errorMsg = e.getCause().getMessage();
            }
            return TeslaResultBuilder.errorResult(errorMsg,"请求错误");
        }
    }

    /**
     * 发送HTTP get请求 将请求参数进行URLEncoder.encode处理
     * @param url 请求地址，支持?拼接参数，会对URL的参数值进行encode处理
     * @param app 应用标识
     * @param accessKey 应用访问Key
     * @param user 用户域账号
     * @param empId 用户工号
     * @param password 用户密码
     * @return
     */
    public static TeslaResult get(String url, String app, String accessKey, String user, String empId, String password){
        try{
            URL serverUrl = new URL(url);
            String httpGetString = "http://"+serverUrl.getHost();
            URIBuilder uriBuilder = new URIBuilder(httpGetString);
            uriBuilder.setPort(serverUrl.getPort());
            uriBuilder.setPath(serverUrl.getPath());
            if(!StringUtils.isEmpty(serverUrl.getQuery())){
                List<NameValuePair> params = URLEncodedUtils.parse(serverUrl.getQuery(), Charset.forName("utf-8"));
                for(NameValuePair param : params){
                    uriBuilder.setParameter(param.getName(), URLEncoder.encode(param.getValue(), "utf-8"));
                }
            }
            HttpGet get = new HttpGet(uriBuilder.build());
            get.setHeader("x-auth-app", app);
            get.setHeader("x-auth-key", accessKey);
            get.setHeader("x-auth-user", user);
            get.setHeader("x-empid", empId);
            get.setHeader("x-auth-passwd", password);

            LOG.debug("Send http get req:{}", get.getURI().toString());
            String retJson = TeslaHttpClient.request(get);
            TeslaResult result = JSONObject.parseObject(retJson, TeslaResult.class);
            return result;
        }catch(Exception e){
            LOG.error("请求错误", e);
            String errorMsg = e.getMessage();
            if(null != e.getCause()){
                errorMsg = e.getCause().getMessage();
            }
            return TeslaResultBuilder.errorResult(errorMsg,"请求错误");
        }
    }

    /**
     * 发送GET请求
     * @param url 会对URL的参数值进行encode处理
     * @return
     */
    public static TeslaResult get(String url){
        try{
            URL serverUrl = new URL(url);
            String httpGetString = "http://"+serverUrl.getHost();
            URIBuilder uriBuilder = new URIBuilder(httpGetString);
            uriBuilder.setPort(serverUrl.getPort());
            uriBuilder.setPath(serverUrl.getPath());
            List<NameValuePair> params = URLEncodedUtils.parse(serverUrl.getQuery(), Charset.forName("utf-8"));
            for(NameValuePair param : params){
                uriBuilder.setParameter(param.getName(), URLEncoder.encode(param.getValue(), "utf-8"));
            }
            HttpGet get = new HttpGet(uriBuilder.build());
            LOG.debug("Send http get req:{}", get.getURI().toString());
            String retJson = TeslaHttpClient.request(get);
            TeslaResult result = JSONObject.parseObject(retJson, TeslaResult.class);
            return result;
        }catch(Exception e){
            LOG.error("请求错误", e);
            String errorMsg = e.getMessage();
            if(null != e.getCause()){
                errorMsg = e.getCause().getMessage();
            }
            return TeslaResultBuilder.errorResult(errorMsg,"请求错误");
        }
    }

    /**
     * 发送get请求
     * @param url
     * @param headerParams
     * @return
     */
    public static TeslaResult get(String url, Map<String, Object> headerParams){
        try{
            URI uri = new URIBuilder(url).build();
            HttpGet get = new HttpGet(uri);
            for (String key : headerParams.keySet()) {
                Object value = headerParams.get(key);
                get.setHeader(key, String.valueOf(value));
            }
            return TeslaHttpClient.teslaResultRequest(get);
        }catch(Exception e){
            LOG.error("请求错误", e);
            String errorMsg = e.getMessage();
            if(null != e.getCause()){
                errorMsg = e.getCause().getMessage();
            }
            return TeslaResultBuilder.failureResult(TeslaResult.BAD_REQUEST,errorMsg);
        }
    }


    /**
     * 发送Http请求，参数form k v结构参数，非JSON
     * @param url
     * @param params 请求参数Map<String,String>
     * @param retCls 返回JSON对象，指定JSON转换的对象类型
     * @param <T> 返回对象范型
     * @return
     * @throws Exception
     */
    public static <T> T post(String url, Map<String, String> params, Class<T> retCls) throws Exception {
        URI uri = (new URIBuilder(url)).build();
        HttpPost post = new HttpPost(uri);
        List <NameValuePair> nvps = new ArrayList<NameValuePair>();
        for (String key : params.keySet()) {
            Object value = params.get(key);
            nvps.add(new BasicNameValuePair(key, String.valueOf(value)));
        }
        post.setEntity(new UrlEncodedFormEntity(nvps, Charset.forName("UTF-8")));
        String retJson = request(post);
        T result = JSONObject.parseObject(retJson, retCls);
        return result;
    }


    /**
     * 发送Http post请求，返回JSON数据
     * @param url
     * @return
     */
    public static String post(String url) {
        try {
            URI uri = (new URIBuilder(url)).build();
            HttpPost post = new HttpPost(uri);
            String retJson = request(post);
            return retJson;
        } catch (Exception e) {
            LOG.error("请求错误", e);
            String errorMsg = e.getMessage();
            if(null != e.getCause()){
                errorMsg = e.getCause().getMessage();
            }
            return errorMsg;
        }
    }

    /**
     * 发送http请求，返回JSON并将JSON转换为指定的class类型
     * @param url
     * @param retCls
     * @return
     */
    public static Object post(String url, Class retCls) {
        try {
            URI uri = (new URIBuilder(url)).build();
            HttpPost post = new HttpPost(uri);
            String retJson = request(post);
            Object result = JSONObject.parseObject(retJson, retCls);
            return result;
        } catch (Exception e) {
            LOG.error("请求错误", e);
            String errorMsg = e.getMessage();
            if(null != e.getCause()){
                errorMsg = e.getCause().getMessage();
            }
            return TeslaResultBuilder.errorResult(errorMsg,"请求错误");
        }
    }

    /**
     * 发送Http post请求，将message参数对象转换为JOSN发送
     * @param url
     * @param message
     * @param retCls
     * @return
     */
    public static Object post(String url, Object message, Class retCls) {
        try {
            URI uri = (new URIBuilder(url)).build();
            HttpPost post = new HttpPost(uri);
            String reqJson = JSONObject.toJSONString(message);
            StringEntity s = new StringEntity(reqJson, "utf-8");
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");
            post.setEntity(s);
            String retJson = request(post);
            LOG.debug("Send POST, body params:{}", reqJson);
            Object result = JSONObject.parseObject(retJson, retCls);
            return result;
        } catch (Exception e) {
            LOG.error("请求错误", e);
            String errorMsg = e.getMessage();
            if(null != e.getCause()){
                errorMsg = e.getCause().getMessage();
            }
            return TeslaResultBuilder.errorResult(errorMsg,"请求错误");
        }
    }

    /**
     * 发送Http POST请求，将message参数转换为json发送，
     * @param url
     * @param message
     * @return JSON
     */
    public static String post(String url, Object message) {
        try {
            URI uri = (new URIBuilder(url)).build();
            HttpPost post = new HttpPost(uri);
            String reqJson = JSONObject.toJSONString(message);
            StringEntity s = new StringEntity(reqJson, "utf-8");
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");
            post.setEntity(s);
            LOG.debug("Send POST, body params:{}", reqJson);
            String retJson = request(post);
            return retJson;
        } catch (Exception e) {
            LOG.error("请求错误", e);
            String errorMsg = e.getMessage();
            if(null != e.getCause()){
                errorMsg = e.getCause().getMessage();
            }
            return errorMsg;
        }
    }

    public static TeslaResult postWithHeader(String url, Object message, String app, String accessKey, String user, String password){
        try{
            URI uri = new URIBuilder(url).build();
            HttpPost post = new HttpPost(uri);
            post.setHeader("x-auth-app", app);
            post.setHeader("x-auth-key", accessKey);
            post.setHeader("x-auth-user", user);
            post.setHeader("x-auth-passwd", password);

            String reqJson = JSONObject.toJSONString(message);
            StringEntity s = new StringEntity(reqJson,"utf-8");
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");
            post.setEntity(s);
            if(LOG.isDebugEnabled()){
                LOG.debug("Send POST, body params:{}", reqJson);
            }
            String retJson = TeslaHttpClient.request(post);

            if(LOG.isDebugEnabled()){
                LOG.debug("Http响应:{}", retJson);
            }
            TeslaResult result = JSONObject.parseObject(retJson, TeslaResult.class);
            return result;

        }catch(Exception e){
            LOG.error("请求错误", e);
            String errorMsg = e.getMessage();
            if(null != e.getCause()){
                errorMsg = e.getCause().getMessage();
            }
            return TeslaResultBuilder.errorResult(errorMsg,"请求错误");
        }
    }

    /**
     * 自带Tesla要求的请求头参数
     * @param url
     * @param message
     * @param app
     * @param accessKey
     * @param user
     * @param empId
     * @param password
     * @return
     */
    public static TeslaResult postWithHeader(String url, Object message, String app, String accessKey, String user, String empId, String password){

        try{
            URI uri = new URIBuilder(url).build();
            HttpPost post = new HttpPost(uri);
            post.setHeader("x-auth-app", app);
            post.setHeader("x-auth-key", accessKey);
            post.setHeader("x-auth-user", user);
            post.setHeader("x-empid", empId);
            post.setHeader("x-auth-passwd", password);

            String reqJson = JSONObject.toJSONString(message);
            StringEntity s = new StringEntity(reqJson,"utf-8");
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");
            post.setEntity(s);
            if(LOG.isDebugEnabled()){
                LOG.debug("Send POST, body params:{}", reqJson);
            }
            String retJson = TeslaHttpClient.request(post);

            if(LOG.isDebugEnabled()){
                LOG.debug("Http响应:{}", retJson);
            }
            TeslaResult result = JSONObject.parseObject(retJson, TeslaResult.class);
            return result;

        }catch(Exception e){
            LOG.error("请求错误", e);
            String errorMsg = e.getMessage();
            if(null != e.getCause()){
                errorMsg = e.getCause().getMessage();
            }
            return TeslaResultBuilder.errorResult(errorMsg,"请求错误");
        }
    }

    public static JSONObject postJsonWithHeader(String url, Object message, String app, String accessKey, String user, String empId, String password){

        try{
            URI uri = new URIBuilder(url).build();
            HttpPost post = new HttpPost(uri);
            post.setHeader("x-auth-app", app);
            post.setHeader("x-auth-key", accessKey);
            post.setHeader("x-auth-user", user);
            post.setHeader("x-empid", empId);
            post.setHeader("x-auth-passwd", password);

            String reqJson = JSONObject.toJSONString(message);
            StringEntity s = new StringEntity(reqJson,"utf-8");
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");
            post.setEntity(s);
            if(LOG.isDebugEnabled()){
                LOG.debug("Send POST, body params:{}", reqJson);
            }

            HttpResponse response = TeslaHttpClient.httpRequest(post);
            TeslaResult teslaResult = new TeslaResult();
            teslaResult.setCode(response.getStatusLine().getStatusCode());
            teslaResult.setData(JSONObject.parseObject(EntityUtils.toString(response.getEntity(), "utf-8")));
            teslaResult.setMessage("SUCCESS");
            JSONObject result = JSONObject.parseObject(JSONObject.toJSONString(teslaResult));
            return result;

        }catch(Exception e){
            LOG.error("请求错误", e);
            return getExceptionJson(e);
        }
    }

    private static JSONObject getExceptionJson(Exception e){
        String errorMsg = e.getMessage();
        if(null != e.getCause()){
            errorMsg = e.getCause().getMessage();
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", 500);
        jsonObject.put("data", "请求错误");
        jsonObject.put("message", errorMsg);
        return jsonObject;
    }

    /**
     * 发送POST请求,ContentType=application/x-www-form-urlencoded
     * @param url
     * @param message String类型参数，非JSON
     * @param app
     * @param accessKey
     * @param user
     * @param password
     * @return
     */
    public static TeslaResult postFormWithHeader(String url, String message, String app, String accessKey, String user, String password){

        try{
            URI uri = new URIBuilder(url).build();
            HttpPost post = new HttpPost(uri);
            post.setHeader("x-auth-app", app);
            post.setHeader("x-auth-key", accessKey);
            post.setHeader("x-auth-user", user);
            post.setHeader("x-auth-passwd", password);

            StringEntity s = new StringEntity(message,"utf-8");
            s.setContentEncoding("UTF-8");
            s.setContentType("application/x-www-form-urlencoded");
            post.setEntity(s);
            if(LOG.isDebugEnabled()){
                LOG.debug("Send POST, body params:{}", message);
            }
            String retJson = TeslaHttpClient.request(post);
            if(LOG.isDebugEnabled()){
                LOG.debug("Http响应:{}", retJson);
            }
            TeslaResult result = JSONObject.parseObject(retJson, TeslaResult.class);
            return result;

        }catch(Exception e){
            LOG.error("请求错误", e);
            String errorMsg = e.getMessage();
            if(null != e.getCause()){
                errorMsg = e.getCause().getMessage();
            }
            return TeslaResultBuilder.errorResult(errorMsg,"请求错误");
        }
    }

    /**
     * 发送POST请求，不带默认的Tesla请求头，将请求参数使用JSON格式发送
     * @param url
     * @param body body消息体数据
     * @param headerParams
     * @return
     * @throws Exception
     */
    public static TeslaResult post(String url, Object body, Map<String, Object> headerParams) throws Exception {
        try{
            URI uri = new URIBuilder(url).build();
            HttpPost post = new HttpPost(uri);
            for (String key : headerParams.keySet()) {
                Object value = headerParams.get(key);
                post.setHeader(key, String.valueOf(value));
            }
            String jsonData = JSONObject.toJSONString(body);
            LOG.debug("Send POST, body params:{}", jsonData);
            StringEntity s = new StringEntity(jsonData,"utf-8");
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");
            post.setEntity(s);
            return TeslaHttpClient.teslaResultRequest(post);
        }catch(Exception e){
            LOG.error("请求错误", e);
            String errorMsg = e.getMessage();
            if(null != e.getCause()){
                errorMsg = e.getCause().getMessage();
            }
            return TeslaResultBuilder.failureResult(TeslaResult.BAD_REQUEST,errorMsg);
        }
    }

    /**
     * 发送POST请求，不带默认的Tesla请求头，将请求参数使用JSON格式发送
     * @param url 请求地址
     * @param jsonData body消息体数据，请传入JSON格式
     * @param headerParams 用户自定义请求头参数
     * @return 返回TeslaResult，code data message三个属性格式
     * @throws Exception
     */
    public static TeslaResult post(String url, String jsonData, Map<String, Object> headerParams) throws Exception {
        try{
            URI uri = new URIBuilder(url).build();
            HttpPost post = new HttpPost(uri);
            for (String key : headerParams.keySet()) {
                Object value = headerParams.get(key);
                post.setHeader(key, String.valueOf(value));
            }
            LOG.debug("Send POST, body params:{}", jsonData);
            StringEntity s = new StringEntity(jsonData,"utf-8");
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");
            post.setEntity(s);
            return TeslaHttpClient.teslaResultRequest(post);
        }catch(Exception e){
            LOG.error("请求错误", e);
            String errorMsg = e.getMessage();
            if(null != e.getCause()){
                errorMsg = e.getCause().getMessage();
            }
            return TeslaResultBuilder.failureResult(TeslaResult.BAD_REQUEST,errorMsg);
        }
    }

    /**
     * 发送POST请求，不支持请求头参数
     * @param url
     * @param jsonData body，JSON格式
     * @return
     * @throws Exception
     */
    public static TeslaResult post(String url, String jsonData) throws Exception {
        try{
            URI uri = new URIBuilder(url).build();
            HttpPost post = new HttpPost(uri);
            LOG.debug("Send POST, body params:{}", jsonData);
            StringEntity s = new StringEntity(jsonData,"utf-8");
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");
            post.setEntity(s);
            return TeslaHttpClient.teslaResultRequest(post);
        }catch(Exception e){
            LOG.error("请求错误", e);
            String errorMsg = e.getMessage();
            if(null != e.getCause()){
                errorMsg = e.getCause().getMessage();
            }
            return TeslaResultBuilder.failureResult(TeslaResult.BAD_REQUEST,errorMsg);
        }
    }

    /**
     * 发送PUT请求
     * @param url
     * @param jsonData body，JSON格式
     * @return
     * @throws Exception
     */
    public static TeslaResult put(String url, Map<String, Object> headerParams, String jsonData) throws Exception {
        try{
            URI uri = new URIBuilder(url).build();
            HttpPut put = new HttpPut(uri);
            for (String key : headerParams.keySet()) {
                Object value = headerParams.get(key);
                put.setHeader(key, String.valueOf(value));
            }
            LOG.debug("Send PUT, body params:{}", jsonData);
            StringEntity s = new StringEntity(jsonData,"utf-8");
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");
            put.setEntity(s);
            return TeslaHttpClient.teslaResultRequest(put);
        }catch(Exception e){
            LOG.error("请求错误", e);
            String errorMsg = e.getMessage();
            if(null != e.getCause()){
                errorMsg = e.getCause().getMessage();
            }
            return TeslaResultBuilder.failureResult(TeslaResult.BAD_REQUEST,errorMsg);
        }
    }

    /**
     * 执行发送HTTP请求
     * @param request HTTP请求对象
     *
     * @throws Exception
     */
    public static String request(HttpUriRequest request) throws Exception {

        CloseableHttpResponse httpResponse = null;
        String response = "";
        try {
            httpResponse = httpClient.execute(request);
            response = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if(LOG.isDebugEnabled()){
                LOG.debug("HTTP请求返回状态:{},返回内容:{}", statusCode, response);
            }
            if(statusCode != HttpStatus.OK.value()){
                //非200拼接异常
                return JSONObject.toJSONString(TeslaResultBuilder.failureResult(statusCode,null, response));
            }
            return response;
        } catch (Exception e) {
            LOG.error("HTTP请求失败", e);
            throw e;
        } finally {
            if(null != httpResponse){
                httpResponse.close();
            }
        }
    }

    public static HttpResponse httpRequest(HttpUriRequest request) throws Exception {

        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(request);
            return httpResponse;
        } catch (Exception e) {
            LOG.error("HTTP请求失败", e);
            throw e;
        } finally {
            if(null != httpResponse){
                httpResponse.close();
            }
        }
    }

    public static TeslaResult teslaResultRequest(HttpUriRequest request) throws Exception {

        Map<String, Object> ret = new HashMap<String, Object>();
        CloseableHttpResponse httpResponse = null;
        String response = "";
        try {
            httpResponse = httpClient.execute(request);
            response = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if(LOG.isDebugEnabled()){
                LOG.debug("HTTP请求返回状态:{},返回内容:{}", statusCode, response);
            }
            return TeslaResultBuilder.successResult(statusCode, response, "success");
        } catch (Exception e) {
            LOG.error("HTTP请求失败", e);
            throw e;
        } finally {
            if(null != httpResponse){
                httpResponse.close();
            }
        }
    }
}