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

import java.util.HashMap;
import java.util.Map;

/**
 * When an upload succeeds, OSS provides the mechanism to send a post message to
 * a callbackurl to trigger some action defined by that callbackurl. The
 * message's method must be Post and the body is the callbackBody's content and
 * it must match the callbackurl's expectation.
 *
 * APIs that support callback include PutObject, PstObject,
 * CompleteMultipartUpload.
 */
public class Callback {

    public static enum CalbackBodyType {
        URL(1), JSON(2);

        private int nCode;

        private CalbackBodyType(int nCode) {
            this.nCode = nCode;
        }

        @Override
        public String toString() {
            return String.valueOf(this.nCode);
        }
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    /**
     * Sets the callback url---the callbackUrl parameter must be Url encoded. It
     * supports multiple callback urls (separated by ';'). When multiple
     * callback urls are specified, OSS will send callback request one by one
     * until the first successful response. After the callback request is sent,
     * OSS expects to get "200 OK" response with a JSON body. The body size
     * should be no more than 3MB.
     * 
     * @param callbackUrl
     *            The callback url(s) in url encoding.
     */
    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getCallbackHost() {
        return callbackHost;
    }

    /**
     * Sets the callback host, only valid when callbackUrl is set. If this is
     * not set, the host will be extracted from the callbackUrl.
     * 
     * @param callbackHost
     *            The host of OSS callback.
     */
    public void setCallbackHost(String callbackHost) {
        this.callbackHost = callbackHost;
    }

    public String getCallbackBody() {
        return callbackBody;
    }

    /**
     * Sets the callback body.For example:
     * key=$(key) &amp; etag=$(etag) &amp; my_var=$(x:my_var). It supports the OSS system
     * variable, custom defined variable or constant and custom defined
     * variable's callbackVar.
     * 
     * @param callbackBody
     *            OSS callback body.
     */
    public void setCallbackBody(String callbackBody) {
        this.callbackBody = callbackBody;
    }

    public CalbackBodyType getCalbackBodyType() {
        return calbackBodyType;
    }

    /**
     * The content-type header in OSS's callback request. It supports
     * application/x-www-form-urlencoded(url) and application/json(json). The
     * default is the former, which means the variable in callback body will be
     * url encoded. If it's latter, the variable in callback body will be
     * formatted (by the SDK) as json's variable.
     *
     * @param calbackBodyType
     *            The content-type header in OSS callback request.
     */
    public void setCalbackBodyType(CalbackBodyType calbackBodyType) {
        this.calbackBodyType = calbackBodyType;
    }

    public Map<String, String> getCallbackVar() {
        return callbackVar;
    }

    /**
     * Sets user customized parameter(s).
     *
     * Customized parameter is a Map&lt;key,value&gt; instance. In the callback
     * request, OSS would put these parameters into the post body. The keys must
     * start with "x:", such as x:my_var.
     * 
     * @param callbackVar
     *            A {@link Map} instance that stores the &lt;key, value&gt; pairs.
     */
    public void setCallbackVar(Map<String, String> callbackVar) {
        this.callbackVar.clear();
        if (callbackVar != null && !callbackVar.isEmpty()) {
            this.callbackVar.putAll(callbackVar);
        }
    }

    /**
     * Adds a new custom parameter.
     * 
     * @param key
     *            Custom key starting with "x:".
     * @param value
     *            The value for the custom key.
     */
    public void addCallbackVar(String key, String value) {
        this.callbackVar.put(key, value);
    }

    public boolean hasCallbackVar() {
        if (this.callbackVar != null && this.callbackVar.size() > 0) {
            return true;
        }
        return false;
    }

    /**
     * The callbackUrl after a successful upload
     */
    private String callbackUrl;

    /**
     * The callback host, only vaid after the callbackUrl is set. If
     * callbackHost is null, the SDK will extract the host from the callbackUrl.
     */
    private String callbackHost;

    /**
     * The callback body in the request.
     */
    private String callbackBody;

    /**
     * The content-type header in the request. It supports url or json type and
     * url is the default.
     */
    private CalbackBodyType calbackBodyType;

    /**
     * The custom parameters
     */
    private Map<String, String> callbackVar = new HashMap<String, String>();

}
