/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ranger;

import com.sun.jersey.api.client.ClientResponse;

public class RangerServiceException extends Exception {
    private final ClientResponse.Status status;

    public RangerServiceException(Exception e) {
        super(e);

        this.status = null;
    }

    public RangerServiceException(RangerClient.API api, ClientResponse response) {
        this(api, response == null ? null : ClientResponse.Status.fromStatusCode(response.getStatus()), response == null ? null : response.getEntity(String.class));
    }

    private RangerServiceException(RangerClient.API api, ClientResponse.Status status, String response) {
        super("Ranger API " + api + " failed: statusCode=" + (status != null ? status.getStatusCode() : "null")
                + ", status=" + status + ", response:" + response);

        this.status = status;
    }

    public ClientResponse.Status getStatus() { return status; }
}
