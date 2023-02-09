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

/**
 * Configure an asynchronous policy request in the JSON format.
 *
 */
public class PutExtensionPolicyRequest {
    @JsonProperty(value = "fetch")
    private FetchBean fetch;

    @JsonProperty(value = "transcode")
    private TranscodeBean transcode;

    @JsonProperty(value = "compress")
    private CompressBean compress;

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
        String fetchStatus = fetch == null ? null : fetch.getStatus();
        String fetchAgency = fetch == null ? null : fetch.getAgency();
        String transcodeStatus = transcode == null ? null : transcode.getStatus();
        String transcodeAgency = transcode == null ? null : transcode.getAgency();
        String compressStatus = compress == null ? null : compress.getStatus();
        String compressAgency = compress == null ? null : compress.getAgency();

        return "ExtensionPolicyRequest [fetch status=" + fetchStatus + ", fetch agency=" + fetchAgency
                + ", transcode status=" + transcodeStatus + ", transcode agency=" + transcodeAgency
                + ", compress status=" + compressStatus + ", compress agency=" + compressAgency + "]";
    }
}
