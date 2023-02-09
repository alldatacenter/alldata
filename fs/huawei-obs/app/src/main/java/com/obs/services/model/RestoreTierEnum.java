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
 */

package com.obs.services.model;

/**
 * Restoration option
 */
public enum RestoreTierEnum {
    /**
     * Expedited restoration, which restores an object in 1 to 5 minutes
     */
    EXPEDITED("Expedited"),
    /**
     * Standard restoration, which restores the object
     * in 3 to 5 hours
     */
    STANDARD("Standard"),
    /**
     * Batch restoration, which restores objects in 5 to
     * 12 hours
     */
    BULK("Bulk");

    private String code;

    RestoreTierEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static RestoreTierEnum getValueFromCode(String code) {
        for (RestoreTierEnum val : RestoreTierEnum.values()) {
            if (val.code.equals(code)) {
                return val;
            }
        }
        return null;
    }
}
