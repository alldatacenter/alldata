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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.funtest.server.api.user;

import org.apache.ambari.funtest.server.AmbariHttpWebRequest;
import org.apache.ambari.funtest.server.ConnectionParams;

/**
 * Deletes the specified user.
 */
public class DeleteUserWebRequest extends AmbariHttpWebRequest {
    private final String userName;
    private static String pathFormat = "/api/v1/users/%s";

    /**
     * Deletes a user
     *
     * @param params - Ambari server connection information
     * @param userName - User to be deleted.
     */
    public DeleteUserWebRequest(ConnectionParams params, String userName) {
        super(params);
        this.userName = userName;
    }

    @Override
    public String getHttpMethod() {
        return "DELETE";
    }

    /**
     * Get REST API path fragment for construction full URI.
     *
     * @return - REST API path
     */
    @Override
    protected String getApiPath() {
        return String.format(pathFormat, userName);
    }
}