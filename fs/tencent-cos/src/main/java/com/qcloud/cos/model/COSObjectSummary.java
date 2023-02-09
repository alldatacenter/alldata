/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Contains the summary of an object stored in an Qcloud COS bucket. This object
 * doesn't contain contain the
 * object's full metadata or any of its contents.
 * 
 * @see COSObject
 */
public class COSObjectSummary implements Serializable {

    /** The name of the bucket in which this object is stored */
    protected String bucketName;

    /** The key under which this object is stored */
    protected String key;

    /** Hex encoded MD5 hash of this object's contents, as computed by Qcloud COS */
    protected String eTag;

    /** The size of this object, in bytes */
    protected long size;

    /** The date, according to Qcloud COS, when this object was last modified */
    protected Date lastModified;

    /** The class of storage used by Qcloud COS to store this object */
    protected String storageClass;
    
    /**
     * The owner of this object - can be null if the requester doesn't have
     * permission to view object ownership information
     */
    protected Owner owner;


    /**
     * Gets the name of the Qcloud COS bucket in which this object is stored.
     * 
     * @return The name of the Qcloud COS bucket in which this object is stored.
     * 
     * @see COSObjectSummary#setBucketName(String)
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the Qcloud COS bucket in which this object is stored.
     * 
     * @param bucketName
     *            The name of the Qcloud COS bucket in which this object is
     *            stored.
     *            
     * @see COSObjectSummary#getBucketName()          
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Gets the key under which this object is stored in Qcloud COS.
     * 
     * @return The key under which this object is stored in Qcloud COS.
     * 
     * @see COSObjectSummary#setKey(String)
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the key under which this object is stored in Qcloud COS.
     * 
     * @param key
     *            The key under which this object is stored in Qcloud COS.
     *            
     * @see COSObjectSummary#getKey()          
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the hex encoded 128-bit MD5 hash of this object's contents as
     * computed by Qcloud COS.
     * 
     * @return The hex encoded 128-bit MD5 hash of this object's contents as
     *         computed by Qcloud COS.
     *         
     * @see COSObjectSummary#setETag(String)       
     */
    public String getETag() {
        return eTag;
    }

    /**
     * Sets the hex encoded 128-bit MD5 hash of this object's contents as
     * computed by Qcloud COS.
     * 
     * @param eTag
     *            The hex encoded 128-bit MD5 hash of this object's contents as
     *            computed by Qcloud COS.
     *            
     * @see COSObjectSummary#getETag()             
     */
    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    /**
     * Gets the size of this object in bytes.
     * 
     * @return The size of this object in bytes.
     * 
     * @see #setSize(long)
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the size of this object in bytes.
     * 
     * @param size
     *            The size of this object in bytes.
     *            
     * @see #getSize()           
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Gets the date when, according to Qcloud COS, this object
     * was last modified.
     * 
     * @return The date when, according to Qcloud COS, this object
     *         was last modified.
     *         
     * @see COSObjectSummary#setLastModified(Date)
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * Sets the date, according to Qcloud COS, this object
     * was last modified.
     * 
     * @param lastModified
     *            The date when, according to Qcloud COS, this object
     *            was last modified.
     *            
     * @see COSObjectSummary#getLastModified()          
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Gets the owner of this object. Returns <code>null</code> 
     * if the requester doesn't have
     * {@link Permission#ReadAcp} permission for this object or owns the bucket
     * in which it resides.
     * 
     * @return The owner of this object. Returns <code>null</code> 
     *         if the requester doesn't have
     *         permission to see object ownership.
     *         
     * @see COSObjectSummary#setOwner(Owner)        
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * Sets the owner of this object.
     * 
     * @param owner
     *            The owner of this object.
     *            
     * @see COSObjectSummary#getOwner()                   
     */
    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    /**
     * Gets the storage class used by Qcloud COS for this object.
     * 
     * @return The storage class used by Qcloud COS for this object.
     * 
     * @see COSObjectSummary#setStorageClass(String)
     */
    public String getStorageClass() {
        return storageClass;
    }

    /**
     * Sets the storage class used by Qcloud COS for this object.
     * 
     * @param storageClass
     *            The storage class used by Qcloud COS for this object.
     *            
     * @see COSObjectSummary#getStorageClass()            
     */
    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }

}

