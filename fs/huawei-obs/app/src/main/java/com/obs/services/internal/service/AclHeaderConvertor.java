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


package com.obs.services.internal.service;

import java.util.HashMap;
import java.util.Map;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;
import com.obs.services.internal.Constants;
import com.obs.services.internal.ObsConstraint;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.trans.NewTransResult;
import com.obs.services.internal.utils.Mimetypes;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.HttpMethodEnum;
import com.obs.services.model.SpecialParamEnum;

public abstract class AclHeaderConvertor extends AbstractRequestConvertor {
    private static final ILogger log = LoggerBuilder.getLogger("com.obs.services.ObsClient");
    
    boolean prepareRESTHeaderAclForV2(String bucketName, Map<String, String> metadata, AccessControlList acl) {
        String restHeaderAclValue = null;
        if (acl == AccessControlList.REST_CANNED_PRIVATE) {
            restHeaderAclValue = Constants.ACL_PRIVATE;
        } else if (acl == AccessControlList.REST_CANNED_PUBLIC_READ) {
            restHeaderAclValue = Constants.ACL_PUBLIC_READ;
        } else if (acl == AccessControlList.REST_CANNED_PUBLIC_READ_WRITE) {
            restHeaderAclValue = Constants.ACL_PUBLIC_READ_WRITE;
        } else if (acl == AccessControlList.REST_CANNED_PUBLIC_READ_DELIVERED) {
            restHeaderAclValue = Constants.ACL_PUBLIC_READ;
        } else if (acl == AccessControlList.REST_CANNED_PUBLIC_READ_WRITE_DELIVERED) {
            restHeaderAclValue = Constants.ACL_PUBLIC_READ_WRITE;
        } else if (acl == AccessControlList.REST_CANNED_AUTHENTICATED_READ) {
            restHeaderAclValue = Constants.ACL_AUTHENTICATED_READ;
        } else if (acl == AccessControlList.REST_CANNED_BUCKET_OWNER_READ) {
            restHeaderAclValue = Constants.ACL_BUCKET_OWNER_READ;
        } else if (acl == AccessControlList.REST_CANNED_BUCKET_OWNER_FULL_CONTROL) {
            restHeaderAclValue = Constants.ACL_BUCKET_OWNER_FULL_CONTROL;
        } else if (acl == AccessControlList.REST_CANNED_LOG_DELIVERY_WRITE) {
            restHeaderAclValue = Constants.ACL_LOG_DELIVERY_WRITE;
        }
        String aclHeader = this.getIHeaders(bucketName).aclHeader();
        if (restHeaderAclValue != null) {
            metadata.put(aclHeader, restHeaderAclValue);
        }
        return metadata.containsKey(aclHeader);
    }
    
    boolean prepareRESTHeaderAclForOBS(String bucketName, Map<String, String> metadata,
                                       AccessControlList acl) throws ServiceException {
        String restHeaderAclValue = null;
        boolean invalid = false;
        if (acl == AccessControlList.REST_CANNED_PRIVATE) {
            restHeaderAclValue = Constants.ACL_PRIVATE;
        } else if (acl == AccessControlList.REST_CANNED_PUBLIC_READ) {
            restHeaderAclValue = Constants.ACL_PUBLIC_READ;
        } else if (acl == AccessControlList.REST_CANNED_PUBLIC_READ_WRITE) {
            restHeaderAclValue = Constants.ACL_PUBLIC_READ_WRITE;
        } else if (acl == AccessControlList.REST_CANNED_PUBLIC_READ_DELIVERED) {
            restHeaderAclValue = Constants.ACL_PUBLIC_READ_DELIVERED;
        } else if (acl == AccessControlList.REST_CANNED_PUBLIC_READ_WRITE_DELIVERED) {
            restHeaderAclValue = Constants.ACL_PUBLIC_READ_WRITE_DELIVERED;
        } else if (acl == AccessControlList.REST_CANNED_AUTHENTICATED_READ) {
            restHeaderAclValue = Constants.ACL_AUTHENTICATED_READ;
            invalid = true;
        } else if (acl == AccessControlList.REST_CANNED_BUCKET_OWNER_READ) {
            restHeaderAclValue = Constants.ACL_BUCKET_OWNER_READ;
            invalid = true;
        } else if (acl == AccessControlList.REST_CANNED_BUCKET_OWNER_FULL_CONTROL) {
            restHeaderAclValue = Constants.ACL_BUCKET_OWNER_FULL_CONTROL;
            invalid = true;
        } else if (acl == AccessControlList.REST_CANNED_LOG_DELIVERY_WRITE) {
            restHeaderAclValue = Constants.ACL_LOG_DELIVERY_WRITE;
            invalid = true;
        }
        if (invalid) {
            log.info("Invalid Canned ACL:" + restHeaderAclValue);
        }

        String aclHeader = this.getIHeaders(bucketName).aclHeader();
        if (restHeaderAclValue != null) {
            metadata.put(aclHeader, restHeaderAclValue);
        }
        return metadata.containsKey(aclHeader);
    }

    protected boolean prepareRESTHeaderAclObject(String bucketName, Map<String, String> metadata, AccessControlList acl)
            throws ServiceException {
        return this.getProviderCredentials().getLocalAuthType(bucketName) == AuthTypeEnum.OBS
                ? this.prepareRESTHeaderAclForOBSObject(bucketName, metadata, acl)
                : this.prepareRESTHeaderAclForV2(bucketName, metadata, acl);
    }

    boolean prepareRESTHeaderAclForOBSObject(String bucketName, Map<String, String> metadata, AccessControlList acl)
            throws ServiceException {
        String restHeaderAclValue = null;
        boolean invalid = false;
        if (acl == AccessControlList.REST_CANNED_PRIVATE) {
            restHeaderAclValue = Constants.ACL_PRIVATE;
        } else if (acl == AccessControlList.REST_CANNED_PUBLIC_READ) {
            restHeaderAclValue = Constants.ACL_PUBLIC_READ;
        } else if (acl == AccessControlList.REST_CANNED_PUBLIC_READ_WRITE) {
            restHeaderAclValue = Constants.ACL_PUBLIC_READ_WRITE;
        } else if (acl == AccessControlList.REST_CANNED_PUBLIC_READ_DELIVERED) {
            restHeaderAclValue = Constants.ACL_PUBLIC_READ;
        } else if (acl == AccessControlList.REST_CANNED_PUBLIC_READ_WRITE_DELIVERED) {
            restHeaderAclValue = Constants.ACL_PUBLIC_READ_WRITE;
        } else if (acl == AccessControlList.REST_CANNED_AUTHENTICATED_READ) {
            restHeaderAclValue = Constants.ACL_AUTHENTICATED_READ;
            invalid = true;
        } else if (acl == AccessControlList.REST_CANNED_BUCKET_OWNER_READ) {
            restHeaderAclValue = Constants.ACL_BUCKET_OWNER_READ;
            invalid = true;
        } else if (acl == AccessControlList.REST_CANNED_BUCKET_OWNER_FULL_CONTROL) {
            restHeaderAclValue = Constants.ACL_BUCKET_OWNER_FULL_CONTROL;
            invalid = true;
        } else if (acl == AccessControlList.REST_CANNED_LOG_DELIVERY_WRITE) {
            restHeaderAclValue = Constants.ACL_LOG_DELIVERY_WRITE;
            invalid = true;
        }
        if (invalid) {
            log.info("Invalid Canned ACL:" + restHeaderAclValue);
        }

        String aclHeader = this.getIHeaders(bucketName).aclHeader();
        if (restHeaderAclValue != null) {
            metadata.put(aclHeader, restHeaderAclValue);
        }
        return metadata.containsKey(aclHeader);
    }

    protected boolean prepareRESTHeaderAcl(String bucketName, Map<String, String> metadata, AccessControlList acl)
            throws ServiceException {
        return this.getProviderCredentials().getLocalAuthType(bucketName) == AuthTypeEnum.OBS
                ? this.prepareRESTHeaderAclForOBS(bucketName, metadata, acl)
                : this.prepareRESTHeaderAclForV2(bucketName, metadata, acl);
    }
    
    protected String getCredential(String shortDate, String accessKey) {
        return new StringBuilder(accessKey).append("/").append(shortDate).append("/")
                .append(ObsConstraint.DEFAULT_BUCKET_LOCATION_VALUE).append("/").append(Constants.SERVICE).append("/")
                .append(Constants.REQUEST_TAG).toString();
    }
    
    /**
     * @param bucketName
     * @param objectKey
     * @param acl
     * @param versionId
     * @param isRequesterPays
     * @throws ServiceException
     */
    protected void putAclImpl(String bucketName, String objectKey, AccessControlList acl, String versionId,
            boolean isRequesterPays) throws ServiceException {
        if (acl != null) {
            Map<String, String> requestParams = new HashMap<>();
            requestParams.put(SpecialParamEnum.ACL.getOriginalStringCode(), "");
            if (versionId != null) {
                requestParams.put(Constants.ObsRequestParams.VERSION_ID, versionId);
            }

            Map<String, String> headers = new HashMap<>();
            headers.put(Constants.CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);
            String xml = this.getIConvertor(bucketName).transAccessControlList(acl, !ServiceUtils.isValid(objectKey));
            headers.put(Constants.CommonHeaders.CONTENT_LENGTH, String.valueOf(xml.length()));
            headers.put(Constants.CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));
            transRequestPaymentHeaders(isRequesterPays, headers, this.getIHeaders(bucketName));
            NewTransResult result = new NewTransResult();
            result.setHttpMethod(HttpMethodEnum.PUT);
            result.setBucketName(bucketName);
            result.setObjectKey(objectKey);
            result.setHeaders(headers);
            result.setParams(requestParams);
            result.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));
            performRequest(result);
        }
    }
}
