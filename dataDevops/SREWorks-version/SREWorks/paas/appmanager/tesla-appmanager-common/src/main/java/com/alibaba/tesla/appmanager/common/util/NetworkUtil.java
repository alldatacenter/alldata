package com.alibaba.tesla.appmanager.common.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import okhttp3.*;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.apache.commons.lang3.StringUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * 网络工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class NetworkUtil {

    /**
     * 下载指定 remoteUrl 对应的文件到本地 localPath 路径中
     *
     * @param remoteUrl 远端 URL
     * @param localPath 本地路径
     */
    public static void download(String remoteUrl, String localPath) {
        try {
            URL url = new URL(remoteUrl);
            ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(localPath);
            FileChannel fileChannel = fileOutputStream.getChannel();
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            throw new AppException(AppErrorCode.NETWORK_ERROR,
                    String.format("Download file from %s to %s failed", remoteUrl, localPath), e);
        }
    }

    /**
     * 拼接 URL 工具
     *
     * @param baseUrl   原 URL
     * @param extraPath 需要附加的 URL 路径
     * @return 拼接后的 URL
     */
    public static URL concatenate(URL baseUrl, String extraPath) throws URISyntaxException, MalformedURLException {
        URI uri = baseUrl.toURI();
        String newPath = uri.getPath() + '/' + extraPath;
        URI newUri = uri.resolve(newPath);
        return newUri.toURL();
    }

    /**
     * 拼接 URL 工具
     *
     * @param baseUrl   原 URL
     * @param extraPath 需要附加的 URL 路径
     * @return 拼接后的 URL
     */
    public static String concatenateStr(String baseUrl, String extraPath) {
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        if (extraPath.startsWith("/")) {
            extraPath = extraPath.substring(1);
        }
        return baseUrl + extraPath;
    }

    /**
     * 通过 okhttp 发送 HTTP 请求，工具代码 (裸 Response)
     *
     * @param httpClient     OkHttp Client
     * @param requestBuilder 请求内容 Builder
     * @return 返回 body 的 JSONObject
     */
    public static Response sendRequestSimple(OkHttpClient httpClient, Request.Builder requestBuilder, String authToken)
            throws IOException {
        Request request;
        if (!StringUtils.isEmpty(authToken)) {
            request = requestBuilder.header("Authorization", "Bearer " + authToken).build();
        } else {
            request = requestBuilder.header("X-EmpId", "SYSTEM").build();
        }
        return httpClient.newCall(request).execute();
    }

    /**
     * 通过 okhttp 发送 HTTP 请求，工具代码
     *
     * @param httpClient     OkHttp Client
     * @param requestBuilder 请求内容 Builder
     * @return 返回 body 的 JSONObject
     */
    public static JSONObject sendRequest(OkHttpClient httpClient, Request.Builder requestBuilder, String authToken)
            throws IOException {
        Request request;
        if (!StringUtils.isEmpty(authToken)) {
            request = requestBuilder.header("Authorization", "Bearer " + authToken).build();
        } else {
            request = requestBuilder.header("X-EmpId", "SYSTEM").build();
        }
        Response response = httpClient.newCall(request).execute();
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            throw new AppException(AppErrorCode.DEPLOY_ERROR, "cannot sync to external environment, null response");
        }
        String bodyStr = responseBody.string();
        if (response.code() != 200) {
            throw new AppException(AppErrorCode.DEPLOY_ERROR,
                    String.format("send request failed, http status not 200|response=%s", bodyStr));
        }
        JSONObject body;
        try {
            body = JSONObject.parseObject(bodyStr);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.DEPLOY_ERROR,
                    String.format("send request failed, response not json|response=%s", bodyStr));
        }
        int code = body.getIntValue("code");
        if (code != 200) {
            throw new AppException(AppErrorCode.DEPLOY_ERROR,
                    String.format("send request failed, response code not 200|response=%s", bodyStr));
        }
        return body;
    }

    /**
     * 创建 OkHttp 使用的 Body
     *
     * @param mediaType   MediaType
     * @param inputStream 请求流
     * @return RequestBody
     */
    public static RequestBody createRequestBodyByStream(final MediaType mediaType, final InputStream inputStream) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return mediaType;
            }

            @Override
            public long contentLength() {
                try {
                    return inputStream.available();
                } catch (IOException e) {
                    return 0;
                }
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                Source source = null;
                try {
                    source = Okio.source(inputStream);
                    sink.writeAll(source);
                } finally {
                    Util.closeQuietly(source);
                }
            }
        };
    }
}
