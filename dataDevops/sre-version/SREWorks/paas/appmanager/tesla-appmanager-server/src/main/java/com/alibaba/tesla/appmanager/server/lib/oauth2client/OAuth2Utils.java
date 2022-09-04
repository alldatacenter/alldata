package com.alibaba.tesla.appmanager.server.lib.oauth2client;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.Response;

import java.util.Map;

@Slf4j
class OAuth2Utils {

    protected static boolean isJsonResponse(Response response) {
        return response.body() != null
                && response.body().contentType() != null
                && response.body().contentType().subtype().equals("json");
    }

    protected static Authenticator getAuthenticator(final OAuth2Client oAuth2Client, final OAuth2State OAuth2State) {
        return (route, response) -> {
            String credential = "";

            OAuth2State.nextState();
            if (OAuth2State.isBasicAuth()) {
                credential = Credentials.basic(oAuth2Client.getUsername(), oAuth2Client.getPassword());
            } else if (OAuth2State.isAuthorizationAuth()) {
                credential = Credentials.basic(oAuth2Client.getClientId(), oAuth2Client.getClientSecret());
            } else if (OAuth2State.isFinalAuth()) {
                return null;
            }

            return response.request().newBuilder()
                    .header(OAuth2Constant.HEADER_AUTHORIZATION, credential)
                    .build();
        };
    }

    protected static void postAddIfValid(FormBody.Builder formBodyBuilder, Map<String, String> params) {
        if (params == null) return;

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (isValid(entry.getValue())) {
                formBodyBuilder.add(entry.getKey(), entry.getValue());
            }
        }
    }

    private static boolean isValid(String s) {
        return (s != null && s.trim().length() > 0);
    }
}