package com.alibaba.tesla.appmanager.server.lib.oauth2client;

public interface OAuth2ResponseCallback {

    void onResponse(OAuth2Response response);
}