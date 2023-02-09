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

package com.aliyun.oss;

/**
 * The OSSException is thrown upon error when accessing OSS.
 */
public class OSSException extends ServiceException {

    private static final long serialVersionUID = -1979779664334663173L;

    private String resourceType;
    private String header;
    private String method;

    public OSSException() {
        super();
    }

    public OSSException(String errorMessage) {
        super(errorMessage);
    }

    public OSSException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }

    public OSSException(String errorMessage, String errorCode, String requestId, String hostId, String header,
            String resourceType, String method) {
        this(errorMessage, errorCode, requestId, hostId, header, resourceType, method, null, null);
    }

    public OSSException(String errorMessage, String errorCode, String requestId, String hostId, String header,
            String resourceType, String method, Throwable cause) {
        this(errorMessage, errorCode, requestId, hostId, header, resourceType, method, null, cause);
    }

    public OSSException(String errorMessage, String errorCode, String requestId, String hostId, String header,
            String resourceType, String method, String rawResponseError) {
        this(errorMessage, errorCode, requestId, hostId, header, resourceType, method, rawResponseError, null);
    }

    public OSSException(String errorMessage, String errorCode, String requestId, String hostId, String header,
            String resourceType, String method, String rawResponseError, Throwable cause) {
        super(errorMessage, errorCode, requestId, hostId, rawResponseError, cause);
        this.resourceType = resourceType;
        this.header = header;
        this.method = method;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getHeader() {
        return header;
    }

    public String getMethod() {
        return method;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + (resourceType == null ? "" : "\n[ResourceType]: " + resourceType)
                + (header == null ? "" : "\n[Header]: " + header) + (method == null ? "" : "\n[Method]: " + method);
    }
}