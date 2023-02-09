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
import com.qcloud.cos.model.DeleteObjectTaggingResult;

/**
 * Handler for retrieving headers from the {@link
 * com.qcloud.cos.model.DeleteObjectTaggingRequest} response.
 */
public class DeleteObjectTaggingHeaderHandler implements HeaderHandler<DeleteObjectTaggingResult> {
    @Override
    public void handle(DeleteObjectTaggingResult result, CosHttpResponse response) {
        result.setVersionId(response.getHeaders().get(Headers.COS_VERSION_ID));
    }
}