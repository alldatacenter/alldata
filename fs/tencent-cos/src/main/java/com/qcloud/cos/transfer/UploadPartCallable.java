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


package com.qcloud.cos.transfer;

import java.util.concurrent.Callable;

import com.qcloud.cos.COS;
import com.qcloud.cos.model.PartETag;
import com.qcloud.cos.model.UploadPartRequest;

public class UploadPartCallable implements Callable<PartETag> {
    private final COS cos;
    private final UploadPartRequest request;

    public UploadPartCallable(COS cos, UploadPartRequest request) {
        this.cos = cos;
        this.request = request;
    }

    public PartETag call() throws Exception {
        PartETag partETag = cos.uploadPart(request).getPartETag();
        return partETag;
    }
}
