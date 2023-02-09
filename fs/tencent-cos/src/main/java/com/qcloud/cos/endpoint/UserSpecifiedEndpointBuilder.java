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
 * user specified endpoint builder
 *
 */
public class UserSpecifiedEndpointBuilder implements EndpointBuilder {

    private String generalApiEndpoint;
    private String getServiceApiEndpoint;
    
    public UserSpecifiedEndpointBuilder(String generalApiEndpoint, String getServiceApiEndpoint) {
        super();
        if (null == generalApiEndpoint  || null == getServiceApiEndpoint) {
            throw new IllegalArgumentException("endpoint must not be null");
        }
        this.generalApiEndpoint = generalApiEndpoint;
        this.getServiceApiEndpoint = getServiceApiEndpoint;
    }

    @Override
    public String buildGeneralApiEndpoint(String bucketName) {
        return generalApiEndpoint;
    }

    @Override
    public String buildGetServiceApiEndpoint() {
        return getServiceApiEndpoint;
    }
    
}
