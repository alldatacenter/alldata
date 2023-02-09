package com.aliyun.oss.internal.signer;

import com.aliyun.oss.common.auth.Credentials;

public class OSSSignerParams {
    /* Note that resource path should not have been url-encoded. */
    private String resourcePath;

    private Credentials credentials;

    private String product;

    private String region;

    private long tickOffset;

    private String cloudBoxId;

    public OSSSignerParams(String resourcePath, Credentials creds) {
        this.resourcePath = resourcePath;
        this.credentials = creds;
        this.tickOffset = 0;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials creds) {
        this.credentials = creds;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public long getTickOffset() {
        return tickOffset;
    }

    public void setTickOffset(long tickOffset) {
        this.tickOffset = tickOffset;
    }

    public String getCloudBoxId() {
        return cloudBoxId;
    }

    public void setCloudBoxId(String cloudBoxId) {
        this.cloudBoxId = cloudBoxId;
    }
}
