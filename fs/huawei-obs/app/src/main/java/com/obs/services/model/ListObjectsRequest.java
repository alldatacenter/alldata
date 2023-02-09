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
 * Parameters in a request for listing objects in a bucket
 */
public class ListObjectsRequest extends GenericRequest {

    {
        httpMethod = HttpMethodEnum.GET;
    }

    private String prefix;

    private String marker;

    private int maxKeys;

    private String delimiter;

    private int listTimeout;

    private String encodingType;

    public ListObjectsRequest() {
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Bucket name
     */
    public ListObjectsRequest(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Bucket name
     * @param maxKeys
     *            Maximum number of objects to be listed
     */
    public ListObjectsRequest(String bucketName, int maxKeys) {
        this.bucketName = bucketName;
        this.maxKeys = maxKeys;
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Bucket name
     * @param prefix
     *            Object name prefix, used for filtering objects to be listed
     * @param marker
     *            Start position for listing objects
     * @param delimiter
     *            Character used for grouping object names
     * @param maxKeys
     *            Maximum number of objects to be listed
     */
    public ListObjectsRequest(String bucketName, String prefix, String marker, String delimiter, int maxKeys) {
        this.bucketName = bucketName;
        this.maxKeys = maxKeys;
        this.marker = marker;
        this.prefix = prefix;
        this.delimiter = delimiter;

    }

    /**
     Constructor
     *
     * @param bucketName
     *            Bucket name
     * @param prefix
     *            Object name prefix, used for filtering objects to be listed
     * @param marker
     *            Start position for listing objects
     * @param delimiter
     *            Character used for grouping object names
     * @param maxKeys
     *            Maximum number of objects to be listed
     * @param encodingType
     *            Use this encoding type to encode keys that contains invalid characters, the value could be "url"
     */
    public ListObjectsRequest(String bucketName, String prefix, String marker, String delimiter, int maxKeys,
                              String encodingType) {
        this.bucketName = bucketName;
        this.maxKeys = maxKeys;
        this.marker = marker;
        this.delimiter = delimiter;
        this.prefix = prefix;
        this.encodingType = encodingType;
    }


    /**
     * Obtain the object name prefix used for filtering objects to be listed.
     *
     * @return Object name prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Set the object name prefix used for filtering objects to be listed.
     *
     * @param prefix
     *            Object name prefix
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Obtain the start position for listing objects.
     *
     * @return Start position marker
     */
    public String getMarker() {
        return marker;
    }

    /**
     * Set the start position for listing objects.
     *
     * @param marker
     *            Start position marker
     */
    public void setMarker(String marker) {
        this.marker = marker;
    }

    /**
     * Obtain the maximum number of objects to be listed.
     *
     * @return Maximum number of objects to be listed
     */
    public int getMaxKeys() {
        return maxKeys;
    }

    /**
     * Set the maximum number of objects to be listed.
     *
     * @param maxKeys
     *            Maximum number of objects to be listed
     */
    public void setMaxKeys(int maxKeys) {
        this.maxKeys = maxKeys;
    }

    /**
     * Set encoding type to encode objectkeys, the value could be url
     * @param encodingType
     *             encoding type for encoding 
     */
    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

    /**
     * Obtain the character used for grouping object names.
     *
     * @return Character for grouping object names
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Get encoding type to encode objectkeys
     *
     * @return encoding type for encoding
     */
    public String getEncodingType() {
        return encodingType;
    }


    /**
     * Set the character used for grouping object names.
     *
     * @param delimiter
     *            Character for grouping object names
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public int getListTimeout() {
        return listTimeout;
    }

    public void setListTimeout(int listTimeout) {
        this.listTimeout = listTimeout;
    }

    @Override
    public String toString() {
        return "ListObjectsRequest [bucketName=" + bucketName + ", prefix=" + prefix + ", marker=" + marker
                + ", maxKeys=" + maxKeys + ", delimiter=" + delimiter + ", listTimeout=" + listTimeout
                + ", encodingType=" + encodingType + "]";
    }

}
