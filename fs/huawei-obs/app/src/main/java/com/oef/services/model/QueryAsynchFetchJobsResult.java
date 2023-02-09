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
 * Query an asynchronous fetch job response.
 *
 */
public class QueryAsynchFetchJobsResult extends HeaderResponse {
    @JsonProperty(value = "request_Id")
    private String requestId;

    @JsonProperty(value = "err")
    private String err;

    @JsonProperty(value = "code")
    private String code;

    @JsonProperty(value = "status")
    private String status;

    @JsonProperty(value = "wait")
    private int wait;

    @JsonProperty(value = "job")
    private CreateAsyncFetchJobsRequest job;

    public QueryAsynchFetchJobsResult() {
        job = new CreateAsyncFetchJobsRequest();
    }

    /**
     * Constructor
     * 
     * @param requestId
     *            Unique ID of a request
     * @param err
     *            Error description
     * @param code
     *            Error code
     * @param status
     *            Job status
     * @param wait
     *            Number of queuing jobs before the current job. The value 0
     *            indicates that the current job is being executed, and the
     *            value -1 indicates that the job has been executed at least
     *            once (the retry logic may be triggered).
     * @param job
     *            Job details
     */
    public QueryAsynchFetchJobsResult(String requestId, String err, String code, String status, int wait,
            CreateAsyncFetchJobsRequest job) {
        this.requestId = requestId;
        this.err = err;
        this.code = code;
        this.status = status;
        this.wait = wait;
        this.job = job;
    }

    /**
     * Obtain the unique ID of a request.
     * 
     * @return Unique ID of a request
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Set the unique ID of a request.
     * 
     * @param requestId
     *            Unique ID of a request
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * Obtain the error description.
     * 
     * @return Error description
     */
    public String getErr() {
        return err;
    }

    /**
     * Set the error description.
     * 
     * @param err
     *            Error description
     */
    public void setErr(String err) {
        this.err = err;
    }

    /**
     * Obtain the error code.
     * 
     * @return Error code
     */
    public String getCode() {
        return code;
    }

    /**
     * Set the error code.
     * 
     * @param code
     *            Error code
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Obtain the job status.
     * 
     * @return Job status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Set the job status.
     * 
     * @param status
     *            Job status
     */
    public void setStatus(String status) {
        this.status = status;
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

    /**
     * Obtain the job details.
     * 
     * @return Job details
     */
    public CreateAsyncFetchJobsRequest getJob() {
        return job;
    }

    /**
     * Set the job details.
     * 
     * @param job
     *            Job details
     */
    public void setJob(CreateAsyncFetchJobsRequest job) {
        this.job = job;
    }

    @Override
    public String toString() {
        return "QueryAsynchFetchJobsResult [requestId=" + requestId + ", err=" + err + ", code=" + code + ", status="
                + status + ", wait=" + wait + ", job url=" + job.getUrl() + ", job bucket=" + job.getBucketName()
                + ", job key=" + job.getObjectKey() + ", job callbackurl=" + job.getCallBackUrl()
                + ", job callbackbody=" + job.getCallBackBody() + "]";
    }
}
