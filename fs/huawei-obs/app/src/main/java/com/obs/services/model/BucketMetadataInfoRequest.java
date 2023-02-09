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
**/

package com.obs.services.model;

import java.util.List;

/**
 * Parameters in a request for obtaining bucket metadata
 *
 */
public class BucketMetadataInfoRequest extends OptionsInfoRequest {

    {
        httpMethod = HttpMethodEnum.HEAD;
    }

    // todo 这个 requestHeaders 具体作用，如何合并？
    private List<String> requestHeaders;

    public BucketMetadataInfoRequest() {
        super();
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     */
    public BucketMetadataInfoRequest(String bucketName) {
        super(bucketName);
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param origin
     *            Origin allowed by the CORS rule
     * @param requestHeaders
     *            Request headers allowed by the CORS rules
     */
    public BucketMetadataInfoRequest(String bucketName, String origin, List<String> requestHeaders) {
        this.bucketName = bucketName;
        this.setOrigin(origin);
        this.requestHeaders = requestHeaders;
    }

    /**
     * Obtain the bucket name.
     * 
     * @return Bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Set the bucket name.
     * 
     * @param bucketName
     *            Bucket name
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @Override
    public String toString() {
        return "BucketMetadataInfoRequest [bucketName=" + bucketName + ", requestHeaders=" + requestHeaders + "]";
    }

}
