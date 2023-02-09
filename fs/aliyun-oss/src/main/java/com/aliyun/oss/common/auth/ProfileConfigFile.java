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

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.common.auth.InvalidCredentialsException;
import com.aliyun.oss.common.utils.AuthUtils;
import com.aliyun.oss.common.utils.LogUtils;
import com.aliyun.oss.common.utils.StringUtils;

/**
 * Loads the local OSS credential profiles from the standard location
 * (~/.oss/credentials), which can be easily overridden through the
 * <code>OSS_CREDENTIAL_PROFILES_FILE</code> environment variable or by
 * specifying an alternate credentials file location through this class'
 * constructor.
 * <p>
 * The OSS credentials file format allows you to specify multiple profiles, each
 * with their own set of OSS security credentials:
 * 
 * <pre>
 * [default]
 * oss_access_key_id=testAccessKey
 * oss_secret_access_key=testSecretKey
 * oss_session_token=testSessionToken
 * </pre>
 *
 * <p>
 * These credential profiles allow you to share multiple sets of OSS security
 * credentails between different tools such as the OSS SDK for Java and the OSS
 * CLI.
 */
public class ProfileConfigFile {

    /**
     * Loads the OSS credential profiles from the file. The path of the file is
     * specified as a parameter to the constructor.
     *
     * @param filePath
     *            File path to read from..
     */
    public ProfileConfigFile(String filePath) {
        this(new File(validateFilePath(filePath)));
    }

    /**
     * Loads the OSS credential profiles from the file. The path of the file is
     * specified as a parameter to the constructor.
     *
     * @param filePath
     *            File path to read from..
     * @param profileLoader
     *            A {@link ProfileConfigLoader} instance.
     */
    public ProfileConfigFile(String filePath, ProfileConfigLoader profileLoader) {
        this(new File(validateFilePath(filePath)), profileLoader);
    }

    private static String validateFilePath(String filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("Unable to load oss profiles: specified file path is null.");
        }
        return filePath;
    }

    /**
     * Loads the OSS credential profiles from the file. The reference to the
     * file is specified as a parameter to the constructor.
     *
     * @param file
     *            File object to read from..
     */
    public ProfileConfigFile(File file) {
        this(file, new ProfileConfigLoader());
    }

    /**
     * Loads the OSS credential profiles from the file. The reference to the
     * file is specified as a parameter to the constructor.
     *
     * @param file
     *            File object to read from..
     * @param profileLoader
     *            A {@link ProfileConfigLoader} instance.
     */
    public ProfileConfigFile(File file, ProfileConfigLoader profileLoader) {
        this.profileFile = file;
        this.profileLoader = profileLoader;
        this.profileFileLastModified = file.lastModified();
    }

    /**
     * Returns the OSS credentials for the specified profile.
     *
     * @return credentials
     */
    public Credentials getCredentials() {
        refresh();
        return credentials;
    }

    /**
     * Reread data from disk.
     */
    public void refresh() {
        if (credentials == null || profileFile.lastModified() > profileFileLastModified) {
            profileFileLastModified = profileFile.lastModified();

            Map<String, String> profileProperties = null;
            try {
                profileProperties = profileLoader.loadProfile(profileFile);
            } catch (IOException e) {
               LogUtils.logException("ProfilesConfigFile.refresh Exception:", e);
               return;
            }
            
            String accessKeyId = StringUtils.trim(profileProperties.get(AuthUtils.OSS_ACCESS_KEY_ID));
            String secretAccessKey = StringUtils.trim(profileProperties.get(AuthUtils.OSS_SECRET_ACCESS_KEY));
            String sessionToken = StringUtils.trim(profileProperties.get(AuthUtils.OSS_SESSION_TOKEN));

            if (StringUtils.isNullOrEmpty(accessKeyId)) {
                throw new InvalidCredentialsException("Access key id should not be null or empty.");
            }
            if (StringUtils.isNullOrEmpty(secretAccessKey)) {
                throw new InvalidCredentialsException("Secret access key should not be null or empty.");
            }

            credentials = new DefaultCredentials(accessKeyId, secretAccessKey, sessionToken);
        }
    } 

    private final File profileFile;
    private final ProfileConfigLoader profileLoader;
    private volatile long profileFileLastModified;
    private volatile DefaultCredentials credentials;
}
