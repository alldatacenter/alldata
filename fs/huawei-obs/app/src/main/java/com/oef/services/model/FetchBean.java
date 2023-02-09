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
 * Content of the asynchronous fetch policy
 *
 */
public class FetchBean {
    @JsonProperty(value = "status")
    private String status;

    @JsonProperty(value = "agency")
    private String agency;

    public FetchBean() {

    }

    /**
     * Constructor
     * 
     * @param status
     *            Policy status
     * @param agency
     *            IAM agency
     */
    public FetchBean(String status, String agency) {
        this.status = status;
        this.agency = agency;
    }

    /**
     * Obtain the policy status.
     * 
     * @return Policy status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Set the policy status.
     * 
     * @param status
     *            Policy status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Obtain the IAM agency.
     * 
     * @return IAM agency
     */
    public String getAgency() {
        return agency;
    }

    /**
     * Set the IAM agency.
     * 
     * @param agency
     *            IAM agency
     */
    public void setAgency(String agency) {
        this.agency = agency;
    }

    @Override
    public String toString() {
        return "FetchBean [status=" + status + ", agency=" + agency + "]";
    }

}
