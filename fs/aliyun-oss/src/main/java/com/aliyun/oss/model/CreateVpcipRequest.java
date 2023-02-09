/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.model;

public class CreateVpcipRequest extends GenericRequest {

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * Deprecated method.
     * Please use method #getLabel instead.
     *
     * @return return labal info
     */
    @Deprecated
    public String getLabal() {
        return label;
    }

    /**
     * Deprecated method.
     * Please use method #setLabel instead.
     *
     * @param label
     *            set label info.
     */
    @Deprecated
    public void setLabal(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getVSwitchId() {
        return vSwitchId;
    }

    public void setVSwitchId(String vSwitchId) {
        this.vSwitchId = vSwitchId;
    }

	@Override
    public String toString() {
        return "CreateVpcipRequest [region=" + region + ", vSwitchId=" + vSwitchId + ", labal=" + label + "]";
    }

    private String region;
    private String vSwitchId;
    private String label;
}
