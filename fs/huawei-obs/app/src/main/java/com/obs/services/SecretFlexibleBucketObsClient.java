/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */


package com.obs.services;

import java.util.List;

import com.obs.services.exception.ObsException;
import com.obs.services.internal.security.ProviderCredentialThreadContext;
import com.obs.services.internal.security.ProviderCredentials;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.BucketCors;
import com.obs.services.model.BucketLocationResponse;
import com.obs.services.model.BucketLoggingConfiguration;
import com.obs.services.model.BucketMetadataInfoRequest;
import com.obs.services.model.BucketMetadataInfoResult;
import com.obs.services.model.BucketNotificationConfiguration;
import com.obs.services.model.BucketPolicyResponse;
import com.obs.services.model.BucketQuota;
import com.obs.services.model.BucketStorageInfo;
import com.obs.services.model.BucketStoragePolicyConfiguration;
import com.obs.services.model.BucketTagInfo;
import com.obs.services.model.BucketVersioningConfiguration;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.LifecycleConfiguration;
import com.obs.services.model.ListBucketsRequest;
import com.obs.services.model.ObsBucket;
import com.obs.services.model.OptionsInfoRequest;
import com.obs.services.model.OptionsInfoResult;
import com.obs.services.model.ReplicationConfiguration;
import com.obs.services.model.WebsiteConfiguration;

public abstract class SecretFlexibleBucketObsClient extends ObsClient {
    /**
     * Constructor
     * 
     * @param config
     *            Configuration parameters of ObsClient
     */
    public SecretFlexibleBucketObsClient(ObsConfiguration config) {
        this("", "", config);
    }

    /**
     * Constructor
     * 
     * @param endPoint
     *            OBS endpoint
     */
    public SecretFlexibleBucketObsClient(String endPoint) {
        this("", "", endPoint);
    }

    /**
     * Constructor
     * 
     * @param accessKey
     *            AK in the access key
     * @param secretKey
     *            SK in the access key
     * @param config
     *            Configuration parameters of ObsClient
     */
    public SecretFlexibleBucketObsClient(String accessKey, String secretKey, ObsConfiguration config) {
        super(accessKey, secretKey, config);
    }

    /**
     * Constructor
     * 
     * @param accessKey
     *            AK in the access key
     * @param secretKey
     *            SK in the access key
     * @param endPoint
     *            OBS endpoint
     */
    public SecretFlexibleBucketObsClient(String accessKey, String secretKey, String endPoint) {
        super(accessKey, secretKey, endPoint);
    }

    /**
     * Constructor
     * 
     * @param accessKey
     *            AK in the temporary access key
     * @param secretKey
     *            SK in the temporary access key
     * @param securityToken
     *            Security token
     * @param config
     *            Configuration parameters of ObsClient
     */
    public SecretFlexibleBucketObsClient(String accessKey, String secretKey, 
            String securityToken, ObsConfiguration config) {
        super(accessKey, secretKey, securityToken, config);
    }
    
    /**
     * Constructor
     * 
     * @param accessKey
     *            AK in the temporary access key
     * @param secretKey
     *            SK in the temporary access key
     * @param securityToken
     *            Security token
     * @param endPoint
     *            OBS endpoint
     */
    public SecretFlexibleBucketObsClient(String accessKey, String secretKey, String securityToken, String endPoint) {
        super(accessKey, secretKey, securityToken, endPoint);
    }
    
    public ObsBucket createBucket(ObsBucket bucket, String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucket.getBucketName(), accessKey, secretKey);
        try {
            return super.createBucket(bucket);
        } finally {
            this.clearContextProviderCredentials();
        }
    }
    
    public ObsBucket createBucket(ObsBucket bucket, String accessKey, String secretKey, String securityToken)
            throws ObsException {
        this.setContextProviderCredentials(bucket.getBucketName(), accessKey, secretKey, securityToken);
        try {
            return super.createBucket(bucket);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public List<ObsBucket> listBuckets(String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials("", accessKey, secretKey);
        try {
            return super.listBuckets(null);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public List<ObsBucket> listBuckets(String accessKey, String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials("", accessKey, secretKey, securityToken);
        try {
            return super.listBuckets(null);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public List<ObsBucket> listBuckets(ListBucketsRequest request, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials("", accessKey, secretKey);
        try {
            return super.listBuckets(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public List<ObsBucket> listBuckets(ListBucketsRequest request, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials("", accessKey, secretKey, securityToken);
        try {
            return super.listBuckets(request);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteBucket(String bucketName, String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.deleteBucket(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteBucket(String bucketName, String accessKey, String secretKey, String securityToken)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.deleteBucket(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }
    
    public boolean headBucket(String bucketName, String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.headBucket(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public boolean headBucket(String bucketName, String accessKey, String secretKey, String securityToken)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.headBucket(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }
    
    public BucketMetadataInfoResult getBucketMetadata(BucketMetadataInfoRequest bucketMetadataInfoRequest,
            String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketMetadataInfoRequest.getBucketName(), accessKey, secretKey);
        try {
            return super.getBucketMetadata(bucketMetadataInfoRequest);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketMetadataInfoResult getBucketMetadata(BucketMetadataInfoRequest bucketMetadataInfoRequest,
            String accessKey, String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketMetadataInfoRequest.getBucketName(),
                accessKey, secretKey, securityToken);
        try {
            return super.getBucketMetadata(bucketMetadataInfoRequest);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public AccessControlList getBucketAcl(String bucketName, String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketAcl(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public AccessControlList getBucketAcl(String bucketName, String accessKey, String secretKey, String securityToken)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketAcl(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse setBucketAcl(String bucketName, String cannedACL, AccessControlList acl, String accessKey,
            String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketAcl(bucketName, cannedACL, acl);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse setBucketAcl(String bucketName, String cannedACL, AccessControlList acl, String accessKey,
            String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketAcl(bucketName, cannedACL, acl);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketAcl(String bucketName, AccessControlList acl, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketAcl(bucketName, acl);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketAcl(String bucketName, AccessControlList acl, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketAcl(bucketName, acl);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketLocationResponse getBucketLocation(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketLocationV2(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketLocationResponse getBucketLocation(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketLocationV2(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketStorageInfo getBucketStorageInfo(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketStorageInfo(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketStorageInfo getBucketStorageInfo(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketStorageInfo(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketQuota getBucketQuota(String bucketName, String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketQuota(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketQuota getBucketQuota(String bucketName, String accessKey, String secretKey, String securityToken)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketQuota(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketQuota(String bucketName, BucketQuota bucketQuota, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketQuota(bucketName, bucketQuota);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketQuota(String bucketName, BucketQuota bucketQuota, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketQuota(bucketName, bucketQuota);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketCors(String bucketName, BucketCors bucketCors, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketCors(bucketName, bucketCors);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketCors(String bucketName, BucketCors bucketCors, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketCors(bucketName, bucketCors);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketCors getBucketCors(String bucketName, String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketCors(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketCors getBucketCors(String bucketName, String accessKey, String secretKey, String securityToken)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketCors(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteBucketCors(String bucketName, String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.deleteBucketCors(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteBucketCors(String bucketName, String accessKey, String secretKey, String securityToken)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.deleteBucketCors(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public OptionsInfoResult optionsBucket(String bucketName, OptionsInfoRequest optionInfo, String accessKey,
            String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.optionsBucket(bucketName, optionInfo);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public OptionsInfoResult optionsBucket(String bucketName, OptionsInfoRequest optionInfo, String accessKey,
            String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.optionsBucket(bucketName, optionInfo);
        } finally {
            this.clearContextProviderCredentials();
        }
    }
    
    @Deprecated
    public BucketLoggingConfiguration getBucketLoggingConfiguration(String bucketName, String accessKey,
            String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketLoggingConfiguration(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketLoggingConfiguration getBucketLogging(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketLogging(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public BucketLoggingConfiguration getBucketLoggingConfiguration(String bucketName, String accessKey,
            String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketLoggingConfiguration(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketLoggingConfiguration getBucketLogging(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketLogging(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse setBucketLoggingConfiguration(String bucketName,
            BucketLoggingConfiguration loggingConfiguration, String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketLoggingConfiguration(bucketName, loggingConfiguration);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketLogging(String bucketName, BucketLoggingConfiguration loggingConfiguration,
            String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketLogging(bucketName, loggingConfiguration);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse setBucketLoggingConfiguration(String bucketName,
            BucketLoggingConfiguration loggingConfiguration, String accessKey, String secretKey, String securityToken)
                    throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketLoggingConfiguration(bucketName, loggingConfiguration);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketLogging(String bucketName, BucketLoggingConfiguration loggingConfiguration,
            String accessKey, String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketLogging(bucketName, loggingConfiguration);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse setBucketLoggingConfiguration(String bucketName,
            BucketLoggingConfiguration loggingConfiguration, boolean updateTargetACLifRequired, String accessKey,
            String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketLoggingConfiguration(bucketName, loggingConfiguration, updateTargetACLifRequired);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse setBucketLoggingConfiguration(String bucketName,
            BucketLoggingConfiguration loggingConfiguration, boolean updateTargetACLifRequired, String accessKey,
            String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketLoggingConfiguration(bucketName, loggingConfiguration, updateTargetACLifRequired);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketVersioning(String bucketName, BucketVersioningConfiguration versioningConfiguration,
            String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketVersioning(bucketName, versioningConfiguration);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketVersioning(String bucketName, BucketVersioningConfiguration versioningConfiguration,
            String accessKey, String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketVersioning(bucketName, versioningConfiguration);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketVersioningConfiguration getBucketVersioning(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketVersioning(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketVersioningConfiguration getBucketVersioning(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketVersioning(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public LifecycleConfiguration getBucketLifecycleConfiguration(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketLifecycleConfiguration(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public LifecycleConfiguration getBucketLifecycle(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketLifecycle(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public LifecycleConfiguration getBucketLifecycleConfiguration(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketLifecycleConfiguration(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public LifecycleConfiguration getBucketLifecycle(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketLifecycle(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse setBucketLifecycleConfiguration(String bucketName, LifecycleConfiguration lifecycleConfig,
            String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketLifecycleConfiguration(bucketName, lifecycleConfig);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketLifecycle(String bucketName, LifecycleConfiguration lifecycleConfig,
            String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketLifecycle(bucketName, lifecycleConfig);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse setBucketLifecycleConfiguration(String bucketName, LifecycleConfiguration lifecycleConfig,
            String accessKey, String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketLifecycleConfiguration(bucketName, lifecycleConfig);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketLifecycle(String bucketName, LifecycleConfiguration lifecycleConfig,
            String accessKey, String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketLifecycle(bucketName, lifecycleConfig);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse deleteBucketLifecycleConfiguration(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.deleteBucketLifecycleConfiguration(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteBucketLifecycle(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.deleteBucketLifecycle(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse deleteBucketLifecycleConfiguration(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.deleteBucketLifecycleConfiguration(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteBucketLifecycle(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.deleteBucketLifecycle(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketPolicyResponse getBucketPolicy(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketPolicyV2(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketPolicyResponse getBucketPolicy(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketPolicyV2(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketPolicy(String bucketName, String policy, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketPolicy(bucketName, policy);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketPolicy(String bucketName, String policy, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketPolicy(bucketName, policy);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteBucketPolicy(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.deleteBucketPolicy(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteBucketPolicy(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.deleteBucketPolicy(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public WebsiteConfiguration getBucketWebsiteConfiguration(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketWebsiteConfiguration(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public WebsiteConfiguration getBucketWebsite(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketWebsite(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public WebsiteConfiguration getBucketWebsiteConfiguration(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketWebsiteConfiguration(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public WebsiteConfiguration getBucketWebsite(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketWebsite(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse setBucketWebsiteConfiguration(String bucketName, WebsiteConfiguration websiteConfig,
            String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketWebsiteConfiguration(bucketName, websiteConfig);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketWebsite(String bucketName, WebsiteConfiguration websiteConfig, String accessKey,
            String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketWebsite(bucketName, websiteConfig);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse setBucketWebsiteConfiguration(String bucketName, WebsiteConfiguration websiteConfig,
            String accessKey, String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketWebsiteConfiguration(bucketName, websiteConfig);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketWebsite(String bucketName, WebsiteConfiguration websiteConfig, String accessKey,
            String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketWebsite(bucketName, websiteConfig);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse deleteBucketWebsiteConfiguration(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.deleteBucketWebsiteConfiguration(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteBucketWebsite(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.deleteBucketWebsite(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse deleteBucketWebsiteConfiguration(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.deleteBucketWebsiteConfiguration(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteBucketWebsite(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.deleteBucketWebsite(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketTagging(String bucketName, BucketTagInfo bucketTagInfo, String accessKey,
            String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketTagging(bucketName, bucketTagInfo);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketTagging(String bucketName, BucketTagInfo bucketTagInfo, String accessKey,
            String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketTagging(bucketName, bucketTagInfo);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketTagInfo getBucketTagging(String bucketName, String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketTagging(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketTagInfo getBucketTagging(String bucketName, String accessKey, String secretKey, String securityToken)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketTagging(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteBucketTagging(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.deleteBucketTagging(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteBucketTagging(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.deleteBucketTagging(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse setBucketReplicationConfiguration(String bucketName,
            ReplicationConfiguration replicationConfiguration, String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketReplicationConfiguration(bucketName, replicationConfiguration);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketReplication(String bucketName, ReplicationConfiguration replicationConfiguration,
            String accessKey, String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketReplication(bucketName, replicationConfiguration);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse setBucketReplicationConfiguration(String bucketName,
            ReplicationConfiguration replicationConfiguration, String accessKey, String secretKey, String securityToken)
                    throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketReplicationConfiguration(bucketName, replicationConfiguration);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketReplication(String bucketName, ReplicationConfiguration replicationConfiguration,
            String accessKey, String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketReplication(bucketName, replicationConfiguration);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public ReplicationConfiguration getBucketReplicationConfiguration(String bucketName, String accessKey,
            String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketReplicationConfiguration(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public ReplicationConfiguration getBucketReplication(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketReplication(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public ReplicationConfiguration getBucketReplicationConfiguration(String bucketName, String accessKey,
            String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketReplicationConfiguration(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public ReplicationConfiguration getBucketReplication(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketReplication(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse deleteBucketReplicationConfiguration(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.deleteBucketReplicationConfiguration(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteBucketReplication(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.deleteBucketReplication(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    @Deprecated
    public HeaderResponse deleteBucketReplicationConfiguration(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.deleteBucketReplicationConfiguration(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse deleteBucketReplication(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.deleteBucketReplication(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketNotificationConfiguration getBucketNotification(String bucketName, String accessKey, String secretKey)
            throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketNotification(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketNotificationConfiguration getBucketNotification(String bucketName, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketNotification(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketNotification(String bucketName,
            BucketNotificationConfiguration bucketNotificationConfiguration, String accessKey, String secretKey)
                    throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketNotification(bucketName, bucketNotificationConfiguration);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketNotification(String bucketName,
            BucketNotificationConfiguration bucketNotificationConfiguration, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketNotification(bucketName, bucketNotificationConfiguration);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketStoragePolicy(final String bucketName,
            final BucketStoragePolicyConfiguration bucketStorage, String accessKey, String secretKey)
                    throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.setBucketStoragePolicy(bucketName, bucketStorage);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public HeaderResponse setBucketStoragePolicy(final String bucketName,
            final BucketStoragePolicyConfiguration bucketStorage, String accessKey, String secretKey,
            String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.setBucketStoragePolicy(bucketName, bucketStorage);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketStoragePolicyConfiguration getBucketStoragePolicy(final String bucketName, String accessKey,
            String secretKey) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey);
        try {
            return super.getBucketStoragePolicy(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    public BucketStoragePolicyConfiguration getBucketStoragePolicy(final String bucketName, String accessKey,
            String secretKey, String securityToken) throws ObsException {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, securityToken);
        try {
            return super.getBucketStoragePolicy(bucketName);
        } finally {
            this.clearContextProviderCredentials();
        }
    }

    protected void setContextProviderCredentials(String bucketName,
                                                 String accessKey, String secretKey, String securityToken) {
        ProviderCredentials providerCredentials = new ProviderCredentials(accessKey, secretKey, securityToken);
        providerCredentials.setIsAuthTypeNegotiation(this.getProviderCredentials().getIsAuthTypeNegotiation());
        providerCredentials.setAuthType(this.getProviderCredentials().getLocalAuthType(bucketName));
        ProviderCredentialThreadContext.getInstance().setProviderCredentials(providerCredentials);
    }

    protected void setContextProviderCredentials(String bucketName, String accessKey, String secretKey) {
        this.setContextProviderCredentials(bucketName, accessKey, secretKey, null);
    }
    
    protected void clearContextProviderCredentials() {
        ProviderCredentialThreadContext.getInstance().clearProviderCredentials();
    }
}
