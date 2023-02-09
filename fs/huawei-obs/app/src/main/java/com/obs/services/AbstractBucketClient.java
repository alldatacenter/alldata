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
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.BaseBucketRequest;
import com.obs.services.model.BucketCors;
import com.obs.services.model.BucketCustomDomainInfo;
import com.obs.services.model.BucketLocationResponse;
import com.obs.services.model.BucketLoggingConfiguration;
import com.obs.services.model.BucketMetadataInfoRequest;
import com.obs.services.model.BucketMetadataInfoResult;
import com.obs.services.model.BucketQuota;
import com.obs.services.model.BucketStorageInfo;
import com.obs.services.model.BucketStoragePolicyConfiguration;
import com.obs.services.model.BucketVersioningConfiguration;
import com.obs.services.model.CreateBucketRequest;
import com.obs.services.model.CreateVirtualBucketRequest;
import com.obs.services.model.CreateVirtualBucketResult;
import com.obs.services.model.DeleteBucketCustomDomainRequest;
import com.obs.services.model.GetBucketCustomDomainRequest;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.ListBucketAliasResult;
import com.obs.services.model.ListBucketsRequest;
import com.obs.services.model.ListBucketsResult;
import com.obs.services.model.ObsBucket;
import com.obs.services.model.RequestPaymentConfiguration;
import com.obs.services.model.RequestPaymentEnum;
import com.obs.services.model.SetBucketAclRequest;
import com.obs.services.model.SetBucketCorsRequest;
import com.obs.services.model.SetBucketCustomDomainRequest;
import com.obs.services.model.SetBucketLoggingRequest;
import com.obs.services.model.SetBucketQuotaRequest;
import com.obs.services.model.SetBucketRequestPaymentRequest;
import com.obs.services.model.SetBucketStoragePolicyRequest;
import com.obs.services.model.SetBucketVersioningRequest;

public abstract class AbstractBucketClient extends AbstractDeprecatedBucketClient {
    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#createBucket(java.lang.String)
     */
    @Override
    public ObsBucket createBucket(String bucketName) throws ObsException {
        ObsBucket obsBucket = new ObsBucket();
        obsBucket.setBucketName(bucketName);
        return this.createBucket(obsBucket);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#createBucket(java.lang.String,
     * java.lang.String)
     */
    @Override
    public ObsBucket createBucket(String bucketName, String location) throws ObsException {
        ObsBucket obsBucket = new ObsBucket();
        obsBucket.setBucketName(bucketName);
        obsBucket.setLocation(location);
        return this.createBucket(obsBucket);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#createBucket(com.obs.services.model.
     * ObsBucket)
     */
    @Override
    public ObsBucket createBucket(final ObsBucket bucket) throws ObsException {
        CreateBucketRequest request = new CreateBucketRequest();
        request.setBucketName(bucket.getBucketName());
        request.setAcl(bucket.getAcl());
        request.setBucketStorageClass(bucket.getBucketStorageClass());
        request.setLocation(bucket.getLocation());
        return this.createBucket(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#createBucket(com.obs.services.model.
     * CreateBucketRequest)
     */
    @Override
    public ObsBucket createBucket(final CreateBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "CreateBucketRequest is null");
        return this.doActionWithResult("createBucket", request.getBucketName(),
                new ActionCallbackWithResult<ObsBucket>() {
                    @Override
                    public ObsBucket action() throws ServiceException {
                        if (isCname()) {
                            throw new ServiceException("createBucket is not allowed in customdomain mode");
                        }
                        try {
                            return AbstractBucketClient.this.createBucketImpl(request);
                        } catch (ServiceException e) {
                            if (AbstractBucketClient.this.isAuthTypeNegotiation() && e.getResponseCode() == 400
                                    && "Unsupported Authorization Type".equals(e.getErrorMessage())
                                    && AbstractBucketClient.this.getProviderCredentials()
                                    .getLocalAuthType(request.getBucketName()) == AuthTypeEnum.OBS) {
                                AbstractBucketClient.this.getProviderCredentials()
                                        .setLocalAuthType(request.getBucketName(), AuthTypeEnum.V2);
                                return AbstractBucketClient.this.createBucketImpl(request);
                            } else {
                                throw e;
                            }
                        }
                    }

                    @Override
                    void authTypeNegotiate(String bucketName) throws ServiceException {
                        AuthTypeEnum authTypeEnum = AbstractBucketClient.this.getProviderCredentials()
                                .getLocalAuthType().get(bucketName);
                        if (authTypeEnum == null) {
                            authTypeEnum = AbstractBucketClient.this.getApiVersion("");
                            AbstractBucketClient.this.getProviderCredentials()
                                    .setLocalAuthType(bucketName, authTypeEnum);
                        }
                    }
                });
    }

    public CreateVirtualBucketResult createVirtualBucket(CreateVirtualBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "CreateVirtualBucketRequest is null");
        ServiceUtils.assertParameterNotNull(request.getBucketAlias(), "bucket alias is null");
        ServiceUtils.assertParameterNotNull(request.getBucketName1(), "bucket name1 is null");
        ServiceUtils.assertParameterNotNull(request.getBucketName2(), "bucket name2 is null");
        ServiceUtils.assertParameterNotNull(request.getAgencyId(), "agency id is null");
        ServiceUtils.assertParameterNotNull(request.getRegionId(), "region id is null");
        ServiceUtils.assertParameterNotNull(request.getToken(), "token is null");
        return this.doActionWithResult("createVirtualBucket", "All Buckets", new ActionCallbackWithResult<CreateVirtualBucketResult>() {
            @Override
            public CreateVirtualBucketResult action() throws ServiceException {
                if (isCname()) {
                    throw new ServiceException("createVirtualBucket is not allowed in customdomain mode");
                }
                return AbstractBucketClient.this.createVirtualBucketImpl(request);
            }

            @Override
            void authTypeNegotiate(String bucketName) throws ServiceException {
                AuthTypeEnum authTypeEnum = AbstractBucketClient.this.getProviderCredentials()
                        .getLocalAuthType().get(bucketName);
                if (authTypeEnum == null) {
                    authTypeEnum = AbstractBucketClient.this.getApiVersion("");
                    AbstractBucketClient.this.getProviderCredentials().setLocalAuthType(bucketName, authTypeEnum);
                }
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#listBuckets(com.obs.services.model.
     * ListBucketsRequest)
     */
    @Override
    public List<ObsBucket> listBuckets(final ListBucketsRequest request) throws ObsException {
        return this.listBucketsV2(request).getBuckets();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#listBucketsV2(com.obs.services.model.
     * ListBucketsRequest)
     */
    @Override
    public ListBucketsResult listBucketsV2(final ListBucketsRequest request) throws ObsException {
        return this.doActionWithResult("listBuckets", "All Buckets", new ActionCallbackWithResult<ListBucketsResult>() {
            @Override
            public ListBucketsResult action() throws ServiceException {
                if (isCname()) {
                    throw new ServiceException("listBuckets is not allowed in customdomain mode");
                }
                return AbstractBucketClient.this.listAllBucketsImpl(request);
            }

            @Override
            void authTypeNegotiate(String bucketName) throws ServiceException {
                AuthTypeEnum authTypeEnum = AbstractBucketClient.this.getProviderCredentials()
                        .getLocalAuthType().get(bucketName);
                if (authTypeEnum == null) {
                    authTypeEnum = AbstractBucketClient.this.getApiVersion("");
                    AbstractBucketClient.this.getProviderCredentials().setLocalAuthType(bucketName, authTypeEnum);
                }
            }
        });
    }

    @Override
    public ListBucketAliasResult listAliasBuckets() throws ObsException {
        return this.doActionWithResult("ListBucketAliasResult", "All Buckets", new ActionCallbackWithResult<ListBucketAliasResult>() {
            @Override
            public ListBucketAliasResult action() throws ServiceException {
                return AbstractBucketClient.this.listAliasBucketsImpl();
            }

            @Override
            void authTypeNegotiate(String bucketName) throws ServiceException {
                AuthTypeEnum authTypeEnum = AbstractBucketClient.this.getProviderCredentials()
                        .getLocalAuthType().get(bucketName);
                if (authTypeEnum == null) {
                    authTypeEnum = AbstractBucketClient.this.getApiVersion("");
                    AbstractBucketClient.this.getProviderCredentials().setLocalAuthType(bucketName, authTypeEnum);
                }
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#headBucket(java.lang.String)
     */
    @Override
    public boolean headBucket(final String bucketName) throws ObsException {
        return headBucket(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#headBucket(java.lang.String)
     */
    @Override
    public boolean headBucket(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("headBucket", request.getBucketName(), new ActionCallbackWithResult<Boolean>() {

            @Override
            public Boolean action() throws ServiceException {
                return AbstractBucketClient.this.headBucketImpl(request);
            }

            @Override
            void authTypeNegotiate(String bucketName) throws ServiceException {
                try {
                    AuthTypeEnum authTypeEnum = AbstractBucketClient.this.getProviderCredentials()
                            .getLocalAuthType().get(bucketName);
                    if (authTypeEnum == null) {
                        authTypeEnum = AbstractBucketClient.this.getApiVersion(bucketName);
                        AbstractBucketClient.this.getProviderCredentials().setLocalAuthType(bucketName, authTypeEnum);
                    }
                } catch (ServiceException e) {
                    if (e.getResponseCode() != 404) {
                        throw e;
                    }
                }
            }
        });
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteBucket(java.lang.String)
     */
    @Override
    public HeaderResponse deleteBucket(final String bucketName) throws ObsException {
        return deleteBucket(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteBucket(java.lang.String)
     */
    @Override
    public HeaderResponse deleteBucket(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("deleteBucket", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketClient.this.deleteBucketImpl(request);
                    }
                });
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.obs.services.IObsClient#getBucketMetadata(com.obs.services.model.
     * BucketMetadataInfoRequest)
     */
    @Override
    public BucketMetadataInfoResult getBucketMetadata(final BucketMetadataInfoRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BucketMetadataInfoRequest is null");
        return this.doActionWithResult("getBucketMetadata", request.getBucketName(),
                new ActionCallbackWithResult<BucketMetadataInfoResult>() {
                    @Override
                    public BucketMetadataInfoResult action() throws ServiceException {
                        return AbstractBucketClient.this.getBucketMetadataImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketAcl(java.lang.String)
     */
    @Override
    public AccessControlList getBucketAcl(final String bucketName) throws ObsException {
        return getBucketAcl(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketAcl(java.lang.String)
     */
    @Override
    public AccessControlList getBucketAcl(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("getBucketAcl", request.getBucketName(),
                new ActionCallbackWithResult<AccessControlList>() {

                    @Override
                    public AccessControlList action() throws ServiceException {
                        return AbstractBucketClient.this.getBucketAclImpl(request);
                    }

                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketAcl(java.lang.String,
     * com.obs.services.model.AccessControlList)
     */
    @Override
    public HeaderResponse setBucketAcl(final String bucketName, final AccessControlList acl) throws ObsException {
        return setBucketAcl(new SetBucketAclRequest(bucketName, acl));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketAcl(java.lang.String,
     * com.obs.services.model.AccessControlList)
     */
    @Override
    public HeaderResponse setBucketAcl(final SetBucketAclRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetBucketAclRequest is null");
        return this.doActionWithResult("setBucketAcl", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {
                        if (request.getAcl() == null && null == request.getCannedACL()) {
                            throw new IllegalArgumentException("Both CannedACL and AccessControlList is null");
                        }
                        return AbstractBucketClient.this.setBucketAclImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketLocation(java.lang.String)
     */
    @Override
    public String getBucketLocation(final String bucketName) throws ObsException {
        return this.getBucketLocation(new BaseBucketRequest(bucketName)).getLocation();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketLocationV2(java.lang.String)
     */
    @Override
    public BucketLocationResponse getBucketLocation(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        return this.doActionWithResult("getBucketLocation", request.getBucketName(),
                new ActionCallbackWithResult<BucketLocationResponse>() {
                    @Override
                    public BucketLocationResponse action() throws ServiceException {
                        return AbstractBucketClient.this.getBucketLocationImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketLocationV2(java.lang.String)
     */
    @Override
    public BucketLocationResponse getBucketLocationV2(final String bucketName) throws ObsException {
        return this.getBucketLocation(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketStorageInfo(java.lang.String)
     */
    @Override
    public BucketStorageInfo getBucketStorageInfo(final String bucketName) throws ObsException {
        return this.getBucketStorageInfo(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketStorageInfo(java.lang.String)
     */
    @Override
    public BucketStorageInfo getBucketStorageInfo(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("getBucketStorageInfo", request.getBucketName(),
                new ActionCallbackWithResult<BucketStorageInfo>() {

                    @Override
                    public BucketStorageInfo action() throws ServiceException {
                        return AbstractBucketClient.this.getBucketStorageInfoImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketQuota(java.lang.String)
     */
    @Override
    public BucketQuota getBucketQuota(final String bucketName) throws ObsException {
        return this.getBucketQuota(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketQuota(java.lang.String)
     */
    @Override
    public BucketQuota getBucketQuota(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        return this.doActionWithResult("getBucketQuota", request.getBucketName(),
                new ActionCallbackWithResult<BucketQuota>() {

                    @Override
                    public BucketQuota action() throws ServiceException {
                        return AbstractBucketClient.this.getBucketQuotaImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketQuota(java.lang.String,
     * com.obs.services.model.BucketQuota)
     */
    @Override
    public HeaderResponse setBucketQuota(final String bucketName, final BucketQuota bucketQuota) throws ObsException {
        return this.setBucketQuota(new SetBucketQuotaRequest(bucketName, bucketQuota));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketQuota(java.lang.String,
     * com.obs.services.model.BucketQuota)
     */
    @Override
    public HeaderResponse setBucketQuota(final SetBucketQuotaRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetBucketQuotaRequest is null");
        ServiceUtils.assertParameterNotNull(request.getBucketQuota(),
                "The bucket '" + request.getBucketName() + "' does not include Quota information");
        return this.doActionWithResult("setBucketQuota", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketClient.this.setBucketQuotaImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketStoragePolicy(java.lang.String)
     */
    @Override
    public BucketStoragePolicyConfiguration getBucketStoragePolicy(final String bucketName) throws ObsException {
        return this.getBucketStoragePolicy(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketStoragePolicy(java.lang.String)
     */
    @Override
    public BucketStoragePolicyConfiguration getBucketStoragePolicy(final BaseBucketRequest request)
            throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        return this.doActionWithResult("getBucketStoragePolicy", request.getBucketName(),
                new ActionCallbackWithResult<BucketStoragePolicyConfiguration>() {

                    @Override
                    public BucketStoragePolicyConfiguration action() throws ServiceException {
                        return AbstractBucketClient.this.getBucketStoragePolicyImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketStoragePolicy(java.lang.String,
     * com.obs.services.model.BucketStoragePolicyConfiguration)
     */
    @Override
    public HeaderResponse setBucketStoragePolicy(final String bucketName,
            final BucketStoragePolicyConfiguration bucketStorage) throws ObsException {
        return this.setBucketStoragePolicy(new SetBucketStoragePolicyRequest(bucketName, bucketStorage));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketStoragePolicy(java.lang.String,
     * com.obs.services.model.BucketStoragePolicyConfiguration)
     */
    @Override
    public HeaderResponse setBucketStoragePolicy(final SetBucketStoragePolicyRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetBucketStoragePolicyRequest is null");
        ServiceUtils.assertParameterNotNull(request.getBucketStorage(),
                "The bucket '" + request.getBucketName() + "' does not include storagePolicy information");

        return this.doActionWithResult("setBucketStoragePolicy", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketClient.this.setBucketStorageImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketCors(java.lang.String,
     * com.obs.services.model.BucketCors)
     */
    @Override
    public HeaderResponse setBucketCors(final String bucketName, final BucketCors bucketCors) throws ObsException {
        return this.setBucketCors(new SetBucketCorsRequest(bucketName, bucketCors));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketCors(java.lang.String,
     * com.obs.services.model.BucketCors)
     */
    @Override
    public HeaderResponse setBucketCors(final SetBucketCorsRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetBucketCorsRequest is null");
        return this.doActionWithResult("setBucketCors", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketClient.this.setBucketCorsImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketCors(java.lang.String)
     */
    @Override
    public BucketCors getBucketCors(final String bucketName) throws ObsException {
        return this.getBucketCors(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketCors(java.lang.String)
     */
    @Override
    public BucketCors getBucketCors(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        return this.doActionWithResult("getBucketCors", request.getBucketName(),
                new ActionCallbackWithResult<BucketCors>() {

                    @Override
                    public BucketCors action() throws ServiceException {
                        return AbstractBucketClient.this.getBucketCorsImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteBucketCors(java.lang.String)
     */
    @Override
    public HeaderResponse deleteBucketCors(final String bucketName) throws ObsException {
        return this.deleteBucketCors(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#deleteBucketCors(java.lang.String)
     */
    @Override
    public HeaderResponse deleteBucketCors(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        return this.doActionWithResult("deleteBucketCors", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketClient.this.deleteBucketCorsImpl(request);
                    }
                });
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketLogging(java.lang.String)
     */
    @Override
    public BucketLoggingConfiguration getBucketLogging(final String bucketName) throws ObsException {
        return getBucketLogging(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketLogging(java.lang.String)
     */
    @Override
    public BucketLoggingConfiguration getBucketLogging(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        return this.doActionWithResult("getBucketLoggingConfiguration", request.getBucketName(),
                new ActionCallbackWithResult<BucketLoggingConfiguration>() {
                    @Override
                    public BucketLoggingConfiguration action() throws ServiceException {
                        return AbstractBucketClient.this.getBucketLoggingConfigurationImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketLoggingConfiguration(java.lang.
     * String, com.obs.services.model.BucketLoggingConfiguration, boolean)
     */
    @Override
    public HeaderResponse setBucketLoggingConfiguration(final String bucketName,
            final BucketLoggingConfiguration loggingConfiguration, final boolean updateTargetACLifRequired)
                    throws ObsException {
        return this.setBucketLogging(
                new SetBucketLoggingRequest(bucketName, loggingConfiguration, updateTargetACLifRequired));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketLogging(java.lang.String,
     * com.obs.services.model.BucketLoggingConfiguration)
     */
    @Override
    public HeaderResponse setBucketLogging(final String bucketName,
            final BucketLoggingConfiguration loggingConfiguration) throws ObsException {
        return this.setBucketLogging(new SetBucketLoggingRequest(bucketName, loggingConfiguration));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketLogging(java.lang.String,
     * com.obs.services.model.BucketLoggingConfiguration)
     */
    @Override
    public HeaderResponse setBucketLogging(final SetBucketLoggingRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetBucketLoggingRequest is null");
        return this.doActionWithResult("setBucketLoggingConfiguration", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {

                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketClient.this.setBucketLoggingConfigurationImpl(request);
                    }
                });
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketVersioning(java.lang.String,
     * com.obs.services.model.BucketVersioningConfiguration)
     */
    @Override
    public HeaderResponse setBucketVersioning(final String bucketName,
            final BucketVersioningConfiguration versioningConfiguration) throws ObsException {
        return setBucketVersioning(
                new SetBucketVersioningRequest(bucketName, versioningConfiguration.getVersioningStatus()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketVersioning(java.lang.String,
     * com.obs.services.model.BucketVersioningConfiguration)
     */
    @Override
    public HeaderResponse setBucketVersioning(final SetBucketVersioningRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetBucketVersioningRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");

        return this.doActionWithResult("setBucketVersioning", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {
                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketClient.this.setBucketVersioningImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketVersioning(java.lang.String)
     */
    @Override
    public BucketVersioningConfiguration getBucketVersioning(final String bucketName) throws ObsException {
        return getBucketVersioning(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#getBucketVersioning(java.lang.String)
     */
    @Override
    public BucketVersioningConfiguration getBucketVersioning(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("getBucketVersioning", request.getBucketName(),
                new ActionCallbackWithResult<BucketVersioningConfiguration>() {

                    @Override
                    public BucketVersioningConfiguration action() throws ServiceException {
                        return AbstractBucketClient.this.getBucketVersioningImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#setBucketRequestPayment()
     */
    @Override
    public HeaderResponse setBucketRequestPayment(String bucketName, RequestPaymentEnum payer) throws ObsException {
        return setBucketRequestPayment(new SetBucketRequestPaymentRequest(bucketName, payer));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.obs.services.IObsClient#setBucketRequestPayment(java.lang.String,
     * com.obs.services.model.BucketVersioningConfiguration)
     */
    @Override
    public HeaderResponse setBucketRequestPayment(final SetBucketRequestPaymentRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "SetBucketRequestPaymentRequest is null");
        return this.doActionWithResult("setBucketRequestPayment", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {
                    @Override
                    public HeaderResponse action() throws ServiceException {
                        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
                        ServiceUtils.assertParameterNotNull(request.getPayer(), "payer is null");
                        return AbstractBucketClient.this.setBucketRequestPaymentImpl(request);
                    }
                });
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.obs.services.IObsClient#getBucketRequestPayment(java.lang.String)
     */
    public RequestPaymentConfiguration getBucketRequestPayment(String bucketName) throws ObsException {
        return getBucketRequestPayment(new BaseBucketRequest(bucketName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.obs.services.IObsClient#getBucketRequestPayment(java.lang.String)
     */
    @Override
    public RequestPaymentConfiguration getBucketRequestPayment(final BaseBucketRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("getBucketRequestPayment", request.getBucketName(),
                new ActionCallbackWithResult<RequestPaymentConfiguration>() {

                    @Override
                    public RequestPaymentConfiguration action() throws ServiceException {
                        return AbstractBucketClient.this.getBucketRequestPaymentImpl(request);
                    }
                });
    }
    
    @Override
    public HeaderResponse deleteBucketCustomDomain(String bucketName, String domainName) throws ObsException {
        return deleteBucketCustomDomain(new DeleteBucketCustomDomainRequest(bucketName, domainName));
    }
    
    @Override
    public HeaderResponse deleteBucketCustomDomain(DeleteBucketCustomDomainRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "request is null");
        ServiceUtils.assertParameterNotNull(request.getBucketName(), "bucketName is null");
        ServiceUtils.assertParameterNotNull2(request.getDomainName(), "domainName is null");
        
        
        return this.doActionWithResult("setBucketCustomDomain", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {
                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketClient.this.deleteBucketCustomDomainImpl(request);
                    }
                });
    }
    
    @Override
    public BucketCustomDomainInfo getBucketCustomDomain(String bucketName) throws ObsException {
        return getBucketCustomDomain(new GetBucketCustomDomainRequest(bucketName));
    }
    
    @Override
    public BucketCustomDomainInfo getBucketCustomDomain(final GetBucketCustomDomainRequest request) 
            throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "BaseBucketRequest is null");
        ServiceUtils.assertParameterNotNull2(request.getBucketName(), "bucketName is null");
        return this.doActionWithResult("getBucketCustomDomain", request.getBucketName(),
                new ActionCallbackWithResult<BucketCustomDomainInfo>() {

                    @Override
                    public BucketCustomDomainInfo action() throws ServiceException {
                        return AbstractBucketClient.this.getBucketCustomDomainImpl(request);
                    }
                });
    }
    
    @Override
    public HeaderResponse setBucketCustomDomain(String bucketName, String domainName) throws ObsException {
        return setBucketCustomDomain(new SetBucketCustomDomainRequest(bucketName, domainName));
    }
    
    @Override
    public HeaderResponse setBucketCustomDomain(SetBucketCustomDomainRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "request is null");
        ServiceUtils.assertParameterNotNull(request.getBucketName(), "bucketName is null");
        ServiceUtils.assertParameterNotNull2(request.getDomainName(), "domainName is null");

        return this.doActionWithResult("setBucketCustomDomain", request.getBucketName(),
                new ActionCallbackWithResult<HeaderResponse>() {
                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return AbstractBucketClient.this.setBucketCustomDomainImpl(request);
                    }
                });
    }
}
