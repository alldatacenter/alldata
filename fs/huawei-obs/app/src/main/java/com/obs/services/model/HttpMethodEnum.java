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
**/

package com.obs.services.model;

/**
 * HTTP/HTTPS request method
 */
public enum HttpMethodEnum {
    /**
     * GET method, normally used for query
     */
    GET("Get"),

    /**
     * PUT method, normally used for adding and modification
     */
    PUT("Put"),

    /**
     * POST method, normally used for adding
     */
    POST("Post"),

    /**
     * DELETE method, normally used for deletion
     */
    DELETE("Delete"),

    /**
     * HEAD method, normally used to query response headers
     */
    HEAD("Head"),

    /**
     * OPTIONS method, normally used for preflight
     */
    OPTIONS("Options");

    private String operationType;

    private HttpMethodEnum(String operationType) {
        if (operationType == null) {
            throw new IllegalArgumentException("operation type code is null");
        }
        this.operationType = operationType;
    }

    public String getOperationType() {
        return this.operationType.toUpperCase();
    }

    public static HttpMethodEnum getValueFromStringCode(String operationType) {
        if (operationType == null) {
            throw new IllegalArgumentException("operation type is null");
        }

        for (HttpMethodEnum installMode : HttpMethodEnum.values()) {
            if (installMode.getOperationType().equals(operationType.toUpperCase())) {
                return installMode;
            }
        }

        throw new IllegalArgumentException("operation type is illegal");
    }
}
