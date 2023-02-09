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

import java.io.Serializable;

import java.util.Date;

/**
 * Contains the summary of a version stored in an OSS bucket. 
 * This object doesn't contain the
 * version's full metadata or any of its contents.
 * 
 * @see OSSObject
 * @see OSSObjectSummary
 */
public class OSSVersionSummary implements Serializable {
	
	private static final long serialVersionUID = -1811213462343527452L;

	/** The name of the bucket in which this version is stored */
    protected String bucketName;
    
    /** The key under which this version is stored */
    private String key;
    
    /** The version ID uniquely identifying this version of an object */
    private String versionId;
    
    /** True if this is the latest version of the associated object */
    private boolean isLatest;
    
    /** The date, according to OSS, when this version was last modified */
    private Date lastModified;

    /**
     * The owner of this version of the associated object - can be null if the
     * requester doesn't have permission to view object ownership information
     */
    private Owner owner;

    /** Hex encoded MD5 hash of this version's contents, as computed by OSS */
    private String eTag;
    
    /** The size of this version, in bytes */
    private long size;
    
    /** The class of storage used by OSS to store this version */
    private String storageClass;
    
    /** True if this object represents a delete marker */
    private boolean isDeleteMarker;

    /** The restore info status of the object  */
    private String restoreInfo;

    
    /**
     * Gets the name of the OSS bucket in which this version is stored.
     * 
     * @return The name of the OSS bucket in which this version is stored.
     * 
     * @see OSSVersionSummary#setBucketName(String)
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the OSS bucket in which this version is stored.
     * 
     * @param bucketName
     *            The name of the OSS bucket in which this version is
     *            stored.
     *            
     * @see OSSVersionSummary#getBucketName()          
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
    
    /**
     * Gets the key under which this version is stored in OSS.
     * 
     * @return The key under which this version is stored in OSS.
     * 
     * @see OSSVersionSummary#setKey(String)
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the key under which this version is stored in OSS.
     * 
     * @param key
     *            The key under which this version is stored in OSS.
     *            
     * @see OSSVersionSummary#getKey()
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the version ID which uniquely identifies this version of an
     * object.
     * <p>
     * Objects created before versioning was enabled or when versioning is
     * suspended will be given the default/null version ID (see
     * {@link com.aliyun.oss.internal.OSSConstants#NULL_VERSION_ID}). Note that the
     * {@link com.aliyun.oss.internal.OSSConstants#NULL_VERSION_ID} is a valid version ID and is not the
     * same as not having a version ID.
     * </p>
     * 
     * @return The version ID which uniquely identifies this version of an
     *         object.
     *         
     * @see OSSVersionSummary#setVersionId(String)        
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Sets the version ID which uniquely identifies this version of an object.
     * 
     * @param id
     *            The version ID which uniquely identifies this version of an
     *            object.
     *            
     * @see OSSVersionSummary#getVersionId()           
     */
    public void setVersionId(String id) {
        this.versionId = id;
    }

    /**
     * Returns whether or not this version is the latest version
     * for the associated object.
     * 
     * @return The value <code>true</code> if this version is the 
     * latest version for the associated object; returns the value
     * <code>false</code> if otherwise.
     */
    public boolean isLatest() {
        return this.isLatest;
    }

    /**
     * For internal use only.
     * Sets whether this version is the latest version for the associated
     * object. This method is intended to be used only by the client internals
     * and developers shouldn't need to use it.
     * 
     * @param isLatest
     *            True if this version represents the latest version for the
     *            associated object in OSS.
     */
    public void setIsLatest(boolean isLatest) {
        this.isLatest = isLatest;
    }

    /**
     * Gets the date according to OSS at which this version was last
     * modified.
     * 
     * @return The date according to OSS at which this version was last
     *         modified.
     *         
     * @see OSSVersionSummary#setLastModified(Date)      
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * Sets the date according to OSS at which this version was last
     * modified.
     * 
     * @param lastModified
     *            The date according to OSS at which this version was
     *            last modified.
     *            
     * @see OSSVersionSummary#getLastModified()                  
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Gets the owner of this version. Returns <code>null</code> 
     * if the requester doesn't have permission for this version or owns the bucket
     * in which it resides.
     * 
     * @return The owner of this version. Returns <code>null</code> 
     *         if the requester doesn't have
     *         permission to see object ownership for this version.
     *         
     * @see OSSVersionSummary#setOwner(Owner)        
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * For internal use only.
     * Sets the owner of this version. This method is intended to be used only
     * by the client internals and developers shouldn't need to use it.
     * 
     * @param owner
     *            The owner of this version.
     *            
     * @see OSSVersionSummary#getOwner()         
     */
    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    /**
     * Returns whether or not this version represents a delete marker.
     * <p>
     * Delete markers are special types of versions that have no data associated
     * with them. When deleting a versioned object in OSS without specifying
     * an explicit version ID, a new delete marker is created as the latest
     * version of that object to mark that the object was deleted.
     * This occurs even though
     * the previous versions still exist in OSS.
     * </p>
     * <p>
     * Delete markers have no data associated with them and therefore have no
     * associated ACL, size or storage class. Only requesters with read access
     * to a bucket can learn of their existence by listing the versions in a
     * bucket.
     * </p>
     * 
     * @return The value <code>true</code> if this version represents a delete marker.
     *         Returns the value <code>false</code> if otherwise.
     */
    public boolean isDeleteMarker() {
        return isDeleteMarker;
    }

    /**
     * Intended for internal use only in the OSS client code. Sets the value of
     * the <code>isDeleteMarker</code> property to record if this is a delete marker or not.
     * 
     * @param isDeleteMarker
     *            Specify <code>true</code> if this version summary represents a delete marker,
     *            otherwise <code>false</code> if it is a regular version summary.
     */
    public void setIsDeleteMarker(boolean isDeleteMarker) {
        this.isDeleteMarker = isDeleteMarker;
    }

    /**
     * Gets the hex encoded hash of this version's contents as computed by OSS.
     * 
     * @return The hex encoded hash of this version's contents as computed by OSS.
     *         
     * @see OSSVersionSummary#setETag(String)       
     */
    public String getETag() {
        return eTag;
    }

    /**
     * Sets the hex encoded hash of this version's contents as computed by OSS.
     * 
     * @param eTag
     *            The hex encoded hash of this version's contents as computed by OSS.
     *            
     * @see OSSVersionSummary#getETag()             
     */
    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    /**
     * Gets the storage class used by OSS for this version.
     * 
     * @return The storage class used by OSS for this version.
     * 
     * @see OSSVersionSummary#setStorageClass(String)
     */
    public String getStorageClass() {
        return storageClass;
    }

    /**
     * Sets the storage class used by OSS for this version.
     * 
     * @param storageClass
     *            The storage class used by OSS for this version.
     *            
     * @see OSSVersionSummary#getStorageClass()         
     */
    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }

    /**
     * Gets the size of this version in bytes.
     * 
     * @return The size of this version in bytes.
     * 
     * @see OSSVersionSummary#setSize(long)
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the size of this version in bytes.
     * 
     * @param size
     *            The size of this version in bytes.
     *            
     * @see OSSVersionSummary#getSize()          
     */
    public void setSize(long size) {
        this.size = size;
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
