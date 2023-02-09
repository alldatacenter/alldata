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

import java.util.ArrayList;
import java.util.List;

/**
 * Parameters in a bucket or object preflight request
 * 
 */
public class OptionsInfoRequest extends GenericRequest {

    {
        httpMethod = HttpMethodEnum.OPTIONS;
    }

    private String origin;

    private List<String> requestMethod;

    private List<String> requestHeaders;

    public OptionsInfoRequest() {
    }

    public OptionsInfoRequest(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Obtain the origin of the preflight request.
     * 
     * @return Origin of the preflight request
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * Set the origin of the preflight request.
     * 
     * @param origin
     *            Origin of the preflight request
     */
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    /**
     * Obtain the list of allowed cross-origin request methods.
     * 
     * @return List of allowed cross-origin request methods
     */
    public List<String> getRequestMethod() {
        if (this.requestMethod == null) {
            this.requestMethod = new ArrayList<>();
        }
        return requestMethod;
    }

    /**
     * Set the list of allowed cross-origin request methods.
     * 
     * @param requestMethod
     *            List of allowed cross-origin request methods
     */
    public void setRequestMethod(List<String> requestMethod) {
        this.requestMethod = requestMethod;
    }

    /**
     * Obtain the list of allowed request headers.
     * 
     * @return List of allowed request headers
     */
    public List<String> getRequestHeaders() {
        if (this.requestHeaders == null) {
            this.requestHeaders = new ArrayList<>();
        }
        return requestHeaders;
    }

    /**
     * Set the list of allowed request headers.
     * 
     * @param requestHeaders
     *            List of allowed request headers
     */
    public void setRequestHeaders(List<String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    @Override
    public String toString() {
        return "OptionsInfoRequest [origin=" + origin + ", requestMethod=" + requestMethod + ", requestHeaders="
                + requestHeaders + "]";
    }

}
