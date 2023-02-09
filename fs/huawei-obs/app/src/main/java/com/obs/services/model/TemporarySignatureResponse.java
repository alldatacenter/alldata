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

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Response to a request for temporarily authorized access
 *
 */
public class TemporarySignatureResponse {
    private String signedUrl;

    private Map<String, String> actualSignedRequestHeaders;

    public TemporarySignatureResponse(String signedUrl) {
        this.signedUrl = signedUrl;
    }

    /**
     * Obtain the URL of the temporarily authorized access.
     * 
     * @return URL of the temporarily authorized access
     */
    public String getSignedUrl() {
        return signedUrl;
    }

    /**
     * Obtain the request headers.
     * 
     * @return Request headers
     */
    public Map<String, String> getActualSignedRequestHeaders() {
        if (actualSignedRequestHeaders == null) {
            this.actualSignedRequestHeaders = new HashMap<String, String>();
        }
        return actualSignedRequestHeaders;
    }

    @Override
    public String toString() {
        return "TemporarySignatureResponse [signedUrl=" + signedUrl + ", actualSignedRequestHeaders="
                + actualSignedRequestHeaders + "]";
    }

}
