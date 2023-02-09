/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.auth;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCOSCachedCredentialsProvider
        implements COSCredentialsProvider, Closeable {
    
    private static final Logger log = LoggerFactory.getLogger(AbstractCOSCachedCredentialsProvider.class);

    private volatile COSCredentials cachedCredentials = null;
    private volatile long lastRefreshTime = System.currentTimeMillis() / 1000;
    private long refreshPeriodSeconds = 30;

    private ScheduledExecutorService executors = null;

    public AbstractCOSCachedCredentialsProvider(long refreshPeriodSeconds) {
        super();
        if (refreshPeriodSeconds <= 0) {
            throw new IllegalArgumentException("refreshPeriodSeconds must be positive num");
        }
        this.refreshPeriodSeconds = refreshPeriodSeconds;
        executors = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setName("credentials-refresh-daemon");
                t.setDaemon(true);
                return t;
            }
        });
        executors.scheduleWithFixedDelay(new Runnable() {
            
            @Override
            public void run() {
                if (ifNeedToRefreshCredentials()) {
                    updateCOSCredentials();
                }
            }
        }, 0, refreshPeriodSeconds, TimeUnit.SECONDS);
    }

    public synchronized void updateCOSCredentials() {
        if (ifNeedToRefreshCredentials()) {
            COSCredentials newCred = null;
            try {
                newCred = fetchNewCOSCredentials();
                if (newCred != null) {
                    cachedCredentials = newCred;
                    updateRefreshTime();
                    log.info("update new cos credentials");
                } else {
                    log.error("fetchNewCOSCredentials return null");
                }
            } catch (Exception e) {
                log.error("fetchNewCOSCredentials get exception.", e);
            }
        }
    }

    private void updateRefreshTime() {
        lastRefreshTime = System.currentTimeMillis() / 1000;
    }
    
    private boolean ifRefreshTimeExpired() {
        long currentTime = System.currentTimeMillis() / 1000;
        return (currentTime - lastRefreshTime >= refreshPeriodSeconds);
    }

    private boolean ifNeedToRefreshCredentials() {
        return cachedCredentials == null || ifRefreshTimeExpired();
    }

    @Override
    public COSCredentials getCredentials() {
        if (ifNeedToRefreshCredentials()) {
            updateCOSCredentials();
        }
        return cachedCredentials;
    }
    
    @Override
    protected void finalize() throws Throwable {
        if (executors != null) {
            executors.shutdownNow();
            executors = null;
        }
    }
    
    @Override
    public void close() throws IOException {
        if (executors != null) {
            executors.shutdownNow();
            executors = null;
        }
    }
    
    // fetch new credentials, this will be called periodically
    // you should implement this method.
    public abstract COSCredentials fetchNewCOSCredentials();

}
