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

import com.obs.services.model.SpecialParamEnum;

public enum RequestParamEnum {
    /**
     * Obtain, set, or delete an asynchronous policy.
     */
    EXTENSION_POLICY("v1/extension_policy"), 
    /**
     * Obtain, set, or delete an asynchronous job.
     */
    ASYNC_FETCH_JOBS("v1/async-fetch/jobs"),

    DIS_POLICIES("v1/dis_policies"),

    SERVICES_CLUSTERS("v1/services/clusters");

    private String stringCode;

    private RequestParamEnum(String stringCode) {
        if (stringCode == null) {
            throw new IllegalArgumentException("stringCode is null");
        }
        this.stringCode = stringCode;
    }

    public String getStringCode() {
        return this.stringCode.toLowerCase();
    }

    public String getOriginalStringCode() {
        return this.stringCode;
    }

    public static SpecialParamEnum getValueFromStringCode(String stringCode) {
        if (stringCode == null) {
            throw new IllegalArgumentException("string code is null");
        }

        for (SpecialParamEnum installMode : SpecialParamEnum.values()) {
            if (installMode.getStringCode().equals(stringCode.toLowerCase())) {
                return installMode;
            }
        }

        throw new IllegalArgumentException("string code is illegal");
    }
}
