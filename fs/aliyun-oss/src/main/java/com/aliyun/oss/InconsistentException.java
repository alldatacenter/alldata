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
 * <p>
 * This exception indicates the checksum returned from Server side is not same
 * as the one calculated from client side.
 * </p>
 * 
 * 
 * <p>
 * Generally speaking, the caller needs to handle the
 * {@link InconsistentException}, because it means the data uploaded or
 * downloaded is not same as its source. Re-upload or re-download is needed to
 * correct the data.
 * </p>
 * 
 * <p>
 * Operations that could throw this exception include putObject, appendObject,
 * uploadPart, uploadFile, getObject, etc.
 * </p>
 * 
 */
public class InconsistentException extends RuntimeException {

    private static final long serialVersionUID = 2140587868503948665L;

    private Long clientChecksum;
    private Long serverChecksum;
    private String requestId;

    public InconsistentException(Long clientChecksum, Long serverChecksum, String requestId) {
        super();
        this.clientChecksum = clientChecksum;
        this.serverChecksum = serverChecksum;
        this.requestId = requestId;
    }

    public Long getClientChecksum() {
        return clientChecksum;
    }

    public void setClientChecksum(Long clientChecksum) {
        this.clientChecksum = clientChecksum;
    }

    public Long getServerChecksum() {
        return serverChecksum;
    }

    public void setServerChecksum(Long serverChecksum) {
        this.serverChecksum = serverChecksum;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String getMessage() {
        return "InconsistentException " + "\n[RequestId]: " + getRequestId() + "\n[ClientChecksum]: "
                + getClientChecksum() + "\n[ServerChecksum]: " + getServerChecksum();
    }

}
