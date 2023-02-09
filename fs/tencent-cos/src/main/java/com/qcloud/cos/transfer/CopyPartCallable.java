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
import com.qcloud.cos.model.CopyPartRequest;
import com.qcloud.cos.model.CopyPartResult;
import com.qcloud.cos.model.PartETag;

/**
 * An implementation of the Callable interface responsible for carrying out the
 * Copy part requests.
 *
 */
public class CopyPartCallable implements Callable<PartETag> {

    /** Reference to the COS client object used for initiating copy part request.*/
    private final COS cos;
    /** Copy part request to be initiated.*/
    private final CopyPartRequest request;

    public CopyPartCallable(COS cos, CopyPartRequest request) {
        this.cos = cos;
        this.request = request;
    }

    public PartETag call() throws Exception {
        CopyPartResult copyPartResult = cos.copyPart(request);
        return copyPartResult == null ? null : copyPartResult.getPartETag();
    }
}