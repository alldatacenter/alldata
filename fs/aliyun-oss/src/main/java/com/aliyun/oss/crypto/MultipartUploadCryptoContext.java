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

package com.aliyun.oss.crypto;

import java.io.Serializable;

public class MultipartUploadCryptoContext implements Serializable {
    private static final long serialVersionUID = -1273005579744565699L;
    private String uploadId;
    private ContentCryptoMaterial cekMaterial;
    private long partSize;
    private long dataSize;

    public MultipartUploadCryptoContext() {
        partSize = 0;
        dataSize = 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int)partSize;
        result = prime * result + (int)dataSize;
        result = prime * result + ((uploadId == null) ? 0 : uploadId.hashCode());
        result = prime * result + ((cekMaterial == null) ? 0 : cekMaterial.hashCode());
        return result;
    }

    public void setPartSize(long partSize) {
        this.partSize = partSize;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }

    public long getDataSize() {
        return dataSize;
    }

    public long getPartSize() {
        return partSize;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setContentCryptoMaterial(ContentCryptoMaterial cekMaterial) {
        this.cekMaterial = cekMaterial;
    }

    /**
     * @return the content encrypting cryptographic material for the multi-part
     *         uploads.
     */
    public ContentCryptoMaterial getContentCryptoMaterial() {
        return cekMaterial;
    }
}
