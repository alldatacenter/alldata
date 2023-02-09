/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.common.auth;

import java.util.concurrent.Semaphore;

import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.utils.AuthUtils;

/**
 * Credentials provider based on OSS configuration profiles. This provider vends
 * Credentials from the profile configuration file for the default profile, or
 * for a specific, named profile. The OSS credentials file locates at
 * ~/.oss/credentials on Linux, macOS, or Unix, or at C:\Users\USERNAME
 * \.oss\credentials on Windows. This file can contain multiple named profiles
 * in addition to a default profile.
 */
public class ProfileCredentialsProvider implements CredentialsProvider {
    
    /**
     * Different between millisecond and nanosecond.
     */
    private static final long DIFF_MILLI_AND_NANO = 1000 * 1000L;

    /**
     * Default refresh interval
     */
    private static final long DEFAULT_REFRESH_INTERVAL_NANOS = 5 * 60 * 1000 * 1000 * 1000L;

    /**
     * Default force reload interval
     */
    private static final long DEFAULT_FORCE_RELOAD_INTERVAL_NANOS = 2 * DEFAULT_REFRESH_INTERVAL_NANOS;

    /**
     * The credential profiles file from which this provider loads the security
     * credentials. Lazily loaded by the double-check idiom.
     */
    private volatile ProfileConfigFile profilesConfigFile;

    /**
     * When the profiles file was last refreshed.
     */
    private volatile long lastRefreshed;

    /**
     * The name of the credential profile
     */
    private final String profileName;

    /**
     * Used to have only one thread block on refresh, for applications making at
     * least one call every REFRESH_INTERVAL_NANOS.
     */
    private final Semaphore refreshSemaphore = new Semaphore(1);

    /**
     * Refresh interval. Defaults to {@link #DEFAULT_REFRESH_INTERVAL_NANOS}
     */
    private long refreshIntervalNanos = DEFAULT_REFRESH_INTERVAL_NANOS;

    /**
     * Force reload interval. Defaults to
     * {@link #DEFAULT_FORCE_RELOAD_INTERVAL_NANOS}
     */
    private long refreshForceIntervalNanos = DEFAULT_FORCE_RELOAD_INTERVAL_NANOS;

    /**
     * Creates a new profile credentials provider that returns the OSS security
     * credentials configured for the default profile. Loading the credential
     * file is deferred until the getCredentials() method is called.
     */
    public ProfileCredentialsProvider() {
        this(null);
    }

    /**
     * Creates a new profile credentials provider that returns the OSS security
     * credentials configured for the named profile. Loading the credential file
     * is deferred until the getCredentials() method is called.
     *
     * @param profileName
     *            The name of a local configuration profile.
     */
    public ProfileCredentialsProvider(String profileName) {
        this((ProfileConfigFile) null, profileName);
    }

    /**
     * Creates a new profile credentials provider that returns the OSS security
     * credentials for the specified profiles configuration file and profile
     * name.
     *
     * @param profilesConfigFilePath
     *            The file path where the profile configuration file is located.
     * @param profileName
     *            The name of a configuration profile in the specified
     *            configuration file.
     */
    public ProfileCredentialsProvider(String profilesConfigFilePath, String profileName) {
        this(new ProfileConfigFile(profilesConfigFilePath), profileName);
    }

    /**
     * Creates a new profile credentials provider that returns the OSS security
     * credentials for the specified profiles configuration file and profile
     * name.
     *
     * @param profilesConfigFile
     *            The profile configuration file containing the profiles used by
     *            this credentials provider or null to defer load to first use.
     * @param profileName
     *            The name of a configuration profile in the specified
     *            configuration file.
     */
    public ProfileCredentialsProvider(ProfileConfigFile profilesConfigFile, String profileName) {
        this.profilesConfigFile = profilesConfigFile;
        if (this.profilesConfigFile != null) {
            this.lastRefreshed = System.nanoTime();
        }
        if (profileName == null) {
            this.profileName = AuthUtils.DEFAULT_PROFILE_PATH;
        } else {
            this.profileName = profileName;
        }
    }
    
    @Override
    public void setCredentials(Credentials creds) {

    }

    @Override
    public Credentials getCredentials() {
        if (profilesConfigFile == null) {
            synchronized (this) {
                if (profilesConfigFile == null) {
                    profilesConfigFile = new ProfileConfigFile(profileName);
                    lastRefreshed = System.nanoTime();
                }
            }
        }

        // Periodically check if the file on disk has been modified
        // since we last read it.
        //
        // For active applications, only have one thread block.
        // For applications that use this method in bursts, ensure the
        // credentials are never too stale.
        long now = System.nanoTime();
        long age = now - lastRefreshed;
        if (age > refreshForceIntervalNanos) {
            refresh();
        } else if (age > refreshIntervalNanos) {
            if (refreshSemaphore.tryAcquire()) {
                try {
                    refresh();
                } finally {
                    refreshSemaphore.release();
                }
            }
        }

        return profilesConfigFile.getCredentials();
    }

    public void refresh() {
        if (profilesConfigFile != null) {
            profilesConfigFile.refresh();
            lastRefreshed = System.nanoTime();
        }
    }

    /**
     * Gets the refresh interval in milliseconds.
     *
     * @return milliseconds
     */
    public long getRefreshIntervalMillis() {
        return refreshIntervalNanos / DIFF_MILLI_AND_NANO;
    }

    /**
     * Sets the refresh interval in milliseconds.
     *
     * @param refreshIntervalMillis milliseconds.        
     */
    public void setRefreshIntervalNanos(long refreshIntervalMillis) {
        this.refreshIntervalNanos = refreshIntervalMillis * DIFF_MILLI_AND_NANO;
    }

    /**
     * Gets the forced refresh interval in milliseconds.
     *
     * @return milliseconds.
     */
    public long getRefreshForceIntervalMillis() {
        return refreshForceIntervalNanos / DIFF_MILLI_AND_NANO;
    }

    /**
     * Sets the forced refresh interval in milliseconds.
     * 
     * @param refreshForceIntervalMillis milliseconds.
     */
    public void setRefreshForceIntervalMillis(long refreshForceIntervalMillis) {
        this.refreshForceIntervalNanos = refreshForceIntervalMillis * DIFF_MILLI_AND_NANO;
    }

}
