package com.alibaba.tesla.appmanager.client.lib;

public class OAuth2Token {

    protected Long expires_in;
    protected String token_type;
    protected String refresh_token;
    protected String access_token;
    protected String scope;

    public Long getExpiresIn() {
        return expires_in;
    }

    public String getTokenType() {
        return token_type;
    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public String getAccessToken() {
        return access_token;
    }

    public String getScope() {
        return scope;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expires_in = expiresIn;
    }

    public void setTokenType(String tokenType) {
        this.token_type = tokenType;
    }

    public void setRefreshToken(String refreshToken) {
        this.refresh_token = refreshToken;
    }

    public void setAccessToken(String accessToken) {
        this.access_token = accessToken;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}