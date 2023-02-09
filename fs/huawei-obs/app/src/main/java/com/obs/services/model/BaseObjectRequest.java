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
 * Basic class of object requests
 * 
 * @since 3.20.3
 */
public class BaseObjectRequest extends GenericRequest {
    protected boolean isIgnorePort;
    protected String objectKey;
    protected boolean encodeHeaders = true;

    public BaseObjectRequest() {
    }

    public BaseObjectRequest(String bucketName) {
        this.bucketName = bucketName;
    }

    public BaseObjectRequest(String bucketName, String objectKey) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
    }

    /**
     * Specifies whether to encode and decode the returned header fields.
     *
     * @param encodeHeaders
     *        Specifies whether to encode and decode header fields.
     */
    public void setIsEncodeHeaders(boolean encodeHeaders) {
        this.encodeHeaders = encodeHeaders;
    }

    /**
     * Specifies whether to encode and decode the returned header fields.
     *
     * @return Specifies whether to encode and decode header fields.
     */
    public boolean isEncodeHeaders() {
        return encodeHeaders;
    }

    /**
     * Obtain the object name.
     * 
     * @return Object name
     */
    public String getObjectKey() {
        return objectKey;
    }

    /**
     * Set the object name.
     * 
     * @param objectKey
     *            Object name
     */
    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public boolean getIsIgnorePort() {
        return isIgnorePort;
    }

    public void setIsIgnorePort(boolean ignorePort) {
        isIgnorePort = ignorePort;
    }

    @Override
    public String toString() {
        return "BaseObjectRequest [bucketName=" + bucketName + ", objectKey=" + objectKey
                + ", isRequesterPays()=" + isRequesterPays() + "]";
    }
}
