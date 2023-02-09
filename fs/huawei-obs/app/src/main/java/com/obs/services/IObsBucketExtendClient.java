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
import com.obs.services.model.BucketCustomDomainInfo;
import com.obs.services.model.DeleteBucketCustomDomainRequest;
import com.obs.services.model.GetBucketCustomDomainRequest;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.SetBucketCustomDomainRequest;

/**
 * 
 * 
 * @since 3.21.8
 */
public interface IObsBucketExtendClient {
    /**
     * Deleting the Custom Domain Name of a Bucket
     * @param bucketName  Bucket name
     * @param domainName  Custom Domain Name
     * @return
     * @throws ObsException
     * @since 3.21.8
     */
    HeaderResponse deleteBucketCustomDomain(String bucketName, String domainName) throws ObsException;
    
    /**
     * Deleting the Custom Domain Name of a Bucket
     * @param request
     * @return
     * @throws ObsException
     * @since 3.21.8
     */
    HeaderResponse deleteBucketCustomDomain(DeleteBucketCustomDomainRequest request) throws ObsException;
    
    /**
     * Obtaining the Custom Domain Name of a Bucket
     * @param bucketName Bucket name
     * @return
     * @throws ObsException
     * @since 3.21.8
     */
    BucketCustomDomainInfo getBucketCustomDomain(String bucketName) throws ObsException;
    
    /**
     * Obtaining the Custom Domain Name of a Bucket
     * @param request
     * @return
     * @throws ObsException
     * @since 3.21.8
     */
    BucketCustomDomainInfo getBucketCustomDomain(GetBucketCustomDomainRequest request) throws ObsException;
    
    /**
     * Configuring a Custom Domain Name for a Bucket
     * @param bucketName Bucket name
     * @param domainName Custom Domain Name
     * @return
     * @throws ObsException
     * @since 3.21.8
     */
    HeaderResponse setBucketCustomDomain(String bucketName, String domainName) throws ObsException;
    
    /**
     * Configuring a Custom Domain Name for a Bucket
     * @param request
     * @return
     * @throws ObsException
     * @since 3.21.8
     */
    HeaderResponse setBucketCustomDomain(SetBucketCustomDomainRequest request) throws ObsException;
}
