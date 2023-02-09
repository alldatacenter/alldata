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

import java.io.InputStream;

import com.qcloud.cos.http.CosHttpResponse;
import com.qcloud.cos.utils.StringUtils;

public class COSStringResponseHandler extends AbstractCosResponseHandler<String> {

    @Override
    public CosServiceResponse<String> handle(CosHttpResponse response) throws Exception {
        CosServiceResponse<String> cosResponse = parseResponseMetadata(response);

        int bytesRead;
        byte[] buffer = new byte[1024];
        StringBuilder builder = new StringBuilder();
        InputStream content = response.getContent();
        while ((bytesRead = content.read(buffer)) > 0) {
            builder.append(new String(buffer, 0, bytesRead, StringUtils.UTF8));
        }
        cosResponse.setResult(builder.toString());

        return cosResponse;
    }

}
