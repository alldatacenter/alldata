package com.alibaba.tesla.appmanager.client.lib;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class OAuth2Access {

    protected static OAuth2Response refreshAccessToken(String token, OAuth2Client oauth2Client) throws IOException {
        FormBody.Builder formBodyBuilder = new FormBody.Builder()
            .add(OAuth2Constant.POST_REFRESH_TOKEN, token);
        OAuth2Utils.postAddIfValid(formBodyBuilder, oauth2Client.getFieldsAsMap());

        final Request request = new Request.Builder()
            .url(oauth2Client.getSite())
            .post(formBodyBuilder.build())
            .build();
        return refreshTokenFromResponse(oauth2Client, request);
    }

    private static OAuth2Response refreshTokenFromResponse(
        final OAuth2Client oAuth2Client, final Request request) throws IOException {
        return getTokenFromResponse(oAuth2Client, oAuth2Client.getOkHttpClient(),
            request, new OAuth2State(OAuth2State.REFRESH_TOKEN));
    }

    protected static OAuth2Response getToken(OAuth2Client oauth2Client) throws IOException {
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        OAuth2Utils.postAddIfValid(formBodyBuilder, oauth2Client.getFieldsAsMap());
        OAuth2Utils.postAddIfValid(formBodyBuilder, oauth2Client.getParameters());
        return getAccessToken(oauth2Client, formBodyBuilder);
    }

    private static OAuth2Response getAccessToken(OAuth2Client oauth2Client, FormBody.Builder formBodyBuilder)
        throws IOException {
        final Request request = new Request.Builder()
            .url(oauth2Client.getSite())
            .post(formBodyBuilder.build())
            .build();
        return getTokenFromResponse(oauth2Client, request);
    }

    private static OAuth2Response getTokenFromResponse(
        final OAuth2Client oAuth2Client, final Request request) throws IOException {
        return getTokenFromResponse(oAuth2Client, oAuth2Client.getOkHttpClient(),
            request, new OAuth2State(OAuth2State.ACCESS_TOKEN));
    }

    private static OAuth2Response getTokenFromResponse(
        final OAuth2Client oauth2Client, final OkHttpClient okhttpClient,
        final Request request, final OAuth2State OAuth2State) throws IOException {
        Response response = okhttpClient.newBuilder()
            .authenticator(OAuth2Utils.getAuthenticator(oauth2Client, OAuth2State))
            .build()
            .newCall(request)
            .execute();
        return new OAuth2Response(response);
    }
}