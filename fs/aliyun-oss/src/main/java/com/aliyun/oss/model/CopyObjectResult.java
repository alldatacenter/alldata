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

import java.util.Date;

/**
 * The result of copying an existing OSS object.
 */
public class CopyObjectResult extends GenericResult {

    // Target object's ETag
    private String etag;

    // Target object's last modified time.
    private Date lastModified;
    
    // The version ID of the new, copied object. This field will only be present
    // if object versioning has been enabled for the bucket to which the object
    // was copied.
    private String versionId;

    /**
     * Constructor
     */
    public CopyObjectResult() {
    }

    /**
     * Gets the target object's ETag.
     * 
     * @return Target object's ETag.
     */
    public String getETag() {
        return etag;
    }

    /**
     * Sets the target object's ETag (used by SDK only)
     * 
     * @param etag
     *            Target object's ETag.
     */
    public void setEtag(String etag) {
        this.etag = etag;
    }

    /**
     * Gets the last modified of target object.
     * 
     * @return Target object's last modified.
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * Sets the last modified time on the target object.
     * 
     * @param lastModified
     *            Target object's last modified time.
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
    
    /**
     * Gets the target object's version id.
     * 
     * @return Target object's version id.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Sets the target object's version id (used by SDK only).
     * 
     * @param versionId
     *            Target object's version id.
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

}
