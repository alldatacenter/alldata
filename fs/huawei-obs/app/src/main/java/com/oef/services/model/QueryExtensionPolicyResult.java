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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.obs.services.model.HeaderResponse;

/**
 * Query an asynchronous fetch job response in the JSON format.
 *
 */
public class QueryExtensionPolicyResult extends HeaderResponse {
    @JsonProperty(value = "fetch")
    private FetchBean fetch;

    @JsonProperty(value = "transcode")
    private TranscodeBean transcode;

    @JsonProperty(value = "compress")
    private CompressBean compress;

    public QueryExtensionPolicyResult() {
        fetch = new FetchBean();
        transcode = new TranscodeBean();
        compress = new CompressBean();
    }

    /**
     * Constructor
     * 
     * @param fetch
     *            Content of the asynchronous fetch policy
     * @param transcode
     *            Content of the asynchronous transcode policy
     * @param compress
     *            Content of the file compression policy
     */
    public QueryExtensionPolicyResult(FetchBean fetch, TranscodeBean transcode, CompressBean compress) {
        this.fetch = fetch;
        this.transcode = transcode;
        this.compress = compress;
    }

    /**
     * Obtain the content of the asynchronous fetch policy.
     * 
     * @return Content of the asynchronous fetch policy
     */
    public FetchBean getFetch() {
        return fetch;
    }

    /**
     * Set the content of the asynchronous fetch policy.
     * 
     * @param fetch
     *            Content of the asynchronous fetch policy
     */
    public void setFetch(FetchBean fetch) {
        this.fetch = fetch;
    }

    /**
     * Obtain the content of the asynchronous transcode policy.
     * 
     * @return Content of the asynchronous transcode policy
     */
    public TranscodeBean getTranscode() {
        return transcode;
    }

    /**
     * Set the content of the asynchronous transcode policy.
     * 
     * @param transcode
     *            Content of the asynchronous transcode policy
     */
    public void setTranscode(TranscodeBean transcode) {
        this.transcode = transcode;
    }

    /**
     * Obtain the content of the file compression policy.
     * 
     * @return Content of the file compression policy
     */
    public CompressBean getCompress() {
        return compress;
    }

    /**
     * Set the content of the file compression policy.
     * 
     * @param compress
     *            Content of the file compression policy
     */
    public void setCompress(CompressBean compress) {
        this.compress = compress;
    }

    @Override
    public String toString() {
        return "ExtensionPolicyResult [fetch status=" + fetch.getStatus() + ", fetch agency=" + fetch.getAgency()
                + ", transcode status=" + transcode.getStatus() + ", transcode agency=" + transcode.getAgency()
                + ", compress status=" + compress.getStatus() + ", compress agency=" + compress.getAgency() + "]";
    }
}
