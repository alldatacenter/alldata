/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.pb.config.pojo;

/**
 * 
 * InlongStreamConfig
 */
public class InlongStreamConfig {

    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_OFFLINE = -1;
    public static final int SAMPLING_NO_REPORT = 0;
    public static final int SAMPLING_ALL_REPORT = 1;

    private String inlongGroupId;
    private String inlongStreamId;
    private int status;
    // sampling=2, sampling rate=1/2=50%; sampling=10, sampling rate=1/10=10%
    private int sampling;

    /**
     * get inlongGroupId
     * 
     * @return the inlongGroupId
     */
    public String getInlongGroupId() {
        return inlongGroupId;
    }

    /**
     * set inlongGroupId
     * 
     * @param inlongGroupId the inlongGroupId to set
     */
    public void setInlongGroupId(String inlongGroupId) {
        this.inlongGroupId = inlongGroupId;
    }

    /**
     * get inlongStreamId
     * 
     * @return the inlongStreamId
     */
    public String getInlongStreamId() {
        return inlongStreamId;
    }

    /**
     * set inlongStreamId
     * 
     * @param inlongStreamId the inlongStreamId to set
     */
    public void setInlongStreamId(String inlongStreamId) {
        this.inlongStreamId = inlongStreamId;
    }

    /**
     * get status
     * 
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * set status
     * 
     * @param status the status to set
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * get sampling
     * 
     * @return the sampling
     */
    public int getSampling() {
        return sampling;
    }

    /**
     * set sampling
     * 
     * @param sampling the sampling to set
     */
    public void setSampling(int sampling) {
        this.sampling = sampling;
    }
}
