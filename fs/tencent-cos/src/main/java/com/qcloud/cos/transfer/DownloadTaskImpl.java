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

import com.qcloud.cos.COS;
import com.qcloud.cos.internal.SkipMd5CheckStrategy;
import com.qcloud.cos.model.COSEncryption;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.utils.ServiceUtils;

final class DownloadTaskImpl implements ServiceUtils.RetryableCOSDownloadTask {
    private final COS cos;
    private final DownloadImpl download;
    private final GetObjectRequest getObjectRequest;
    private final SkipMd5CheckStrategy skipMd5CheckStrategy = SkipMd5CheckStrategy.INSTANCE;

    DownloadTaskImpl(COS cos, DownloadImpl download, GetObjectRequest getObjectRequest) {
        this.cos = cos;
        this.download = download;
        this.getObjectRequest = getObjectRequest;
    }
    
    @Override
    public COSObject getCOSObjectStream() {
        COSObject cosObject = cos.getObject(getObjectRequest);
        download.setCosObject(cosObject);
        return cosObject;
    }

    @Override
    public boolean needIntegrityCheck() {
        // Don't perform the integrity check if the checksum won't matchup.
        return !(cos instanceof COSEncryption)
                && !skipMd5CheckStrategy.skipClientSideValidationPerRequest(getObjectRequest);
    }
}
