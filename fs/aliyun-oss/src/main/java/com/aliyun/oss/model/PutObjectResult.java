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

package com.aliyun.oss.model;

import java.io.InputStream;

/**
 * The result class of a Put Object request.
 */
public class PutObjectResult extends GenericResult implements CallbackResult {

    // Object ETag
    private String eTag;
    
    // Object Version Id
    private String versionId;

    // The callback response body. Caller needs to close it.
    private InputStream callbackResponseBody;

    /**
     * Gets the target {@link OSSObject}'s ETag.
     * 
     * @return Target OSSObject's ETag.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * Sets the target {@link OSSObject}'s ETag.
     * 
     * @param eTag
     *            Target OSSObject's ETag.
     */
    public void setETag(String eTag) {
        this.eTag = eTag;
    }
    
    /**
     * Gets version id.
     * 
     * @return version id.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Sets version id.
     * 
     * @param versionId version id.
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    /**
     * Gets the callback response body. The caller needs to close it. Deprecated
     * method. Please use this.getResponse().getContent() instead.
     * 
     * @return The callback response body.
     */
    @Override
    @Deprecated
    public InputStream getCallbackResponseBody() {
        return callbackResponseBody;
    }

    /**
     * Sets the callback response body.
     * 
     * @param callbackResponseBody
     *            The callback response body.
     */
    @Override
    public void setCallbackResponseBody(InputStream callbackResponseBody) {
        this.callbackResponseBody = callbackResponseBody;
    }

}
