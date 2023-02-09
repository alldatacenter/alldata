/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.endpoint;


/**
 * get the endpoint, the endpoint will be used by host header and EndpointResolver.
 */

public interface EndpointBuilder {  
    /**
     * build endpoint endpoint, the endpoint is used by bucket and objects api. except the get service api.
     * @param bucketName the bucketName should contain the appid. for example. movie-12510000.
     * @return the endpoint.
     */
    public String buildGeneralApiEndpoint(String bucketName);
    
    /**
     * the endpoint is used by  Get Service Api.
     */
    public String buildGetServiceApiEndpoint();
}
