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
 * Redirection condition
 */
public class RouteRuleCondition {
    private String keyPrefixEquals;

    private String httpErrorCodeReturnedEquals;

    /**
     * Obtain the object name prefix when the redirection takes effect.
     * 
     * @return Object name prefix when the redirection takes effect
     */
    public String getKeyPrefixEquals() {
        return keyPrefixEquals;
    }

    /**
     * Set the object name prefix when the redirection takes effect.
     * 
     * @param keyPrefixEquals
     *            Object name prefix when the redirection takes effect
     */
    public void setKeyPrefixEquals(String keyPrefixEquals) {
        this.keyPrefixEquals = keyPrefixEquals;
    }

    /**
     * Obtain the HTTP error code configuration when the redirection takes
     * effect.
     * 
     * @return HTTP error code configuration when the redirection takes effect
     */
    public String getHttpErrorCodeReturnedEquals() {
        return httpErrorCodeReturnedEquals;
    }

    /**
     * Configure the HTTP error code when the redirection takes effect.
     * 
     * @param httpErrorCodeReturnedEquals
     *            HTTP error code configuration when the redirection takes
     *            effect.
     */
    public void setHttpErrorCodeReturnedEquals(String httpErrorCodeReturnedEquals) {
        this.httpErrorCodeReturnedEquals = httpErrorCodeReturnedEquals;
    }

    @Override
    public String toString() {
        return "RouteRuleCondition [keyPrefixEquals=" + keyPrefixEquals + ", httpErrorCodeReturnedEquals="
                + httpErrorCodeReturnedEquals + "]";
    }

}
