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

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.obs.services.internal.utils.ServiceUtils;

/**
 * Object properties
 */
public class ObjectMetadata extends HeaderResponse {
    private Date lastModified;

    private Long contentLength;

    private String contentType;

    private String contentEncoding;

    private String contentDisposition;
    
    private String cacheControl;
    
    private String contentLanguage;
    
    private String expires;
    
    private String etag;

    private String contentMd5;

    private String crc64;

    private StorageClassEnum storageClass;

    private String webSiteRedirectLocation;

    private long nextPosition = -1;

    private boolean appendable;

    private Map<String, Object> userMetadata;

    /**
     * Identify whether an object is appendable.
     * 
     * @return Identifier specifying whether the object is appendable
     */
    public boolean isAppendable() {
        return appendable;
    }

    public void setAppendable(boolean appendable) {
        this.appendable = appendable;
    }

    /**
     * Obtain the start position for next appending. This setting is valid only
     * when "isAppendable" is set to "true" and this parameter value is larger
     * than "0".
     * 
     * @return Position from which the next appending starts
     */
    public long getNextPosition() {
        return nextPosition;
    }

    public void setNextPosition(long nextPosition) {
        this.nextPosition = nextPosition;
    }

    /**
     * Obtain object properties.
     * 
     * @return Object properties
     */
    public Map<String, Object> getAllMetadata() {
        if (userMetadata == null) {
            userMetadata = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        }
        return this.userMetadata;
    }

    @Deprecated
    public Map<String, Object> getMetadata() {
        return this.getResponseHeaders();
    }

    /**
     * Add customized metadata for an object.
     * 
     * @param key
     *            Keyword of the customized metadata
     * @param value
     *            Value of the customized metadata
     */
    public void addUserMetadata(String key, String value) {
        getAllMetadata().put(key, value);
    }

    /**
     * Obtain the customized metadata of an object.
     * 
     * @param key
     *            Keyword of the customized metadata
     * @return Value of the customized metadata
     */
    public Object getUserMetadata(String key) {
        return getAllMetadata().get(key);
    }

    /**
     * Obtain the ETag of the object.
     * 
     * @return ETag of the object
     */
    public String getEtag() {
        return etag;
    }

    public void setEtag(String objEtag) {
        this.etag = objEtag;
    }

    /**
     * Set object properties.
     * 
     * @param metadata
     *            Object properties
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.responseHeaders = metadata;
    }

    /**
     * Obtain the last modification time of the object.
     * 
     * @return Last modification time of the object
     */
    public Date getLastModified() {
        return ServiceUtils.cloneDateIgnoreNull(this.lastModified);
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = ServiceUtils.cloneDateIgnoreNull(lastModified);
    }

    /**
     * Obtain the content encoding of the object.
     * 
     * @return Content encoding
     */
    public String getContentEncoding() {
        return contentEncoding;
    }

    /**
     * Set the content encoding of the object.
     * 
     * @param contentEncoding
     *            Content encoding
     */
    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    /**
     * 
     * @return
     * 
     * @since 3.20.7
     */
    public String getContentDisposition() {
        return contentDisposition;
    }

    /**
     * 设置对象的Content-Disposition
     * 
     * @param contentDisposition
     * 
     * @since 3.20.7
     */
    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    /**
     * 
     * @return
     * 
     * @since 3.20.7
     */
    public String getCacheControl() {
        return cacheControl;
    }

    /**
     * 
     * @param cacheControl
     * 
     * @since 3.20.7
     */
    public void setCacheControl(String cacheControl) {
        this.cacheControl = cacheControl;
    }

    /**
     * 
     * @return
     * 
     * @since 3.20.7
     */
    public String getContentLanguage() {
        return contentLanguage;
    }

    /**
     * 
     * @param contentLanguage
     * 
     * @since 3.20.7
     */
    public void setContentLanguage(String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }

    /**
     * 
     * @return
     * 
     * @since 3.20.7
     */
    public String getExpires() {
        return expires;
    }

    /**
     * 
     * @param expires
     * 
     * @since 3.20.7
     */
    public void setExpires(String expires) {
        this.expires = expires;
    }
    
    /**
     * Obtain the content length of an object.
     * 
     * @return Content length of the object
     */
    public Long getContentLength() {
        return contentLength;
    }

    /**
     * Set the content length of an object.
     * 
     * @param contentLength
     *            Content length of the object
     */
    public void setContentLength(Long contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * Obtain the MIME type of an object.
     * 
     * @return MIME type of the object
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Set the MIME type for an object.
     * 
     * @param contentType
     *            MIME type of the object
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Obtain the object storage class.
     * 
     * @return Object storage class
     */
    @Deprecated
    public String getStorageClass() {
        return this.storageClass != null ? this.storageClass.getCode() : null;
    }

    /**
     * Set the object storage class.
     * 
     * @param storageClass
     *            Object storage class
     */
    @Deprecated
    public void setStorageClass(String storageClass) {
        this.storageClass = StorageClassEnum.getValueFromCode(storageClass);
    }

    /**
     * Obtain the object storage class.
     * 
     * @return Object storage class
     */
    public StorageClassEnum getObjectStorageClass() {
        return storageClass;
    }

    /**
     * Set the object storage class.
     * 
     * @param storageClass
     *            Object storage class
     */
    public void setObjectStorageClass(StorageClassEnum storageClass) {
        this.storageClass = storageClass;
    }

    public Object getValue(String name) {
        for (Entry<String, Object> entry : this.getAllMetadata().entrySet()) {
            if (isEqualsIgnoreCase(entry.getKey(), name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isEqualsIgnoreCase(String key1, String key2) {
        if (key1 == null && key2 == null) {
            return true;
        }
        if (key1 == null || key2 == null) {
            return false;
        }

        return key1.equalsIgnoreCase(key2);
    }

    /**
     * Obtain Base64-encoded MD5 value of an object.
     * 
     * @return Base64-encoded MD5 value of the object
     */
    public String getContentMd5() {
        return contentMd5;
    }

    /**
     * Set the Base64-encoded MD5 value for an object.
     * 
     * @param contentMd5
     *            Base64-encoded MD5 value of the object
     */
    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
    }

    public String getCrc64() {
        return crc64;
    }

    public void setCrc64(String crc64) {
        this.crc64 = crc64;
    }

    /**
     * Set the redirection link which can redirect the request to another object
     * in the bucket or to an external URL.
     * 
     * @return Redirection link
     */
    public String getWebSiteRedirectLocation() {
        return webSiteRedirectLocation;
    }

    /**
     * Obtain the redirection link which can redirect the request to another
     * object in the bucket or to an external URL.
     * 
     * @param webSiteRedirectLocation
     *            Redirection link
     */
    public void setWebSiteRedirectLocation(String webSiteRedirectLocation) {
        this.webSiteRedirectLocation = webSiteRedirectLocation;
    }

    /**
     * Set user custom metadata
     *
     * @param userMetadata
     *         user custom metadata
     */
    public void setUserMetadata(Map<String, Object> userMetadata) {
        this.userMetadata = userMetadata;
    }

    @Override
    public String toString() {
        return "ObjectMetadata [metadata=" + this.getAllMetadata() + ", lastModified=" + lastModified
                + ", contentDisposition=" + contentDisposition + ", cacheControl=" + cacheControl 
                + ", expires=" + expires + ", contentLength=" + contentLength + ", contentType=" 
                + contentType + ", contentEncoding=" + contentEncoding + ", etag=" + etag 
                + ", contentMd5=" + contentMd5 + ", storageClass=" + storageClass 
                + ", webSiteRedirectLocation=" + webSiteRedirectLocation + ", nextPosition=" 
                + nextPosition + ", appendable=" + appendable + "]";
    }

}
