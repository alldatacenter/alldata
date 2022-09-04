package com.alibaba.tesla.appmanager.server.storage.impl;

public class BaseStorage {

    protected String endpoint;
    protected String accessKey;
    protected String secretKey;

    public BaseStorage(String endpoint, String accessKey, String secretKey) {
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }
}
