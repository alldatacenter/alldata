package com.alibaba.tesla.appmanager.server.lib.oauth2client;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import okhttp3.Response;

import java.io.IOException;

public class OAuth2Response {

    private Response response;
    private String responseBody;
    private OAuth2Token token;
    private OAuth2Error error;
    private boolean jsonParsed;
    private Long expiresAt;

    protected OAuth2Response(Response response) throws IOException {
        this.response = response;
        if (response == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "null response provided");
        }
        responseBody = response.body().string();

        if (!OAuth2Utils.isJsonResponse(response)) {
            AppException e = new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("invalid oauth2 response %s", responseBody));
            error = new OAuth2Error(e);
            jsonParsed = false;
        } else if (!response.isSuccessful()) {
            try {
                error = JSONObject.parseObject(responseBody, OAuth2Error.class);
                jsonParsed = true;
            } catch (Exception e) {
                error = new OAuth2Error(e);
                jsonParsed = false;
            }
        } else {
            token = JSONObject.parseObject(responseBody, OAuth2Token.class);
            jsonParsed = true;
            if (token.expires_in != null) {
                expiresAt = (token.expires_in * 1000) + System.currentTimeMillis();
            }
        }
    }

    protected OAuth2Response(Exception e) {
        this.response = null;
        this.error = new OAuth2Error(e);
    }

    public boolean isSuccessful() {
        return response != null && response.isSuccessful() && jsonParsed;
    }

    public boolean isJsonResponse() {
        return jsonParsed;
    }

    public Integer getCode() {
        return response != null ? response.code() : null;
    }

    public Long getExpiresIn() {
        return token != null ? token.expires_in : null;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public String getTokenType() {
        return token != null ? token.token_type : null;
    }

    public String getRefreshToken() {
        return token != null ? token.refresh_token : null;
    }

    public String getAccessToken() {
        return token != null ? token.access_token : null;
    }

    public String getScope() {
        return token != null ? token.scope : null;
    }

    public String getBody() {
        return responseBody;
    }

    public OAuth2Error getOAuthError() {
        return error;
    }

    public Response getHttpResponse() {
        return response;
    }
}