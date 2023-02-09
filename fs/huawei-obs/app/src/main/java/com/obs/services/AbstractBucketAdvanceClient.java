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

import com.obs.services.exception.ObsException;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.BaseBucketRequest;
import com.obs.services.model.BucketDirectColdAccess;
import com.obs.services.model.BucketEncryption;
import com.obs.services.model.BucketNotificationConfiguration;
import com.obs.services.model.BucketPolicyResponse;
import com.obs.services.model.BucketTagInfo;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.LifecycleConfiguration;
import com.obs.services.model.ReplicationConfiguration;
import com.obs.services.model.SetBucketDirectColdAccessRequest;
import com.obs.services.model.SetBucketEncryptionRequest;
import com.obs.services.model.SetBucketLifecycleRequest;
import com.obs.services.model.SetBucketNotificationRequest;
import com.obs.services.model.SetBucketPolicyRequest;
import com.obs.services.model.SetBucketReplicationRequest;
import com.obs.services.model.SetBucketTaggingRequest;
import com.obs.services.model.SetBucketWebsiteRequest;
import com.obs.services.model.WebsiteConfiguration;
import com.obs.services.model.fs.GetBucketFSStatusRequest;
import com.obs.services.model.fs.GetBucketFSStatusResult;
import com.obs.services.model.fs.SetBucketFSStatusRequest;

public abstract class AbstractBucketAdvanceClient extends AbstractBucketClient {
    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketLifecycle(java.lang.String)
     */
    @Override
    public LifecycleConfiguration getBucketLifecycle(final String bucketName) throws ObsException {
        return this.getBucketLifecycle(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketLifecycle(java.lang.String)
     */
    @Override
    public LifecycleConfiguration getBucketLifecycle(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("getBucketLifecycleConfiguration", request.getBucketName(),
                new ActionCallbackWithResult<LifecycleConfiguration>() {

                    @Override
                    public LifecycleConfiguration action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.getBucketLifecycleConfigurationImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketLifecycle(java.lang.String,
     * com.obs.services.model.LifecycleConfiguration)
     */
    @Override
    public HeaderResponse setBucketLifecycle(final String bucketName, final LifecycleConfiguration lifecycleConfig)
            throws ObsException {
        return this.setBucketLifecycle(new SetBucketLifecycleRequest(bucketName, lifecycleConfig));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketLifecycle(java.lang.String,
     * com.obs.services.model.LifecycleConfiguration)
     */
    @Override
    public HeaderResponse setBucketLifecycle(final SetBucketLifecycleRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetBucketLifecycleRequest is null");
        ServiceUtils.assertParameterNotNull(request.getLifecycleConfig(), "LifecycleConfiguration is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("setBucketLifecycleConfiguration", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.setBucketLifecycleConfigurationImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteBucketLifecycle(java.lang.String)
     */
    @Override
    public HeaderResponse deleteBucketLifecycle(final String bucketName) throws ObsException {
        return this.deleteBucketLifecycle(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteBucketLifecycle(java.lang.String)
     */
    @Override
    public HeaderResponse deleteBucketLifecycle(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("deleteBucketLifecycleConfiguration", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {
                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.deleteBucketLifecycleConfigurationImpl(request);
                    }
                });

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketPolicy(java.lang.String)
     */
    @Override
    public String getBucketPolicy(final String bucketName) throws ObsException {
        return this.getBucketPolicyV2(new BaseBucketRequest(bucketName)).getPolicy();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketPolicy(java.lang.String)
     */
    @Override
    public String getBucketPolicy(final BaseBucketRequest request) throws ObsException {
        return this.getBucketPolicyV2(request).getPolicy();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketPolicyV2(java.lang.String)
     */
    @Override
    public BucketPolicyResponse getBucketPolicyV2(final String bucketName) throws ObsException {
        return getBucketPolicyV2(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketPolicyV2(java.lang.String)
     */
    @Override
    public BucketPolicyResponse getBucketPolicyV2(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("getBucketPolicy", request.getBucketName(),
                new ActionCallbackWithResult<BucketPolicyResponse>() {
                    @Override
                    public BucketPolicyResponse action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.getBucketPolicyImpl(request);
                    }

                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketPolicy(java.lang.String,
     * java.lang.String)
     */
    @Override
    public HeaderResponse setBucketPolicy(final String bucketName, final String policy) throws ObsException {
        return setBucketPolicy(new SetBucketPolicyRequest(bucketName, policy));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketPolicy(java.lang.String,
     * java.lang.String)
     */
    @Override
    public HeaderResponse setBucketPolicy(final SetBucketPolicyRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetBucketPolicyRequest is null");
        ServiceUtils.assertParameterNotNull(request.getPolicy(), "policy is null");
        return this.doActionWithResult("setBucketPolicy", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.setBucketPolicyImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteBucketPolicy(java.lang.String)
     */
    @Override
    public HeaderResponse deleteBucketPolicy(final String bucketName) throws ObsException {
        return deleteBucketPolicy(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteBucketPolicy(java.lang.String)
     */
    @Override
    public HeaderResponse deleteBucketPolicy(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("deleteBucketPolicy", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {
                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.deleteBucketPolicyImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketWebsite(java.lang.String)
     */
    @Override
    public WebsiteConfiguration getBucketWebsite(final String bucketName) throws ObsException {
        return getBucketWebsite(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketWebsite(java.lang.String)
     */
    @Override
    public WebsiteConfiguration getBucketWebsite(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("getBucketWebsiteConfiguration", request.getBucketName(),
                new ActionCallbackWithResult<WebsiteConfiguration>() {

                    @Override
                    public WebsiteConfiguration action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.getBucketWebsiteConfigurationImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketWebsite(java.lang.String,
     * com.obs.services.model.WebsiteConfiguration)
     */
    @Override
    public HeaderResponse setBucketWebsite(final String bucketName, final WebsiteConfiguration websiteConfig)
            throws ObsException {
        return setBucketWebsite(new SetBucketWebsiteRequest(bucketName, websiteConfig));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketWebsite(java.lang.String,
     * com.obs.services.model.WebsiteConfiguration)
     */
    @Override
    public HeaderResponse setBucketWebsite(final SetBucketWebsiteRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetBucketWebsiteRequest is null");
        ServiceUtils.assertParameterNotNull(request.getWebsiteConfig(), "WebsiteConfiguration is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");

        return this.doActionWithResult("setBucketWebsiteConfiguration", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {

                        return AbstractBucketAdvanceClient.this.setBucketWebsiteConfigurationImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteBucketWebsite(java.lang.String)
     */
    @Override
    public HeaderResponse deleteBucketWebsite(final String bucketName) throws ObsException {
        return deleteBucketWebsite(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteBucketWebsite(java.lang.String)
     */
    @Override
    public HeaderResponse deleteBucketWebsite(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("deleteBucketWebsiteConfiguration", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.deleteBucketWebsiteConfigurationImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketTagging(java.lang.String)
     */
    @Override
    public BucketTagInfo getBucketTagging(final String bucketName) throws ObsException {
        return getBucketTagging(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketTagging(java.lang.String)
     */
    @Override
    public BucketTagInfo getBucketTagging(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("getBucketTagging", request.getBucketName(),
                new ActionCallbackWithResult<BucketTagInfo>() {

                    @Override
                    public BucketTagInfo action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.getBucketTaggingImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketTagging(java.lang.String,
     * com.obs.services.model.BucketTagInfo)
     */
    @Override
    public HeaderResponse setBucketTagging(final String bucketName, final BucketTagInfo bucketTagInfo)
            throws ObsException {
        return setBucketTagging(new SetBucketTaggingRequest(bucketName, bucketTagInfo));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketTagging(java.lang.String,
     * com.obs.services.model.BucketTagInfo)
     */
    @Override
    public HeaderResponse setBucketTagging(final SetBucketTaggingRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetBucketTaggingRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("setBucketTagging", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {
                    @Override
                    public HeaderResponse action() throws ServiceException {

                        return AbstractBucketAdvanceClient.this.setBucketTaggingImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteBucketTagging(java.lang.String)
     */
    @Override
    public HeaderResponse deleteBucketTagging(final String bucketName) throws ObsException {
        return deleteBucketTagging(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteBucketTagging(java.lang.String)
     */
    @Override
    public HeaderResponse deleteBucketTagging(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");

        return this.doActionWithResult("deleteBucketTagging", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.deleteBucketTaggingImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketEncryption(java.lang.String)
     */
    @Override
    public BucketEncryption getBucketEncryption(final String bucketName) throws ObsException {
        return this.getBucketEncryption(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketEncryption(java.lang.String)
     */
    @Override
    public BucketEncryption getBucketEncryption(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        return this.doActionWithResult("getBucketEncryption", request.getBucketName(),
                new ActionCallbackWithResult<BucketEncryption>() {

                    @Override
                    public BucketEncryption action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.getBucketEncryptionImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketEncryption(java.lang.String,
     * com.obs.services.model.BucketEncryption)
     */
    @Override
    public HeaderResponse setBucketEncryption(final String bucketName, final BucketEncryption bucketEncryption)
            throws ObsException {
        return this.setBucketEncryption(new SetBucketEncryptionRequest(bucketName, bucketEncryption));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketEncryption(java.lang.String,
     * com.obs.services.model.BucketEncryption)
     */
    @Override
    public HeaderResponse setBucketEncryption(final SetBucketEncryptionRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetBucketEncryptionRequest is null");
        ServiceUtils.assertParameterNotNull(request.getBucketEncryption(),
                "SetBucketEncryptionRequest.bucketEncryption is null");
        return this.doActionWithResult("setBucketEncryption", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.setBucketEncryptionImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteBucketEncryption(java.lang.String)
     */
    @Override
    public HeaderResponse deleteBucketEncryption(final String bucketName) throws ObsException {
        return this.deleteBucketEncryption(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteBucketEncryption(java.lang.String)
     */
    @Override
    public HeaderResponse deleteBucketEncryption(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        return this.doActionWithResult("deleteBucketEncryption", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {
                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.deleteBucketEncryptionImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketReplication(java.lang.String,
     * com.obs.services.model.ReplicationConfiguration)
     */
    @Override
    public HeaderResponse setBucketReplication(final String bucketName,
            final ReplicationConfiguration replicationConfiguration) throws ObsException {
        return setBucketReplication(new SetBucketReplicationRequest(bucketName, replicationConfiguration));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketReplication(java.lang.String,
     * com.obs.services.model.ReplicationConfiguration)
     */
    @Override
    public HeaderResponse setBucketReplication(final SetBucketReplicationRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetBucketReplicationRequest is null");
        ServiceUtils.assertParameterNotNull(request.getReplicationConfiguration(), "ReplicationConfiguration is null");
        ServiceUtils.assertParameterNotNull(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("setBucketReplication", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.setBucketReplicationConfigurationImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketReplication(java.lang.String)
     */
    @Override
    public ReplicationConfiguration getBucketReplication(final String bucketName) throws ObsException {
        return getBucketReplication(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketReplication(java.lang.String)
     */
    @Override
    public ReplicationConfiguration getBucketReplication(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("getBucketReplicationConfiguration", request.getBucketName(),
                new ActionCallbackWithResult<ReplicationConfiguration>() {

                    @Override
                    public ReplicationConfiguration action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.getBucketReplicationConfigurationImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.obs.services.IObsClient#deleteBucketReplication(java.lang.String)
     */
    @Override
    public HeaderResponse deleteBucketReplication(final String bucketName) throws ObsException {
        return deleteBucketReplication(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.obs.services.IObsClient#deleteBucketReplication(java.lang.String)
     */
    @Override
    public HeaderResponse deleteBucketReplication(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");

        return this.doActionWithResult("deleteBucketReplicationConfiguration", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.deleteBucketReplicationConfigurationImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketNotification(java.lang.String)
     */
    @Override
    public BucketNotificationConfiguration getBucketNotification(final String bucketName) throws ObsException {
        return this.getBucketNotification(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketNotification(java.lang.String)
     */
    @Override
    public BucketNotificationConfiguration getBucketNotification(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        return this.doActionWithResult("getBucketNotification", request.getBucketName(),
                new ActionCallbackWithResult<BucketNotificationConfiguration>() {

                    @Override
                    public BucketNotificationConfiguration action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.getBucketNotificationConfigurationImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketNotification(java.lang.String,
     * com.obs.services.model.BucketNotificationConfiguration)
     */
    @Override
    public HeaderResponse setBucketNotification(final String bucketName,
            final BucketNotificationConfiguration bucketNotificationConfiguration) throws ObsException {
        return this
                .setBucketNotification(new SetBucketNotificationRequest(bucketName, bucketNotificationConfiguration));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketNotification(java.lang.String,
     * com.obs.services.model.BucketNotificationConfiguration)
     */
    @Override
    public HeaderResponse setBucketNotification(final SetBucketNotificationRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetBucketNotificationRequest is null");
        return this.doActionWithResult("setBucketNotification", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {
                        if (null == request.getBucketNotificationConfiguration()) {
                            request.setBucketNotificationConfiguration(new BucketNotificationConfiguration());
                        }
                        return AbstractBucketAdvanceClient.this.setBucketNotificationImpl(request);
                    }
                });
    }
    
    @Override
    public HeaderResponse setBucketFSStatus(final SetBucketFSStatusRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetBucketFileInterfaceRequest is null");
        return this.doActionWithResult("setBucketFSStatus", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.setBucketFSStatusImpl(request);
                    }
                });
    }

    @Override
    public GetBucketFSStatusResult getBucketFSStatus(final GetBucketFSStatusRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "GetBucketFSStatusRequest is null");
        return this.doActionWithResult("getBucketFSStatus", request.getBucketName(),
                new ActionCallbackWithResult<GetBucketFSStatusResult>() {

                    @Override
                    public GetBucketFSStatusResult action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.getBucketMetadataImpl(request);
                    }
                });
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketDirectColdAccess()
     */
    @Override
    public HeaderResponse setBucketDirectColdAccess(final String bucketName, final BucketDirectColdAccess access)
            throws ObsException {
        return this.setBucketDirectColdAccess(new SetBucketDirectColdAccessRequest(bucketName, access));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketDirectColdAccess()
     */
    @Override
    public HeaderResponse setBucketDirectColdAccess(final SetBucketDirectColdAccessRequest request)
            throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetBucketDirectColdAccessRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        ServiceUtils.assertParameterNotNull(request.getAccess(), "bucketDirectColdAccess is null");

        return this.doActionWithResult("setBucketDirectColdAccess", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {
                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.setBucketDirectColdAccessImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketDirectColdAccess()
     */
    @Override
    public BucketDirectColdAccess getBucketDirectColdAccess(final String bucketName) throws ObsException {
        return this.getBucketDirectColdAccess(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketDirectColdAccess()
     */
    @Override
    public BucketDirectColdAccess getBucketDirectColdAccess(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("getBucketDirectColdAccess", request.getBucketName(),
                new ActionCallbackWithResult<BucketDirectColdAccess>() {
                    @Override
                    public BucketDirectColdAccess action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.getBucketDirectColdAccessImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteBucketDirectColdAccess()
     */
    @Override
    public HeaderResponse deleteBucketDirectColdAccess(final String bucketName) throws ObsException {
        return this.deleteBucketDirectColdAccess(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteBucketDirectColdAccess()
     */
    @Override
    public HeaderResponse deleteBucketDirectColdAccess(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("deleteBucketDirectColdAccess", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {
                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketAdvanceClient.this.deleteBucketDirectColdAccessImpl(request);
                    }
                });
    }
}
