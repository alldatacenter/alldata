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

import java.io.InputStream;

/**
 * The result of the callback.
 */
public interface CallbackResult {

    /**
     * Gets the response body of the callback request. The caller needs to close
     * it after usage.
     * 
     * @return The {@link InputStream} instance of the response body.
     */
    public InputStream getCallbackResponseBody();

    /**
     * Sets the callback response body.
     * 
     * @param callbackResponseBody
     *            The {@link InputStream} instance of the response body.
     */
    public void setCallbackResponseBody(InputStream callbackResponseBody);
}
