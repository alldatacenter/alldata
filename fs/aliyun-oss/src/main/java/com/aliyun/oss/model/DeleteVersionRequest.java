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

/**
 * <p>
 * Provides options for deleting a specific version of an object in the specified bucket.
 * Once deleted, there is no method to restore or undelete an object version.
 * This is the only way to permanently delete object versions that are protected
 * by versioning.
 * </p>
 * <p>
 * Because deleting an object version is permanent and irreversible, it is a
 * privileged operation that only the owner of the bucket containing the version
 * may perform.
 * </p>
 * <p>
 * An owner can only delete a version of an object if the owner has enabled versioning for
 * their bucket. 
 * For more information about enabling versioning for a bucket, see 
 * {@link com.aliyun.oss.OSS#setBucketVersioning(SetBucketVersioningRequest)}.
 * </p>
 * <p>
 * Note: When attempting to delete an object that does not exist, 
 * OSS returns a success message, not an error message.
 * </p>
 */
public class DeleteVersionRequest extends GenericRequest {

    /**
     * The version ID uniquely identifying which version of the object to
     * delete.
     */
    private String versionId;
    
    /**
     * Constructs a new {@link DeleteVersionRequest} object, 
     * ready to be executed to
     * delete the version identified by the specified version ID, in the
     * specified bucket and key.
     * 
     * @param bucketName
     *            The name of the bucket containing the version to delete.
     * @param key
     *            The key of the object version to delete.
     * @param versionId
     *            The version ID identifying the version to delete.
     */
    public DeleteVersionRequest(String bucketName, String key, String versionId) {
    	super(bucketName, key);
        this.versionId = versionId;
    }

    /**
     * Gets the version ID uniquely identifying which version of the object
     * to delete.
     * 
     * @return The version ID uniquely identifying which version of the object
     *         to delete.
     *         
     * @see DeleteVersionRequest#setVersionId(String)
     * @see DeleteVersionRequest#withVersionId(String)    
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Sets the version ID uniquely identifying which version of the
     * object to delete.
     * 
     * @param versionId
     *            The version ID uniquely identifying which version of
     *            the object to delete.
     *            
     * @see DeleteVersionRequest#getVersionId()           
     * @see DeleteVersionRequest#withVersionId(String)                
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    /**
     * Sets the version ID uniquely identifying which version of the object to
     * delete
     * Returns this {@link DeleteVersionRequest}, enabling additional method
     * calls to be chained together.
     * 
     * @param versionId
     *            The version ID uniquely identifying which version of the
     *            object to delete.
     * 
     * @return This {@link DeleteVersionRequest}, enabling additional method
     *         calls to be chained together.
     *         
     * @see DeleteVersionRequest#getVersionId()
     * @see DeleteVersionRequest#setVersionId(String)         
     */
    public DeleteVersionRequest withVersionId(String versionId) {
        setVersionId(versionId);
        return this;
    }

}
