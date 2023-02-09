package com.qcloud.cos;

class TemporyToken {
    private String tempSecretId;
    private String tempSecretKey;
    private String tempToken;

    public TemporyToken(String tempSecretId, String tempSecretKey, String tempToken) {
        super();
        this.tempSecretId = tempSecretId;
        this.tempSecretKey = tempSecretKey;
        this.tempToken = tempToken;
    }

    public String getTempSecretId() {
        return tempSecretId;
    }

    public String getTempSecretKey() {
        return tempSecretKey;
    }

    public String getTempToken() {
        return tempToken;
    }
}
