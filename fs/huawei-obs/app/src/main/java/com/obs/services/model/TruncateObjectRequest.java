/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model;

/**
 * Request parameters for truncating a file
 * 
 * @since 3.20.3
 */
public class TruncateObjectRequest extends BaseObjectRequest {

    {
        httpMethod = HttpMethodEnum.PUT;
    }

    private long newLength;

    public TruncateObjectRequest() {
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            File name
     * @param newLength
     *            File size after the truncation
     */
    public TruncateObjectRequest(String bucketName, String objectKey, long newLength) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.newLength = newLength;
    }

    /**
     * Obtain the file size after the truncation.
     * 
     * @return File size after the truncation
     */
    public long getNewLength() {
        return newLength;
    }

    /**
     * Set the post-truncation file size.
     * 
     * @param newLength
     *            File size after the truncation
     */
    public void setNewLength(long newLength) {
        this.newLength = newLength;
    }
}
