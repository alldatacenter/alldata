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

import java.util.Map;
import java.util.TreeMap;

import com.obs.services.internal.Constants;
import com.obs.services.internal.service.InternalHeaderResponse;

/**
 * Public response result, including the request ID and response headers
 *
 */
public class HeaderResponse extends InternalHeaderResponse {

    /**
     * Obtain response headers.
     * 
     * @return Response headers
     */
    public Map<String, Object> getResponseHeaders() {
        if (responseHeaders == null) {
            responseHeaders = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        }
        return responseHeaders;
    }

    /**
     * Obtain original response headers.
     *
     * @return original response headers
     */
    public Map<String, Object> getOriginalHeaders() {
        if (originalHeaders == null) {
            originalHeaders = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
        }
        return originalHeaders;
    }

    /**
     * Obtain the request ID returned by the server.
     * 
     * @return Request ID returned by the server
     */
    public String getRequestId() {
        Object id = this.getResponseHeaders().get(Constants.REQUEST_ID_HEADER);
        return id == null ? "" : id.toString();
    }

    /**
     * Obtain the HTTP status code returned by the server.
     * 
     * @return HTTP status code returned by the server
     */
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String toString() {
        return "HeaderResponse [responseHeaders=" + responseHeaders + ", statusCode=" + statusCode + "]";
    }

}
