package com.qcloud.cos.auth;

import com.qcloud.cos.exception.CosClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

public class InstanceCredentialsProvider implements COSCredentialsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceCredentialsProvider.class);

    private static final long DEFAULT_REFRESH_INTERVAL_MILLISECONDS = 30 * 1000;
    private static final int DEFAULT_MAX_FETCH_RETRY_TIMES = 3;

    private InstanceProfileCredentials credentials;
    private InstanceCredentialsFetcher fetcher;
    private long refreshIntervalInMilliseconds = DEFAULT_REFRESH_INTERVAL_MILLISECONDS;           // millisecond
    private int fetchRetryTimes = DEFAULT_MAX_FETCH_RETRY_TIMES;
    private long lastFailedRefreshTimeInMilliseconds = 0;
    private ReentrantLock lock = new ReentrantLock();

    public InstanceCredentialsProvider(InstanceCredentialsFetcher fetcher) {
        this.fetcher = fetcher;
    }

    public InstanceCredentialsProvider withRefreshInterval(long refreshIntervalInMilliseconds) {
        this.refreshIntervalInMilliseconds = refreshIntervalInMilliseconds;
        return this;
    }

    public InstanceCredentialsProvider withFetchRetryTimes(int fetchRetryTimes) {
        this.fetchRetryTimes = fetchRetryTimes;
        return this;
    }

    @Override
    public COSCredentials getCredentials() {
        if (null == this.credentials || this.credentials.isExpired()) {
            try {
                this.lock.lock();
                if (null == this.credentials || this.credentials.isExpired()) {
                    try {
                        this.credentials = (InstanceProfileCredentials) this.fetcher.fetch(this.fetchRetryTimes);
                    } catch (CosClientException e) {
                        LOG.error("The InstanceCredentials fetch an exception.", e);
                        return null;
                    }
                }
            } finally {
                this.lock.unlock();
            }
        } else if (this.credentials.willSoonExpire() && this.shouldRefresh()) {
            try {
                this.lock.lock();
                try {
                    this.credentials = (InstanceProfileCredentials) this.fetcher.fetch();
                } catch (CosClientException e) {
                    this.lastFailedRefreshTimeInMilliseconds = System.currentTimeMillis();
                    LOG.warn("The InstanceCredentials fetch an exception. Wait for the next round to retry", e);
                }
            } finally {
                this.lock.unlock();
            }
        }

        return this.credentials;
    }

    @Override
    public void refresh() {
    }

    public boolean shouldRefresh() {
        if (null == this.credentials) {
            return true;
        }

        return System.currentTimeMillis() - this.lastFailedRefreshTimeInMilliseconds > this.refreshIntervalInMilliseconds;
    }
}
