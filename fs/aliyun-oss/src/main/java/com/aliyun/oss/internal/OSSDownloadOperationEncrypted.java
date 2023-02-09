/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.internal;

import com.aliyun.oss.OSSEncryptionClient;
import com.aliyun.oss.common.utils.IOUtils;
import com.aliyun.oss.crypto.AdjustedRangeInputStream;
import com.aliyun.oss.crypto.CipherInputStream;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;

import java.io.InputStream;

public class OSSDownloadOperationEncrypted extends OSSDownloadOperation {
    private OSSEncryptionClient ossEncryptionClient;

    public OSSDownloadOperationEncrypted(OSSEncryptionClient ossEncryptionClient) {
        super(ossEncryptionClient.getObjectOperation());
        this.ossEncryptionClient = ossEncryptionClient;
    }

    @Override
    protected OSSObject getObjectWrap(GetObjectRequest getObjectRequest){
        return ossEncryptionClient.getObject(getObjectRequest);
    }

    @Override
    protected Long getInputStreamCRCWrap(InputStream inputStream) {
        if (inputStream instanceof AdjustedRangeInputStream) {
            InputStream subInputStream = ((AdjustedRangeInputStream) inputStream).getWrappedInputStream();
            if (subInputStream instanceof CipherInputStream) {
                InputStream checkedInputStream = ((CipherInputStream) subInputStream).getDelegateStream();
                return IOUtils.getCRCValue(checkedInputStream);
            }
        }
        return null;
    }
}
