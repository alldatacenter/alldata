package com.obs.services.internal.trans;

import com.obs.services.model.HttpMethodEnum;
import okhttp3.RequestBody;

import java.util.HashMap;
import java.util.Map;

public class NewTransResult {
    private Map<String, String> headers;
    private Map<String, String> userHeaders;
    private Map<String, String> params;
    private RequestBody body;
    private String bucketName;
    private String objectKey;
    private HttpMethodEnum httpMethod;
    private boolean encodeHeaders = false;
    private boolean encodeUrl = true;

    public NewTransResult() {
    }

    public NewTransResult(Map<String, String> headers) {
        this.headers = headers;
        this.params = new HashMap<>();
    }

    public Map<String, String> getHeaders() {
        if (this.headers == null) {
            headers = new HashMap<>();
        }
        return this.headers;
    }

    public Map<String, String> getParams() {
        if (this.params == null) {
            params = new HashMap<>();
        }
        return this.params;
    }

    public Map<String, String> getUserHeaders() {
        if (this.userHeaders == null) {
            userHeaders = new HashMap<>();
        }
        return this.userHeaders;
    }

    public void setUserHeaders(Map<String, String> userHeaders) {
        this.userHeaders = userHeaders;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setBody(RequestBody body) {
        this.body = body;
    }

    public void setHttpMethod(HttpMethodEnum httpMethod) {
        this.httpMethod = httpMethod;
    }

    public HttpMethodEnum getHttpMethod() {
        return httpMethod;
    }

    public RequestBody getBody() {
        return body;
    }

    public void setIsEncodeHeaders(boolean encodeHeaders) {
        this.encodeHeaders = encodeHeaders;
    }

    public boolean isEncodeHeaders() {
        return encodeHeaders;
    }

    public boolean isEncodeUrl() {
        return encodeUrl;
    }

    public void setEncodeUrl(boolean encodeUrl) {
        this.encodeUrl = encodeUrl;
    }
}