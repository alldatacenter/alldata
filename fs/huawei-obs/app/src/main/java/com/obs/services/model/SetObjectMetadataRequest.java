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

import java.util.HashMap;
import java.util.Map;

/**
 * Parameters in a request for setting object properties
 */
public class SetObjectMetadataRequest extends BaseObjectRequest {

    {
        httpMethod = HttpMethodEnum.PUT;
    }

    private String versionId;

    private StorageClassEnum storageClass;

    private String webSiteRedirectLocation;

    private boolean removeUnset;

    private Map<String, String> userMetadata;

    private boolean encodeHeaders = true;

    private final ObjectRepleaceMetadata replaceMetadata = new ObjectRepleaceMetadata();

    public SetObjectMetadataRequest() {
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     */
    public SetObjectMetadataRequest(String bucketName, String objectKey) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param versionId
     *            Version ID of the object
     */
    public SetObjectMetadataRequest(String bucketName, String objectKey, String versionId) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.versionId = versionId;
    }

    /**
     * Obtain the object version ID.
     * 
     * @return Version ID of the object
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Set the version ID of the object.
     * 
     * @param versionId
     *            Version ID of the object
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
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

    /**
     * Specify whether to delete not specified properties. The default value is "false." 
     * true: Use properties set in the request parameters to overwrite
     * the existing property values. Properties not specified in the request
     * will be deleted. 
     * false: Use properties set in the request parameters to
     * overwrite the existing property values. For properties not specified in
     * the request, their existing values are retained.
     * 
     * @return Identifier specifying whether to delete not specified properties
     */
    public boolean isRemoveUnset() {
        return removeUnset;
    }

    /**
     * Specify whether to delete not specified properties.
     * 
     * @param removeUnset
     *            Identifier specifying whether to delete not specified
     *            properties
     */
    public void setRemoveUnset(boolean removeUnset) {
        this.removeUnset = removeUnset;
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
        getAllUserMetadata().put(key, value);
    }

    /**
     * Add customized metadata for a group of objects.
     * 
     * @param userMetadata
     *            Customized metadata for a group of objects
     */
    public void addAllUserMetadata(Map<String, String> userMetadata) {
        if (userMetadata != null) {
            getAllUserMetadata().putAll(userMetadata);
        }
    }

    /**
     * Obtain the customized metadata of an object.
     * 
     * @param key
     *            Keyword of the customized metadata
     * @return Value of the customized metadata
     */
    public Object getUserMetadata(String key) {
        return getAllUserMetadata().get(key);
    }

    /**
     * Obtain the rewritten "Content-Type" header in the response.
     * 
     * @return "Content-Type" header in the response
     */
    public String getContentType() {
        return replaceMetadata.getContentType();
    }

    /**
     * Rewrite the "Content-Type" header in the response.
     * 
     * @param contentType
     *            "Content-Type" header in the response
     */
    public void setContentType(String contentType) {
        replaceMetadata.setContentType(contentType);
    }

    /**
     * Obtain the rewritten "Content-Language" header in the response.
     * 
     * @return "Content-Language" header in the response
     */
    public String getContentLanguage() {
        return replaceMetadata.getContentLanguage();
    }

    /**
     * Rewrite the "Content-Language" header in the response.
     * 
     * @param contentLanguage
     *            "Content-Language" header in the response
     */
    public void setContentLanguage(String contentLanguage) {
        replaceMetadata.setContentLanguage(contentLanguage);
    }

    /**
     * Obtain the rewritten "Expires" header in the response.
     * 
     * @return "Expires" header in the response
     */
    public String getExpires() {
        return replaceMetadata.getExpires();
    }

    /**
     * Rewrite the "Expires" header in the response.
     * 
     * @param expires
     *            Rewritten "Expires" header in the response
     */
    public void setExpires(String expires) {
        replaceMetadata.setExpires(expires);
    }

    /**
     * Obtain the rewritten "Cache-Control" header in the response.
     * 
     * @return "Cache-Control" header in the response
     */
    public String getCacheControl() {
        return replaceMetadata.getCacheControl();
    }

    /**
     * Rewrite the "Cache-Control" header in the response.
     * 
     * @param cacheControl
     *            "Cache-Control" header in the response
     */
    public void setCacheControl(String cacheControl) {
        replaceMetadata.setCacheControl(cacheControl);
    }

    /**
     * Obtain the rewritten "Content-Disposition" header in the response.
     * 
     * @return "Content-Disposition" header in the response
     */
    public String getContentDisposition() {
        return replaceMetadata.getContentDisposition();
    }

    /**
     * Rewrite the "Content-Disposition" header in the response.
     * 
     * @param contentDisposition
     *            "Content-Disposition" header in the response
     */
    public void setContentDisposition(String contentDisposition) {
        replaceMetadata.setContentDisposition(contentDisposition);
    }

    /**
     * Obtain the rewritten "Content-Encoding" header in the response.
     * 
     * @return "Content-Encoding" header in the response
     */
    public String getContentEncoding() {
        return replaceMetadata.getContentEncoding();
    }

    /**
     * Rewrite the "Content-Encoding" header in the response.
     * 
     * @param contentEncoding
     *            "Content-Encoding" header in the response
     */
    public void setContentEncoding(String contentEncoding) {
        replaceMetadata.setContentEncoding(contentEncoding);
    }

    public Map<String, String> getAllUserMetadata() {
        if (userMetadata == null) {
            userMetadata = new HashMap<String, String>();
        }
        return this.userMetadata;
    }

    @Deprecated
    public Map<String, String> getMetadata() {
        return getAllUserMetadata();
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

    @Override
    public String toString() {
        return "SetObjectMetadataRequest [bucketName=" + bucketName + ", objectKey=" + objectKey + ", versionId="
                + versionId + ", storageClass=" + storageClass + ", webSiteRedirectLocation=" + webSiteRedirectLocation
                + ", removeUnset=" + removeUnset + ", userMetadata=" + userMetadata
                + ", replaceMetadata=" + replaceMetadata
                + ", isEncodeHeaders=" + encodeHeaders + "]";
    }

}
