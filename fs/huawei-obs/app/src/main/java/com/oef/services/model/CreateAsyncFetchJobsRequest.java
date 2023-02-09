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

package com.oef.services.model;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.utils.ServiceUtils;

/**
 * Create an asynchronous fetch job request in the JSON format.
 *
 */
public class CreateAsyncFetchJobsRequest {
    @JsonProperty(value = "url")
    private String url;

    @JsonProperty(value = "bucket")
    private String bucket;

    @JsonProperty(value = "host")
    private String host;

    @JsonProperty(value = "key")
    private String key;

    @JsonProperty(value = "md5")
    private String md5;

    @JsonProperty(value = "callbackurl")
    private String callBackUrl;

    @JsonProperty(value = "callbackbody")
    private String callBackBody;

    @JsonProperty(value = "callbackbodytype")
    private String callBackBodyType;

    @JsonProperty(value = "callbackhost")
    private String callBackHost;

    @JsonProperty(value = "file_type")
    private String fileType;

    @JsonProperty(value = "ignore_same_key")
    private boolean ignoreSameKey;

    public CreateAsyncFetchJobsRequest() {
    }

    /**
     * Constructor
     * 
     * @param url
     *            URL to be fetched. You can set multiple URLs and separate them
     *            with semicolons (;).
     * @param bucket
     *            Bucket name
     */
    public CreateAsyncFetchJobsRequest(String url, String bucket) {
        this.url = url;
        this.bucket = bucket;
    }

    /**
     * Obtain the URL.
     * 
     * @return url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the URL.
     * 
     * @param url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Obtain the bucket name.
     * 
     * @return Bucket name
     */
    public String getBucketName() {
        return bucket;
    }

    /**
     * Set the bucket name.
     * 
     * @param bucket
     *            Bucket name
     */
    public void setBucketName(String bucket) {
        this.bucket = bucket;
    }

    /**
     * Obtain the host used for downloading data from a specified URL.
     * 
     * @return Host used for downloading data from a specified URL
     */
    public String getHost() {
        return host;
    }

    /**
     * Set the host used for downloading data from a specified URL.
     * 
     * @param host
     *            Host used for downloading data from a specified URL
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Obtain the object name.
     * 
     * @return Object name
     */
    public String getObjectKey() {
        return key;
    }

    /**
     * Set the object name.
     * 
     * @param key
     *            Object name
     */
    public void setObjectKey(String key) {
        this.key = key;
    }

    /**
     * Obtain the MD5 file.
     * 
     * @return File MD5
     */
    public String getMd5() {
        return md5;
    }

    /**
     * Set the MD5 file.
     * 
     * @param md5
     */
    public void setMd5(String md5) {
        this.md5 = md5;
    }

    /**
     * Obtain the callback URL.
     * 
     * @return Callback URL.
     */
    public String getCallBackUrl() {
        return callBackUrl;
    }

    /**
     * Set the callback URL.
     * 
     * @param callBackUrl
     *            Callback URL.
     */
    public void setCallBackUrl(String callBackUrl) {
        this.callBackUrl = callBackUrl;
    }

    /**
     * Obtain the callback body.
     * 
     * @return Callback body
     */
    public String getCallBackBody() {
        return callBackBody;
    }

    /**
     * Set the callback body (Base64 encoded).
     * 
     * @param callBackBody
     *            Callback body
     * @throws ServiceException
     */
    public void setCallBackBody(String callBackBody) throws ServiceException {
        this.callBackBody = ServiceUtils.toBase64(callBackBody.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Obtain the content type of the callback body.
     * 
     * @return Content type of the callback body
     */
    public String getCallbackBodyType() {
        return callBackBodyType;
    }

    /**
     * Set the content type of the callback body.
     * 
     * @param callBackBodyType
     *            Content type of the callback body
     */
    public void setCallbackBodyType(String callBackBodyType) {
        this.callBackBodyType = callBackBodyType;
    }

    /**
     * Obtain the host used for callback.
     * 
     * @return Host used for callback
     */
    public String getCallBackHost() {
        return callBackHost;
    }

    /**
     * Set the host used for callback.
     * 
     * @param callBackHost
     *            Host used for callback
     */
    public void setCallBackHost(String callBackHost) {
        this.callBackHost = callBackHost;
    }

    /**
     * Obtain the storage file type
     * 
     * @return Storage file type
     */
    public String getFileType() {
        return fileType;
    }

    /**
     * Set the storage file type
     * 
     * @param fileType
     *            Storage file type
     */
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    /**
     * If a file with the same name already exists in the namespace, the current
     * fetch job is canceled.
     * 
     * @return ignoreSameKey
     */
    public boolean isignoreSameKey() {
        return ignoreSameKey;
    }

    /**
     * @param ignoreSameKey
     */
    public void setignoreSameKey(boolean ignoreSameKey) {
        this.ignoreSameKey = ignoreSameKey;
    }

    @Override
    public String toString() {
        return "CreateAsyncFetchJobsRequest [url=" + url + ", bucket=" + bucket + ", host=" + host + ", key=" + key
                + ", md5=" + md5 + ", callBackUrl=" + callBackUrl + ", callBackBody=" + callBackBody
                + ", callBackBodyType=" + callBackBodyType + ", callBackHost=" + callBackHost + ", fileType=" + fileType
                + ", ignoreSameKey=" + ignoreSameKey + "]";
    }

}
