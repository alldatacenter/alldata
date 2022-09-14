package com.alibaba.tesla.appmanager.server.service.unit.helper;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.server.factory.HttpClientFactory;
import com.alibaba.tesla.appmanager.server.lib.oauth2client.OAuth2Client;
import com.alibaba.tesla.appmanager.server.lib.oauth2client.OAuth2Response;
import com.alibaba.tesla.appmanager.server.repository.domain.UnitDO;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * 单元 HTTP Helper
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class UnitHttpHelper {

    private static String LOCALHOST = "localhost";

    /**
     * 获取指定单元的 appmanager 的 auth token
     *
     * @param unit       单元对象
     * @param httpClient http client (带 proxy)
     * @return auth token 字符串
     */
    public static String getAuthToken(UnitDO unit, OkHttpClient httpClient) {
        String endpoint = unit.getEndpoint();
        String authUsername = unit.getUsername();
        String authPassword = unit.getPassword();
        String authClientId = unit.getClientId();
        String authClientSecret = unit.getClientSecret();
        return getAuthToken(endpoint, authUsername, authPassword, authClientId, authClientSecret, httpClient);
    }

    /**
     * 获取指定的 appmanager 的 auth token
     *
     * @param endpoint         Endpoint
     * @param authUsername     Username
     * @param authPassword     Password
     * @param authClientId     Client ID
     * @param authClientSecret Client Secret
     * @param httpClient       HTTP Client
     * @return Auth Token
     */
    public static String getAuthToken(
            String endpoint, String authUsername, String authPassword, String authClientId, String authClientSecret,
            OkHttpClient httpClient) {
        boolean auth = true;
        String authToken = "";
        if (StringUtils.isAnyEmpty(authUsername, authPassword, authClientId, authClientSecret)) {
            auth = false;
        }
        if (auth) {
            String authUrl = String.format("%s/oauth/token", endpoint);
            OAuth2Client client = new OAuth2Client
                    .Builder(authUsername, authPassword, authClientId, authClientSecret, authUrl)
                    .okHttpClient(httpClient)
                    .scope("all")
                    .build();
            OAuth2Response authResponse;
            try {
                authResponse = client.requestAccessToken();
            } catch (IOException e) {
                throw new AppException(AppErrorCode.NETWORK_ERROR, "cannot get unit access token", e);
            }
            if (!authResponse.isSuccessful()) {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("cannot get unit auth token|url=%s|response=%s", authUrl, authResponse.getBody()));
            }
            authToken = authResponse.getAccessToken();
        }
        return authToken;
    }

    /**
     * 获取指定单元的 http client
     *
     * @param unit 单元对象
     * @return http client
     */
    public static OkHttpClient getHttpClient(UnitDO unit) {
        OkHttpClient httpClient;
        if (unit.getEndpoint().contains(LOCALHOST)) {
            httpClient = HttpClientFactory.getHttpClient();
        } else if (StringUtils.isNotEmpty(unit.getProxyIp()) && Integer.parseInt(unit.getProxyPort()) > 0) {
            httpClient = HttpClientFactory.getHttpClient(unit.getProxyIp(), Integer.parseInt(unit.getProxyPort()));
        } else {
            httpClient = HttpClientFactory.getHttpClient();
        }
        return httpClient;
    }
}
