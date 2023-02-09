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

import com.aliyuncs.auth.KeyPairCredentials;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;

/**
 * Credentials provider factory to share providers across potentially many
 * clients.
 */
public class CredentialsProviderFactory {

    /**
     * Create an instance of DefaultCredentialProvider.
     * 
     * @param accessKeyId
     *            Access Key ID.
     * @param secretAccessKey
     *            Secret Access Key.
     * @return A {@link DefaultCredentialProvider} instance.
     */
    public static DefaultCredentialProvider newDefaultCredentialProvider(String accessKeyId, String secretAccessKey) {
        return new DefaultCredentialProvider(accessKeyId, secretAccessKey);
    }

    /**
     * Create an instance of DefaultCredentialProvider.
     * 
     * @param accessKeyId
     *            Access Key ID.
     * @param secretAccessKey
     *            Secret Access Key.
     * @param securityToken
     *            Security Token from STS.
     * @return A {@link DefaultCredentialProvider} instance.
     */
    public DefaultCredentialProvider newDefaultCredentialProvider(String accessKeyId, String secretAccessKey,
            String securityToken) {
        return new DefaultCredentialProvider(accessKeyId, secretAccessKey, securityToken);
    }

    /**
     * Create an instance of EnvironmentVariableCredentialsProvider by reading
     * the environment variable to obtain the ak/sk, such as OSS_ACCESS_KEY_ID
     * and OSS_ACCESS_KEY_SECRET
     * 
     * @return A {@link EnvironmentVariableCredentialsProvider} instance.
     * @throws ClientException
     *             OSS Client side exception.
     */
    public static EnvironmentVariableCredentialsProvider newEnvironmentVariableCredentialsProvider()
            throws ClientException {
        return new EnvironmentVariableCredentialsProvider();
    }

    /**
     * Create an instance of EnvironmentVariableCredentialsProvider by reading
     * the java system property used when starting up the JVM to enable the
     * default metrics collected by the OSS SDK, such as -Doss.accessKeyId and
     * -Doss.accessKeySecret.
     * 
     * @return A {@link SystemPropertiesCredentialsProvider} instance.
     * @throws ClientException
     *             OSS Client side exception.
     */
    public static SystemPropertiesCredentialsProvider newSystemPropertiesCredentialsProvider() throws ClientException {
        return new SystemPropertiesCredentialsProvider();
    }

    /**
     * Create a new STSAssumeRoleSessionCredentialsProvider, which makes a
     * request to the Aliyun Security Token Service (STS), uses the provided
     * roleArn to assume a role and then request short lived session
     * credentials, which will then be returned by the credentials provider's
     * {@link CredentialsProvider#getCredentials()} method.
     * 
     * @param regionId
     *            RAM's available area, for more information about regionId, see
     *            <a href="https://help.aliyun.com/document_detail/40654.html">
     *            RegionIdList</a>.
     * @param accessKeyId
     *            Access Key ID of the child user.
     * @param accessKeySecret
     *            Secret Access Key of the child user.
     * @param roleArn
     *            The ARN of the Role to be assumed.
     * @return A {@link STSAssumeRoleSessionCredentialsProvider} instance.
     * @throws ClientException
     *             OSS Client side exception.
     */
    public static STSAssumeRoleSessionCredentialsProvider newSTSAssumeRoleSessionCredentialsProvider(String regionId,
            String accessKeyId, String accessKeySecret, String roleArn) throws ClientException {
        DefaultProfile profile = DefaultProfile.getProfile(regionId);
        com.aliyuncs.auth.BasicCredentials basicCredentials = new com.aliyuncs.auth.BasicCredentials(accessKeyId,
                accessKeySecret);
        return new STSAssumeRoleSessionCredentialsProvider(basicCredentials, roleArn, profile);
    }

    /**
     * Create an instance of InstanceProfileCredentialsProvider obtained the
     * ak/sk by ECS Metadata Service.
     * 
     * @param roleName
     *            Role name of the ECS binding, NOT ROLE ARN.
     * @return A {@link InstanceProfileCredentialsProvider} instance.
     * @throws ClientException
     *             OSS Client side exception.
     */
    public static InstanceProfileCredentialsProvider newInstanceProfileCredentialsProvider(String roleName)
            throws ClientException {
        return new InstanceProfileCredentialsProvider(roleName);
    }

    /**
     * Create an instance of InstanceProfileCredentialsProvider based on RSA key
     * pair.
     * 
     * @param regionId
     *            RAM's available area, for more information about regionId, see
     *            <a href="https://help.aliyun.com/document_detail/40654.html">
     *            RegionIdList</a>.
     * @param publicKeyId
     *            Public Key ID.
     * @param privateKey
     *            Private Key.
     * @return A {@link STSKeyPairSessionCredentialsProvider} instance.
     * @throws ClientException
     *             OSS Client side exception.
     */
    public static STSKeyPairSessionCredentialsProvider newSTSKeyPairSessionCredentialsProvider(String regionId,
            String publicKeyId, String privateKey) throws ClientException {
        DefaultProfile profile = DefaultProfile.getProfile(regionId);
        KeyPairCredentials keyPairCredentials = new KeyPairCredentials(publicKeyId, privateKey);
        return new STSKeyPairSessionCredentialsProvider(keyPairCredentials, profile);
    }

    /**
     * Create an instance of InstanceProfileCredentialsProvider obtained the
     * ak/sk by the authorization server defined by the OSS. The protocol format
     * of the authorized service is as follows:
     * <p>
     * { 
     *     "StatusCode":"200", 
     *     "AccessKeyId":"STS.3p******gdasdg",
     *     "AccessKeySecret":"rpnwO9******rddgsR2YrTtI",
     *     "SecurityToken":"CAES......zZGstZGVtbzI=",
     *     "Expiration":"2017-11-06T09:16:56Z" 
     * }
     * </p>
     * An example of the authorized service to see
     * <a href="https://help.aliyun.com/document_detail/31926.html">
     * AuthorizedService</a>.
     * 
     * @param ossAuthServerHost
     *            The host of the authorized server, such as
     *            http://192.168.1.11:9090/sts/getsts.
     * @return A {@link CustomSessionCredentialsProvider} instance.
     * @throws ClientException
     *             OSS Client side exception.
     */
    public static CustomSessionCredentialsProvider newCustomSessionCredentialsProvider(String ossAuthServerHost)
            throws ClientException {
        return new CustomSessionCredentialsProvider(ossAuthServerHost);
    }

}