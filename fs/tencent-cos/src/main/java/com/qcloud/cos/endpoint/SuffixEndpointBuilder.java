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

import com.qcloud.cos.internal.BucketNameUtils;
import com.qcloud.cos.internal.UrlComponentUtils;

public class SuffixEndpointBuilder implements EndpointBuilder {

    private String endpointSuffix;

    public SuffixEndpointBuilder(String endpointSuffix) {
        super();
        if (endpointSuffix == null) {
            throw new IllegalArgumentException("endpointSuffix must not be null");
        }
        while (endpointSuffix.startsWith(".")) {
            endpointSuffix = endpointSuffix.substring(1);
        }
        UrlComponentUtils.validateEndPointSuffix(endpointSuffix);
        this.endpointSuffix = endpointSuffix.trim();
    }

    @Override
    public String buildGeneralApiEndpoint(String bucketName) {
        BucketNameUtils.validateBucketName(bucketName);
        return String.format("%s.%s", bucketName, this.endpointSuffix);
    }

    @Override
    public String buildGetServiceApiEndpoint() {
        return this.endpointSuffix;
    }


}
