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

package com.obs.services.model;

/**
 * Status of the requester-pays function of a bucket
 *
 * @since 3.20.3
 */
public enum RequestPaymentEnum {

    /**
     * The bucket owner pays.
     */
    BUCKET_OWNER("BucketOwner"),

    /**
     * The requester pays.
     */
    REQUESTER("Requester");

    private String code;

    private RequestPaymentEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static RequestPaymentEnum getValueFromCode(String code) {
        for (RequestPaymentEnum val : RequestPaymentEnum.values()) {
            if (val.code.equals(code)) {
                return val;
            }
        }
        return null;
    }

}
