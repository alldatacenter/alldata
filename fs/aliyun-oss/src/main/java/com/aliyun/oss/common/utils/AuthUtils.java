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

package com.aliyun.oss.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.aliyun.oss.common.auth.PublicKey;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.ram.model.v20150501.DeletePublicKeyRequest;
import com.aliyuncs.ram.model.v20150501.ListPublicKeysRequest;
import com.aliyuncs.ram.model.v20150501.ListPublicKeysResponse;
import com.aliyuncs.ram.model.v20150501.UploadPublicKeyRequest;
import com.aliyuncs.ram.model.v20150501.UploadPublicKeyResponse;

public class AuthUtils {

    /**
     * Default expiration time
     */
    public static final int DEFAULT_EXPIRED_DURATION_SECONDS = 3600;

    /**
     * Default expiration time adjustment factor
     */
    public static final double DEFAULT_EXPIRED_FACTOR = 0.8;

    /**
     * The maximum number of retries when getting AK/SK from ECS
     */
    public static final int MAX_ECS_METADATA_FETCH_RETRY_TIMES = 3;

    /**
     * AK/SK expiration time obtained from ECS Metadata Service, default 6 hours
     */
    public static final int DEFAULT_ECS_SESSION_TOKEN_DURATION_SECONDS = 3600 * 6;

    /**
     * AK/SK expire time obtained from STS, default 1 hour
     */
    public static final int DEFAULT_STS_SESSION_TOKEN_DURATION_SECONDS = 3600 * 1;

    /**
     * Connection timeout when getting AK/SK, the default 5 seconds
     */
    public static final int DEFAULT_HTTP_SOCKET_TIMEOUT_IN_MILLISECONDS = 5000;

    /**
     * Environment variable name for the oss access key ID
     */
    public static final String ACCESS_KEY_ENV_VAR = "OSS_ACCESS_KEY_ID";

    /**
     * Environment variable name for the oss secret key
     */
    public static final String SECRET_KEY_ENV_VAR = "OSS_ACCESS_KEY_SECRET";

    /**
     * Environment variable name for the oss session token
     */
    public static final String SESSION_TOKEN_ENV_VAR = "OSS_SESSION_TOKEN";

    /**
     * System property used when starting up the JVM to enable the default
     * metrics collected by the OSS SDK.
     *
     * <pre>
     * Example: -Doss.accessKeyId
     * </pre>
     */
    /** System property name for the OSS access key ID */
    public static final String ACCESS_KEY_SYSTEM_PROPERTY = "oss.accessKeyId";

    /** System property name for the OSS secret key */
    public static final String SECRET_KEY_SYSTEM_PROPERTY = "oss.accessKeySecret";

    /** System property name for the OSS session token */
    public static final String SESSION_TOKEN_SYSTEM_PROPERTY = "oss.sessionToken";

    /**
     * Loads the local OSS credential profiles from the standard location
     * (~/.oss/credentials), which can be easily overridden through the
     * <code>OSS_CREDENTIAL_PROFILES_FILE</code> environment variable.
     * <p>
     * The OSS credentials file format allows you to specify multiple profiles,
     * each with their own set of OSS security credentials:
     * 
     * <pre>
     * [default]
     * oss_access_key_id=testAccessKey
     * oss_secret_access_key=testSecretKey
     * oss_session_token=testSessionToken
     *
     * [test-user]
     * oss_access_key_id=testAccessKey
     * oss_secret_access_key=testSecretKey
     * oss_session_token=testSessionToken
     * </pre>
     */
    /** Credential profile file at the default location */
    public static final String DEFAULT_PROFILE_PATH = defaultProfilePath();

    /** Default section name in the profile file */
    public static final String DEFAULT_SECTION_NAME = "default";

    /** Property name for specifying the OSS Access Key */
    public static final String OSS_ACCESS_KEY_ID = "oss_access_key_id";

    /** Property name for specifying the OSS Secret Access Key */
    public static final String OSS_SECRET_ACCESS_KEY = "oss_secret_access_key";

    /** Property name for specifying the OSS Session Token */
    public static final String OSS_SESSION_TOKEN = "oss_session_token";

    /**
     * Get the default profile path.
     * 
     * @return Default profile path
     */
    public static String defaultProfilePath() {
        return System.getProperty("user.home") + File.separator + ".oss" + File.separator + "credentials";
    }

    /**
     * Upload the public key of RSA key pair.
     * 
     * @param regionId
     *            RAM's available area.
     * @param accessKeyId
     *            Access Key ID of the root user.
     * @param accessKeySecret
     *            Secret Access Key of the root user.
     * @param publicKey
     *            Public key content.
     * @return Public key description, include public key id etc.
     * @throws ClientException
     *            If any errors are encountered in the client while making the
     *            request or handling the response.
     */
    public static PublicKey uploadPublicKey(String regionId, String accessKeyId, String accessKeySecret,
            String publicKey) throws ClientException {
        DefaultProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
        DefaultAcsClient client = new DefaultAcsClient(profile);

        UploadPublicKeyRequest uploadPublicKeyRequest = new UploadPublicKeyRequest();
        uploadPublicKeyRequest.setPublicKeySpec(publicKey);

        UploadPublicKeyResponse uploadPublicKeyResponse = client.getAcsResponse(uploadPublicKeyRequest);
        com.aliyuncs.ram.model.v20150501.UploadPublicKeyResponse.PublicKey pubKey = uploadPublicKeyResponse
                .getPublicKey();

        return new PublicKey(pubKey);
    }

    /**
     * List the public keys that has been uploaded.
     * 
     * @param regionId
     *            RAM's available area.
     * @param accessKeyId
     *            Access Key ID of the root user.
     * @param accessKeySecret
     *            Secret Access Key of the root user.
     * @return Public keys.
     * @throws ClientException
     *            If any errors are encountered in the client while making the
     *            request or handling the response.
     */
    public static List<PublicKey> listPublicKeys(String regionId, String accessKeyId, String accessKeySecret)
            throws ClientException {
        DefaultProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
        DefaultAcsClient client = new DefaultAcsClient(profile);

        ListPublicKeysRequest listPublicKeysRequest = new ListPublicKeysRequest();
        ListPublicKeysResponse listPublicKeysResponse = client.getAcsResponse(listPublicKeysRequest);

        List<PublicKey> publicKeys = new ArrayList<PublicKey>();
        for (com.aliyuncs.ram.model.v20150501.ListPublicKeysResponse.PublicKey publicKey : listPublicKeysResponse
                .getPublicKeys()) {
            publicKeys.add(new PublicKey(publicKey));
        }

        return publicKeys;
    }

    /**
     * Delete the uploaded public key.
     * 
     * @param regionId
     *            RAM's available area.
     * @param accessKeyId
     *            Access Key ID of the root user.
     * @param accessKeySecret
     *            Secret Access Key of the root user.
     * @param publicKeyId
     *            Public Key Id.
     * @throws ClientException
     *            If any errors are encountered in the client while making the
     *            request or handling the response.
     */
    public static void deletePublicKey(String regionId, String accessKeyId, String accessKeySecret, String publicKeyId)
            throws ClientException {
        DefaultProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
        DefaultAcsClient client = new DefaultAcsClient(profile);

        DeletePublicKeyRequest deletePublicKeyRequest = new DeletePublicKeyRequest();
        deletePublicKeyRequest.setUserPublicKeyId(publicKeyId);

        client.getAcsResponse(deletePublicKeyRequest);
    }

    /**
     * Load public key content from file and format.
     * 
     * @param publicKeyPath
     *            Public key file path.
     * @return Formatted public key content.
     * @throws IOException
     *            if an I/O error occurs
     */
    public static String loadPublicKeyFromFile(String publicKeyPath) throws IOException {
        File file = new File(publicKeyPath);
        byte[] filecontent = new byte[(int) file.length()];
        InputStream in = null;

        try {
            in = new FileInputStream(file);
            in.read(filecontent);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }

        return new String(filecontent);
    }

    /**
     * Load private key content from file and format.
     * 
     * @param privateKeyPath
     *            Private key file path.
     * @return Formatted private key content.
     * @throws IOException
     *            if an I/O error occurs
     */
    public static String loadPrivateKeyFromFile(String privateKeyPath) throws IOException {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();

        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(privateKeyPath)));
            String str = null;
            while (true) {
                str = reader.readLine();
                if (str == null) {
                    break;
                }

                if (str.indexOf("-----BEGIN PRIVATE KEY-----") != -1) {
                    continue;
                }

                if (str.indexOf("-----END PRIVATE KEY-----") != -1) {
                    break;
                }

                if (str != null) {
                    builder.append(str + "\n");
                }
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

        return builder.toString();
    }

}
