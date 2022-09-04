package com.alibaba.tesla.appmanager.server.lib.oauth2client;

import lombok.Data;

@Data
class OAuth2Token {

    protected Long expires_in;
    protected String token_type;
    protected String refresh_token;
    protected String access_token;
    protected String scope;
}