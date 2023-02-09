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

import java.math.BigInteger;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.TreeMap;
import java.util.Map;

import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.internal.SignV2Utils;

/**
 * OSS Object's metadata. It has the user's custom metadata, as well as some
 * standard http headers sent to OSS, such as Content-Length, ETag, etc.
 */
public class ObjectMetadata {

    // The user's custom metadata, whose prefix in http header is x-oss-meta-.
    private Map<String, String> userMetadata = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

    // Other non-custom metadata.
    protected Map<String, Object> metadata = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);

    public static final String AES_256_SERVER_SIDE_ENCRYPTION = "AES256";

    public static final String KMS_SERVER_SIDE_ENCRYPTION = "KMS";

    /**
     * <p>
     * Gets the user's custom metadata.
     * </p>
     * <p>
     * The custom metadata would be appended with x-oss-meta- in the request to
     * OSS. But here the caller should not use x-oss-meta- as the prefix for the
     * keys of the Map instance returned. Meanwhile，the keys is case insenstive
     * and all keys returned from OSS server is in lower case.
     * </p>
     * 
     * @return User's custom metadata.
     */
    public Map<String, String> getUserMetadata() {
        return userMetadata;
    }

    /**
     * Sets the user's custom metadata.
     * 
     * @param userMetadata
     *            The user's custom metadata.
     */
    public void setUserMetadata(Map<String, String> userMetadata) {
        this.userMetadata.clear();
        if (userMetadata != null && !userMetadata.isEmpty()) {
            this.userMetadata.putAll(userMetadata);
        }
    }

    /**
     * Sets the http header (SDK internal usage only).
     * 
     * @param key
     *            The key of header
     * @param value
     *            The value of the key.
     */
    public void setHeader(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * Removes the http header (SDK internal usage only).
     * 
     * @param key
     *            The key of header.
     */
    public void removeHeader(String key) {
        metadata.remove(key);
    }

    /**
     * Adds a new custom metadata.
     * 
     * @param key
     *            Key of the header (not starting with x-oss-meta-)
     * @param value
     *            The value for the key.
     */
    public void addUserMetadata(String key, String value) {
        this.userMetadata.put(key, value);
    }

    /**
     * Gets the value of Last-Modified header, which means the last modified
     * time of the object.
     * 
     * @return Object's last modified time.
     */
    public Date getLastModified() {
        return (Date) metadata.get(OSSHeaders.LAST_MODIFIED);
    }

    /**
     * Sets the value of Last-Modified header, which means the last modified
     * time of the object.
     * 
     * @param lastModified
     *            Object's last modified time.
     */
    public void setLastModified(Date lastModified) {
        metadata.put(OSSHeaders.LAST_MODIFIED, lastModified);
    }

    /**
     * Gets the {@link Date} value of the "Expires" header in Rfc822 format. If
     * expiration is not set, then the value is null.
     * 
     * @return Expires header's value in Rfc822.
     * @throws ParseException
     *             The value is not in the Rfc822 format.
     */
    public Date getExpirationTime() throws ParseException {
        String expires = (String) metadata.get(OSSHeaders.EXPIRES);

        if (expires != null)
            return DateUtil.parseRfc822Date((String) metadata.get(OSSHeaders.EXPIRES));

        return null;
    }

    /**
     * Gets the string value of the "Expires" header in Rfc822 format. If
     * expiration is not set, then the value is null.
     * 
     * @return The string value of "Expires" header.
     */
    public String getRawExpiresValue() {
        return (String) metadata.get(OSSHeaders.EXPIRES);
    }

    /**
     * Sets the "Expires" header.
     * 
     * @param expirationTime
     *            Expiration time.
     */
    public void setExpirationTime(Date expirationTime) {
        metadata.put(OSSHeaders.EXPIRES, DateUtil.formatRfc822Date(expirationTime));
    }

    /**
     * Gets Content-Length header, which is the object content's size.
     * 
     * @return The object content's size.
     */
    public long getContentLength() {
        Long contentLength = (Long) metadata.get(OSSHeaders.CONTENT_LENGTH);
        return contentLength == null ? 0 : contentLength.longValue();
    }

    /**
     * Sets the Content-Length header to indicate the object's size. The correct
     * Content-Length header is needed for a file upload.
     * 
     * @param contentLength
     *            Object content size.
     */
    public void setContentLength(long contentLength) {
        metadata.put(OSSHeaders.CONTENT_LENGTH, contentLength);
    }

    /**
     * Gets the Content-Type header to indicate the object content's type in
     * MIME type format.
     * 
     * @return The content-type header in MIME type format.
     */
    public String getContentType() {
        return (String) metadata.get(OSSHeaders.CONTENT_TYPE);
    }

    /**
     * Sets the Content-Type header to indicate the object content's type in
     * MIME type format.
     * 
     * @param contentType
     *            The content-type header in MIME type format.
     */
    public void setContentType(String contentType) {
        metadata.put(OSSHeaders.CONTENT_TYPE, contentType);
    }

    public String getContentMD5() {
        return (String) metadata.get(OSSHeaders.CONTENT_MD5);
    }

    public void setContentMD5(String contentMD5) {
        metadata.put(OSSHeaders.CONTENT_MD5, contentMD5);
    }

    /**
     * Gets the Content-Encoding header which is to encode the object content.
     * 
     * @return Object content's encoding.
     */
    public String getContentEncoding() {
        return (String) metadata.get(OSSHeaders.CONTENT_ENCODING);
    }

    /**
     * Sets the Content-Encoding header which is to encode the object content.
     * 
     * @param encoding
     *            Object content's encoding.
     */
    public void setContentEncoding(String encoding) {
        metadata.put(OSSHeaders.CONTENT_ENCODING, encoding);
    }

    /**
     * Gets the Cache-Control header. This is the standard http header.
     * 
     * @return Cache-Control header.
     */
    public String getCacheControl() {
        return (String) metadata.get(OSSHeaders.CACHE_CONTROL);
    }

    /**
     * Sets the Cache-Control header. This is the standard http header.
     * 
     * @param cacheControl
     *            Cache-Control header.
     */
    public void setCacheControl(String cacheControl) {
        metadata.put(OSSHeaders.CACHE_CONTROL, cacheControl);
    }

    /**
     * Gets the Content-Disposition header.This is the standard http header.
     * 
     * @return Content-Disposition header.
     */
    public String getContentDisposition() {
        return (String) metadata.get(OSSHeaders.CONTENT_DISPOSITION);
    }

    /**
     * Sets Content-Disposition header.
     * 
     * @param disposition
     *            Content-Disposition header.
     */
    public void setContentDisposition(String disposition) {
        metadata.put(OSSHeaders.CONTENT_DISPOSITION, disposition);
    }

    /**
     * Gets the ETag of the object. ETag is the 128bit MD5 signature in Hex.
     * @return the object's ETag.
     */
    public String getETag() {
        return (String) metadata.get(OSSHeaders.ETAG);
    }

    /**
     * Gets the object's server side encryption.
     * 
     * @return The server side encryption. Null means no encryption.
     */
    public String getServerSideEncryption() {
        return (String) metadata.get(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION);
    }

    /**
     * Sets the object's server side encryption.
     * 
     * @param serverSideEncryption
     *            The server side encryption.
     */
    public void setServerSideEncryption(String serverSideEncryption) {
        metadata.put(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION, serverSideEncryption);
    }

    /**
     * Gets the object's server side encryption key ID.
     *
     * @return The server side encryption key ID. Null means no encryption key ID.
     */
    public String getServerSideEncryptionKeyId() {
        return (String) metadata.get(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION_KEY_ID);
    }

    /**
     * Sets the object's server side encryption key ID.
     *
     * @param serverSideEncryptionKeyId
     *            The server side encryption key ID.
     */
    public void setServerSideEncryptionKeyId(String serverSideEncryptionKeyId) {
        metadata.put(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION_KEY_ID, serverSideEncryptionKeyId);
    }

    /**
     * Sets the object's server side data encryption.
     *
     * @param serverSideDataEncryption
     *            The server side data encryption.
     */
    public void setServerSideDataEncryption(String serverSideDataEncryption) {
        metadata.put(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION, serverSideDataEncryption);
    }

    /**
     * Gets the object's server side data encryption.
     *
     * @return The server side data encryption. Null means no data encryption.
     */
    public String getServerSideDataEncryption() {
        return (String) metadata.get(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION);
    }

    /**
     * Gets the object's storage type, which only supports "normal" and
     * "appendable" for now.
     * 
     * @return Object's storage type.
     */
    public String getObjectType() {
        return (String) metadata.get(OSSHeaders.OSS_OBJECT_TYPE);
    }

    /**
     * Sets the object ACL. For now it only supports default, private,
     * public-read, public-read-write.
     * 
     * @param cannedAcl
     *            Object ACL.
     */
    public void setObjectAcl(CannedAccessControlList cannedAcl) {
        metadata.put(OSSHeaders.OSS_OBJECT_ACL, cannedAcl != null ? cannedAcl.toString() : "");
    }

    /**
     * Gets the raw metadata (SDK internal usage only). The value returned is
     * immutable.
     * 
     * @return The raw metadata object.
     */
    public Map<String, Object> getRawMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * Gets the request Id.
     * 
     * @return RequestId.
     */
    public String getRequestId() {
        return (String) metadata.get(OSSHeaders.OSS_HEADER_REQUEST_ID);
    }
    
    /**
     * Gets the version ID of the associated OSS object if available.
     * Version IDs are only assigned to objects when an object is uploaded to an
     * OSS bucket that has object versioning enabled.
     *
     * @return The version ID of the associated OSS object if available.
     */
    public String getVersionId() {
        return (String)metadata.get(OSSHeaders.OSS_HEADER_VERSION_ID);
    }

    /**
     * Gets the service crc.
     *
     * @return service crc.
     */
    public Long getServerCRC() {
        String strSrvCrc = (String) metadata.get(OSSHeaders.OSS_HASH_CRC64_ECMA);

        if (strSrvCrc != null) {
            BigInteger bi = new BigInteger(strSrvCrc);
            return bi.longValue();
        }
        return null;
    }

    /**
     * Gets the object's storage class, which is "standard", "IA" or "Archive".
     * 
     * @return The storage class of the object.
     */
    public StorageClass getObjectStorageClass() {
        String storageClassString = (String) metadata.get(OSSHeaders.OSS_STORAGE_CLASS);
        if (storageClassString != null) {
            return StorageClass.parse(storageClassString);
        }
        return StorageClass.Standard;
    }

    /**
     * Gets the restore status of the object of Archive type.
     * 
     * @return Object's restore status.
     */
    public String getObjectRawRestore() {
        return (String) metadata.get(OSSHeaders.OSS_RESTORE);
    }

    /**
     * Gets the flag of completeness of restoring the Archive file.
     * 
     * @return The flag of completeness of restoring.
     */
    public boolean isRestoreCompleted() {
        String restoreString = getObjectRawRestore();
        if (restoreString == null) {
            throw new NullPointerException();
        }

        if (restoreString.equals(OSSHeaders.OSS_ONGOING_RESTORE)) {
            return false;
        }
        return true;
    }
    
    public void setObjectTagging(Map<String, String> tags) {
        if (tags != null && !tags.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> tag : tags.entrySet()) {
                String key = tag.getKey();
                String value = tag.getValue();
                
                if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
                    throw new IllegalArgumentException();
                }
                
                if (builder.length() > 0) {
                    builder.append("&");
                }
                builder.append(SignV2Utils.uriEncoding(key));
                builder.append("=");
                builder.append(SignV2Utils.uriEncoding(value));
            }
            metadata.put(OSSHeaders.OSS_TAGGING, builder.toString());
        }
    }
    
}
