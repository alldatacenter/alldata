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
 * Create an asynchronous fetch job response.
 *
 */
public class CreateAsynchFetchJobsResult extends HeaderResponse {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "Wait")
    private int wait;

    public CreateAsynchFetchJobsResult() {

    }

    /**
     * Constructor
     * 
     * @param id
     *            Job ID
     * @param wait
     *            Number of queuing jobs before the current job. The value 0
     *            indicates that the current job is being executed, and the
     *            value -1 indicates that the job has been executed at least
     *            once (the retry logic may be triggered).
     */
    public CreateAsynchFetchJobsResult(String id, int wait) {
        this.id = id;
        this.wait = wait;
    }

    /**
     * Obtain the job ID.
     * 
     * @return Job ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set the job ID.
     * 
     * @param id
     *            Job ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Obtain the number of queuing jobs.
     * 
     * @return Number of queuing jobs
     */
    public int getWait() {
        return wait;
    }

    /**
     * Set the number of queuing jobs.
     * 
     * @param wait
     *            Number of queuing jobs
     */
    public void setWait(int wait) {
        this.wait = wait;
    }

    @Override
    public String toString() {
        return "CreateAsynchFetchJobsResult [id=" + id + ", Wait=" + wait + "]";
    }
}
