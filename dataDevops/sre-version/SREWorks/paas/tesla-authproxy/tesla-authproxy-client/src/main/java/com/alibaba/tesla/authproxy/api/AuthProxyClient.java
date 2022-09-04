package com.alibaba.tesla.authproxy.api;

import com.alibaba.tesla.authproxy.api.dto.TeslaUserInfoDo;
import com.alibaba.tesla.authproxy.api.exception.*;
import com.alibaba.tesla.authproxy.api.lib.AuthProxyResourceEnum;
import com.alibaba.tesla.authproxy.api.util.AuthProxyHttpUtil;
import com.google.gson.*;
import okhttp3.*;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 权代服务客户端
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class AuthProxyClient {

    private static final Logger logger = LoggerFactory.getLogger(AuthProxyClient.class);

    private String endpoint;

    private OkHttpClient client;

    // 通用 JSON 处理
    private Gson gson = new GsonBuilder().serializeNulls().create();

    /**
     * 初始化 AuthProxy Client
     * @param endpoint authproxy endpoint
     */
    public AuthProxyClient(String endpoint) {
        this.endpoint = endpoint;
        this.client = AuthProxyHttpUtil.initHttpClient();
        logger.info("AuthProxy client has initialized, endpoint={}", this.endpoint);
    }

    /**
     * 获取用户信息 (内部版本)
     * @return TeslaGetUserInfoResponse 对象
     */
    public TeslaUserInfoDo getUserDetail(String authUser, String authUserId)
        throws AuthProxyClientException, AuthProxyServerException {
        String apiResource = endpoint + AuthProxyResourceEnum.GET_USER_DETAIL.toURI();
        if (logger.isDebugEnabled()) {
            logger.debug("[AuthProxy] getUserDetail, authUser={}, authUserId={}", authUser, authUserId);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Auth-User", authUser);
        headers.put("X-Auth-UserId", authUserId);
        try {
            JsonElement response = doGet(apiResource, null, headers);
            if (logger.isDebugEnabled()) {
                logger.debug("[AuthProxy] getUserDetail response, response={}", gson.toJson(response));
            }
            return gson.fromJson(response, TeslaUserInfoDo.class);
        } catch (JsonSyntaxException e) {
            throw new AuthProxyServerException(e);
        }
    }

    /**
     * 获取用户信息
     * @param appId appId
     * @param request request 请求
     * @return TeslaGetUserInfoResponse 对象
     */
    public TeslaUserInfoDo getUserInfo(String appId, HttpServletRequest request)
            throws AuthProxyClientException, AuthProxyServerException {
        Map<String, String> headers = Collections.list(request.getHeaderNames()).stream()
                .collect(Collectors.toMap(p -> p, request::getHeader));
        String apiResource;
        String xBucId = request.getHeader("X-Buc-Id");
        String xAuthUser = request.getHeader("X-Auth-User");
        String xEmpId = request.getHeader("X-Empid");
        // 当不存在上面三个 Header 时，需要添加 appId 使用新版权代的登录信息
        if (xBucId == null || xAuthUser == null || xEmpId == null) {
            Map<String, String> params = new HashMap<>();
            params.put("appId", appId);
            apiResource = endpoint + AuthProxyResourceEnum.GET_USER_INFO.toURI()
                    + "?" + URLEncodedUtils.format(convertNamePair(params), "utf-8");
        } else {
            apiResource = endpoint + AuthProxyResourceEnum.OLD_GET_USER_INFO.toURI();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("[AuthProxy] getUserInfo, appId={}, headers={}, apiResource={}", appId, headers, apiResource);
        }

        try {
            JsonElement response = doGet(apiResource, null, headers);
            if (logger.isDebugEnabled()) {
                logger.debug("[AuthProxy] getUserInfo response, response={}", gson.toJson(response));
            }
            return gson.fromJson(response, TeslaUserInfoDo.class);
        } catch (JsonSyntaxException e) {
            throw new AuthProxyServerException(e);
        }
    }

    /**
     * 内部函数: 发送 GET 请求，并且添加指定的 headers, 返回相应的字符串
     *
     * @param url     URL
     * @param params  参数 map
     * @param headers 需要添加的 headers
     * @return 响应内容对应的 JsonElement
     */
    private JsonElement doGet(String url, Map<String, String> params, Map<String, String> headers)
            throws AuthProxyClientException, AuthProxyServerException {
        HttpUrl parsedUrl = HttpUrl.parse(url);
        if (parsedUrl == null) {
            throw new AuthProxyParamException("Authproxy url cannot be parsed: " + url);
        }
        HttpUrl.Builder queryUrl = parsedUrl.newBuilder();
        if (params != null) {
            for (Map.Entry<String, String> param : params.entrySet()) {
                queryUrl.addQueryParameter(param.getKey(), param.getValue());
            }
        }
        Request.Builder requestBuilder = new Request.Builder().url(queryUrl.build()).get();
        return doRequest(requestBuilder, headers);
    }

    /**
     * 内部函数: 发送请求，并且添加指定的 headers, 返回相应的字符串
     *
     * @param requestBuilder 已经构建好的请求
     * @return 响应内容对应的 JsonElement
     */
    private JsonElement doRequest(Request.Builder requestBuilder, Map<String, String> headers)
            throws AuthProxyServerException, AuthProxyClientException {
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                String name = header.getKey().toLowerCase();
                if (name.startsWith("x") || name.equals("cookie")) {
                    requestBuilder.addHeader(header.getKey(), header.getValue());
                }
            }
        }
        String responseStr;
        try {
            responseStr = makeHttpCall(requestBuilder.build());
        } catch (IOException e) {
            throw new AuthProxyServerException(String.format("Authproxy server call failed, message=%s",
                    e.getMessage()));
        }
        try {
            JsonObject responseJson = new JsonParser().parse(responseStr).getAsJsonObject();
            JsonElement code = responseJson.get("code");
            if (code == null) {
                throw new AuthProxyServerException(String.format("Authproxy server response code is " +
                        "null, raw=%s", responseStr));
            }
            switch (code.getAsInt()) {
                case 200:
                    break;
                case 400:
                    throw new AuthProxyClientException(String.format("Authproxy bad request, message=%s",
                            responseStr));
                case 401:
                    throw new AuthProxyUnauthorizedException();
                case 403:
                    throw new AuthProxyForbiddenException();
                default:
                    throw new AuthProxyServerException(String.format("Authproxy server response code " +
                            "not 200, raw=%s", responseStr));
            }
            JsonElement data = responseJson.get("data");
            if (data == null) {
                throw new AuthProxyServerException(String.format("Authproxy server response data" +
                        " is null, raw=%s", responseStr));
            }
            return data;
        } catch (Exception e) {
            if (e instanceof AuthProxyException) {
                throw e;
            } else {
                throw new AuthProxyServerException(String.format("Error when processing authproxy server response, " +
                        "raw=%s, message=%s", responseStr, e.getMessage()));
            }
        }
    }

    /**
     * 根据 Request 发起真正的 HTTP 请求，并返回 Response 字符串
     */
    private String makeHttpCall(Request request) throws IOException, AuthProxyServerException {
        Response response = client.newCall(request).execute();
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            response.close();
            throw new AuthProxyServerException("NULL body from authproxy interface");
        }
        String responseStr = responseBody.string();
        if (response.code() != 200) {
            Integer code = response.code();
            response.close();
            throw new AuthProxyServerException(String.format("Authproxy server response status code " +
                    "is %d, message=%s", code, responseStr));
        }
        response.close();
        return responseStr;
    }

    /**
     * 将 map 转换为 list namevaluepair
     */
    public List<NameValuePair> convertNamePair(Map<String, String> pairs) {
        List<NameValuePair> nvpList = new ArrayList<>(pairs.size());
        for (Map.Entry<String, String> entry : pairs.entrySet()) {
            nvpList.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        return nvpList;
    }

}
