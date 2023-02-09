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

package com.oef.services;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;
import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.utils.JSONChange;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.HeaderResponse;
import com.oef.services.model.CreateAsyncFetchJobsRequest;
import com.oef.services.model.CreateAsynchFetchJobsResult;
import com.oef.services.model.PutExtensionPolicyRequest;
import com.oef.services.model.QueryAsynchFetchJobsResult;
import com.oef.services.model.QueryExtensionPolicyResult;

/**
 * OEF client
 */
public class OefClient extends ObsClient implements IOefClient {

    private static final ILogger ILOG = LoggerBuilder.getLogger(OefClient.class);

    /**
     * Constructor
     * 
     * @param endPoint
     *            OEF service address
     * 
     */
    public OefClient(String endPoint) {
        super(endPoint);
    }

    /**
     * Constructor
     * 
     * @param config
     *            Configuration parameters of OEF client
     * 
     */
    public OefClient(ObsConfiguration config) {
        super(config);
    }

    /**
     * Constructor
     * 
     * @param accessKey
     *            Access key ID
     * @param secretKey
     *            Secret access key
     * @param endPoint
     *            OEF service address
     * 
     */
    public OefClient(String accessKey, String secretKey, String endPoint) {
        super(accessKey, secretKey, endPoint);
    }

    /**
     * Constructor
     * 
     * @param accessKey
     *            Access key ID
     * @param secretKey
     *            Secret access key
     * @param config
     *            Configuration parameters of OEF client
     * 
     */
    public OefClient(String accessKey, String secretKey, ObsConfiguration config) {
        super(accessKey, secretKey, config);
    }

    /**
     * Constructor
     * 
     * @param accessKey
     *            AK in the temporary access keys
     * @param secretKey
     *            SK in the temporary access keys
     * @param securityToken
     *            Security token
     * @param endPoint
     *            OEF service address
     * 
     */
    public OefClient(String accessKey, String secretKey, String securityToken, String endPoint) {
        super(accessKey, secretKey, securityToken, endPoint);
    }

    /**
     * Constructor
     * 
     * @param accessKey
     *            AK in the temporary access keys
     * @param secretKey
     *            SK in the temporary access keys
     * @param securityToken
     *            Security Token
     * @param config
     *            Configuration parameters of OEF client
     * 
     */
    public OefClient(String accessKey, String secretKey, String securityToken, ObsConfiguration config) {
        super(accessKey, secretKey, securityToken, config);
    }

    @Override
    public HeaderResponse putExtensionPolicy(final String bucketName, final PutExtensionPolicyRequest request)
            throws ObsException {
        ServiceUtils.assertParameterNotNull(bucketName, "bucket is null");
        ServiceUtils.assertParameterNotNull(request, "policy is null");
        if (null == request.getCompress() && null == request.getFetch() && null == request.getTranscode()) {
            throw new IllegalArgumentException(
                    "putExtensionPolicy failed: compress, fetch and transcode cannot be empty at the same time");
        }
        return this.doActionWithResult("putExtensionPolicy", bucketName,
                new ActionCallbackWithResult<HeaderResponse>() {
                    @Override
                    public HeaderResponse action() throws ServiceException {
                        String policy = JSONChange.objToJson(request);
                        return OefClient.this.setExtensionPolicyImpl(bucketName, policy);
                    }
                });
    }

    @Override
    public QueryExtensionPolicyResult queryExtensionPolicy(final String bucketName) throws ObsException {
        ServiceUtils.assertParameterNotNull(bucketName, "bucket is null");
        return this.doActionWithResult("queryExtensionPolicy", bucketName,
                new ActionCallbackWithResult<QueryExtensionPolicyResult>() {
                    @Override
                    public QueryExtensionPolicyResult action() throws ServiceException {
                        return OefClient.this.queryExtensionPolicyImpl(bucketName);

                    }
                });
    }

    @Override
    public HeaderResponse deleteExtensionPolicy(final String bucketName) throws ObsException {
        ServiceUtils.assertParameterNotNull(bucketName, "bucket is null");
        return this.doActionWithResult("deleteExtensionPolicy", bucketName,
                new ActionCallbackWithResult<HeaderResponse>() {
                    @Override
                    public HeaderResponse action() throws ServiceException {
                        return OefClient.this.deleteExtensionPolicyImpl(bucketName);
                    }
                });
    }

    @Override
    public CreateAsynchFetchJobsResult createFetchJob(final CreateAsyncFetchJobsRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request.getBucketName(), "bucket is null");
        ServiceUtils.assertParameterNotNull(request, "policy is null");
        ServiceUtils.assertParameterNotNull(request.getUrl(), "url is null");
        if (request.getCallBackUrl() != null) {
            ServiceUtils.assertParameterNotNull(request.getCallBackBody(),
                    "callbackbody is null when callbackurl is not null");
        }
        return this.doActionWithResult("CreateFetchJob", request.getBucketName(),
                new ActionCallbackWithResult<CreateAsynchFetchJobsResult>() {
                    @Override
                    public CreateAsynchFetchJobsResult action() throws ServiceException {
                        String policy = JSONChange.objToJson(request);
                        return OefClient.this.createFetchJobImpl(request.getBucketName(), policy);

                    }
                });
    }

    @Override
    public QueryAsynchFetchJobsResult queryFetchJob(final String bucketName, final String jobId) throws ObsException {
        ServiceUtils.assertParameterNotNull(bucketName, "bucket is null");
        ServiceUtils.assertParameterNotNull(jobId, "jobId is null");
        return this.doActionWithResult("queryFetchJob", bucketName,
                new ActionCallbackWithResult<QueryAsynchFetchJobsResult>() {
                    @Override
                    public QueryAsynchFetchJobsResult action() throws ServiceException {
                        return OefClient.this.queryFetchJobImpl(bucketName, jobId);

                    }
                });
    }
}
