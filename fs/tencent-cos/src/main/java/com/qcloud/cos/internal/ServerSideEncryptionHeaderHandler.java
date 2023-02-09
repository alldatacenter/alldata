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


package com.qcloud.cos.internal;

import com.qcloud.cos.Headers;
import com.qcloud.cos.http.CosHttpResponse;

/**
 * Base request handler for responses that include a server-side encryption
 * header
 */
public class ServerSideEncryptionHeaderHandler <T extends ServerSideEncryptionResult> implements HeaderHandler<T>{

    @Override
    public void handle(T result, CosHttpResponse response) {
        result.setSSEAlgorithm(response.getHeaders().get(Headers.SERVER_SIDE_ENCRYPTION));
        result.setSSECustomerAlgorithm(response.getHeaders().get(Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM));
        result.setSSECustomerKeyMd5(response.getHeaders().get(Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5));
    }
}

