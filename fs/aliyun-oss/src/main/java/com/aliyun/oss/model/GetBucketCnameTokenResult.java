package com.aliyun.oss.model;

public class GetBucketCnameTokenResult extends GenericResult{
    /**
     * the bucket name.
     */
    private String bucket;

    /**
     * the cname.
     */
    private String cname;

    /**
     * the token.
     */
    private String token;

    /**
     * the expire time.
     */
    private String expireTime;

    /**
     * Gets the bucket name.
     * @return null if no bucket name exists.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Sets the bucket name.
     * @param bucket the bucket name that the response contains.
     */
    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    /**
     * Gets the cname.
     * @return null if no cname exists.
     */
    public String getCname() {
        return cname;
    }

    /**
     * Sets the cname.
     * @param cname the cname that the response contains.
     */
    public void setCname(String cname) {
        this.cname = cname;
    }

    /**
     * Gets the token.
     * @return null if no token exists.
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the token.
     * @param token the token that the response contains.
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Gets the expire time.
     * @return null if no expire time exists.
     */
    public String getExpireTime() {
        return expireTime;
    }

    /**
     * Sets the expire time.
     * @param expireTime the expire time that the response contains.
     */
    public void setExpireTime(String expireTime) {
        this.expireTime = expireTime;
    }
}
