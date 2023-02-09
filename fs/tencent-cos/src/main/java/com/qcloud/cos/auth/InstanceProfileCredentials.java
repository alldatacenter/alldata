package com.qcloud.cos.auth;

public class InstanceProfileCredentials extends BasicSessionCredentials {
    private static final long DEFAULT_EXPIRATION_DURATION_SECONDS = 1800;
    private static final double DEFAULT_EXPIRED_FACTOR = 0.2;

    private final long expiredTime;             // Unit is second
    private long expirationDurationSeconds = DEFAULT_EXPIRATION_DURATION_SECONDS;
    private double expiredFactor = DEFAULT_EXPIRED_FACTOR;

    @Deprecated
    public InstanceProfileCredentials(String appId, String accessKey, String secretKey, String sessionToken,
                                      long expiredTime) {
        super(appId, accessKey, secretKey, sessionToken);
        this.expiredTime = expiredTime;
        this.expirationDurationSeconds = this.expiredTime - (System.currentTimeMillis() / 1000);
    }

    public InstanceProfileCredentials(String accessKey, String secretKey, String sessionToken, long expiredTime) {
        super(accessKey, secretKey, sessionToken);
        this.expiredTime = expiredTime;
        this.expirationDurationSeconds = this.expiredTime - (System.currentTimeMillis() / 1000);
    }

    public InstanceProfileCredentials withExpiredFactor(double expiredFactor) {
        this.expiredFactor = expiredFactor;
        return this;
    }

    public long getExpiredTime() {
        return expiredTime;
    }

    public boolean isExpired() {
        //The 30 seconds before the expired time is defined as expired.
        return (System.currentTimeMillis() / 1000) >= (this.expiredTime - 30);
    }

    public boolean willSoonExpire() {
        // It is defined as the willSoonExpire, when the remaining time is less than the percentage of the
        // expirationDurationSeconds.
        return (this.expirationDurationSeconds * this.expiredFactor) >=
                (this.expiredTime - (System.currentTimeMillis() / 1000.0));
    }
}
