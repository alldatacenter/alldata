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
 * {@link OSSObject} summary information.
 */
public class OSSObjectSummary {

    /** The name of the bucket in which this object is stored */
    private String bucketName;

    /** The key under which this object is stored */
    private String key;

    private String eTag;

    private long size;

    private Date lastModified;

    private String storageClass;

    private Owner owner;

    private String type;

    /** The restore info status of the object  */
    private String restoreInfo;

    /**
     * Constructor.
     */
    public OSSObjectSummary() {
    }

    /**
     * Gets the {@link Bucket} name.
     * 
     * @return The bucket name.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the {@link Bucket} name.
     * 
     * @param bucketName
     *            The {@link Bucket} name.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Gets the object key.
     * 
     * @return Object key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the object key.
     * 
     * @param key
     *            Object key.
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the object ETag. ETag is a 128bit MD5 signature about the object in
     * hex.
     * 
     * @return ETag value.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * Sets the object ETag.
     * 
     * @param eTag
     *            ETag value.
     */
    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    /**
     * Gets the object Size
     * 
     * @return Object size.
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the object size.
     * 
     * @param size
     *            Object size.
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Gets the last modified time of the object.
     * 
     * @return The last modified time.
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * Sets the last modified time.
     * 
     * @param lastModified
     *            Last modified time.
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Gets the owner of the object.
     * 
     * @return Object owner.
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * Sets the owner of the object.
     * 
     * @param owner
     *            Object owner.
     */
    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    /**
     * Gets the storage class of the object.
     * 
     * @return Object storage class.
     */
    public String getStorageClass() {
        return storageClass;
    }

    /**
     * Sets the storage class of the object.
     * 
     * @param storageClass
     *            Object storage class.
     */
    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }

    /**
     * Gets the type of the object.
     *
     * @return Object storage class.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the object.
     *
     * @param type
     *            object type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the restore info of the object.
     *
     * @return Object restore info.
     */
    public String getRestoreInfo() {
        return restoreInfo;
    }

    /**
     * Sets the restore info of the object.
     *
     * @param restoreInfo
     *            object restore info
     */
    public void setRestoreInfo(String restoreInfo) {
        this.restoreInfo = restoreInfo;
    }
}
