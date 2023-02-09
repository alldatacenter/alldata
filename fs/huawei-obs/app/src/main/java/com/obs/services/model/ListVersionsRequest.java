/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 **/

package com.obs.services.model;

/**
 * Parameters in a request for listing versioning objects in a bucket
 */
public class ListVersionsRequest extends GenericRequest {

    {
        httpMethod = HttpMethodEnum.GET;
    }

    private String prefix;

    private String keyMarker;

    private String versionIdMarker;

    private int maxKeys;

    private String delimiter;

    private int listTimeout;

    private String encodingType;

    public ListVersionsRequest() {
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Bucket name
     */
    public ListVersionsRequest(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Bucket name
     * @param maxKeys
     *            Maximum number of versioning objects to be listed
     */
    public ListVersionsRequest(String bucketName, int maxKeys) {
        this.bucketName = bucketName;
        this.maxKeys = maxKeys;
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Bucket name
     * @param prefix
     *            Oject name prefix used for listing versioning objects
     * @param keyMarker
     *            Start position for listing versioning objects
     * @param delimiter
     *            Character used for sorting versioning object names into groups
     * @param maxKeys
     *            Maximum number of versioning objects to be listed
     */
    public ListVersionsRequest(String bucketName, String prefix, String keyMarker, String delimiter, int maxKeys) {
        this.bucketName = bucketName;
        this.maxKeys = maxKeys;
        this.keyMarker = keyMarker;
        this.delimiter = delimiter;
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Bucket name
     * @param prefix
     *            Oject name prefix used for listing versioning objects
     * @param keyMarker
     *            Start position for listing versioning objects
     * @param delimiter
     *            Character used for sorting versioning object names into groups
     * @param maxKeys
     *            Maximum number of versioning objects to be listed
     * @param encodingType
     *            Encoding type uesd for encoding the keys��the value can be url
     */
    public ListVersionsRequest(String bucketName, String prefix, String keyMarker, String delimiter,
                               int maxKeys, String encodingType) {
        this.bucketName = bucketName;
        this.maxKeys = maxKeys;
        this.prefix = prefix;
        this.keyMarker = keyMarker;
        this.delimiter = delimiter;
        this.encodingType = encodingType;
    }

    /**
     * Set encodingType used for encoding keys, the value can be url
     *
     * @param encodingType used for encoding keys, the value can be url
     */
    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

    /**
     * Get encodingType
     *
     * @return encodingType
     */
    public String getEncodingType() {
        return encodingType;
    }

    /**
     * Obtain the object name prefix used for listing versioning objects.
     *
     * @return Object name prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Set the object name prefix used for listing versioning objects.
     *
     * @param prefix
     *            Object name prefix
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Obtain the maximum number of versioning objects to be listed.
     *
     * @return Maximum number of versioning objects to be listed
     */
    public int getMaxKeys() {
        return maxKeys;
    }

    /**
     * Set the maximum number of versioning objects to be listed.
     *
     * @param maxKeys
     *            Maximum number of versioning objects to be listed
     */
    public void setMaxKeys(int maxKeys) {
        this.maxKeys = maxKeys;
    }

    /**
     * Character used for grouping versioning object names.
     *
     * @return Character for grouping object names
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Set the character used for grouping versioning object names.
     *
     * @param delimiter
     *            Character for grouping objet names
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Obtain the start position (versionId) for listing versioning objects.
     *
     * @return Start position for listing versioning objects
     */
    public String getVersionIdMarker() {
        return versionIdMarker;
    }

    /**
     * Set the start position (versionId) for listing versioning objects.
     *
     * @param versionIdMarker
     *            Start position for listing versioning objects
     */
    public void setVersionIdMarker(String versionIdMarker) {
        this.versionIdMarker = versionIdMarker;
    }

    /**
     * Obtain the start position for listing versioning objects.
     *
     * @return Start position for listing versioning objects
     */
    public String getKeyMarker() {
        return keyMarker;
    }

    /**
     * Set the start position for listing versioning objects.
     *
     * @param keyMarker
     *            Start position for listing versioning objects
     */
    public void setKeyMarker(String keyMarker) {
        this.keyMarker = keyMarker;
    }

    public int getListTimeout() {
        return listTimeout;
    }

    public void setListTimeout(int listTimeout) {
        this.listTimeout = listTimeout;
    }

    @Override
    public String toString() {
        return "ListVersionsRequest [bucketName=" + bucketName + ", prefix=" + prefix + ", keyMarker=" + keyMarker
                + ", versionIdMarker=" + versionIdMarker + ", maxKeys=" + maxKeys + ", delimiter=" + delimiter
                + ", listTimeout=" + listTimeout + ", encodingType=" + encodingType + "]";
    }

}
