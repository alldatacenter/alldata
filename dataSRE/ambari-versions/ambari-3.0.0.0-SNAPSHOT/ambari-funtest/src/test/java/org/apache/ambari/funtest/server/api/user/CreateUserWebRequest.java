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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.ambari.funtest.server.AmbariHttpWebRequest;
import org.apache.ambari.funtest.server.ConnectionParams;

/**
 * Creates a new user.
 */
public class CreateUserWebRequest extends AmbariHttpWebRequest {
    private final String userName;
    private final String password;
    private final boolean isActive;
    private final boolean isAdmin;
    private static String pathFormat = "/api/v1/users/%s";

    public enum ActiveUser {
        FALSE,
        TRUE
    }

    public enum AdminUser {
        FALSE,
        TRUE
    }

    /**
     * Adds the specified service component to the specified service.
     *
     * @param params - Ambari server connection information.
     * @param userName - User name.
     * @param password - Password in clear text.
     * @param activeUser - Specifies whether the user is active or not.
     * @param adminUser - Specifies whether the user is an administrator or not.
     */
    public CreateUserWebRequest(ConnectionParams params, String userName, String password,
                             ActiveUser activeUser, AdminUser adminUser) {
        super(params);
        this.userName = userName;
        this.password = password;
        this.isActive = (activeUser == ActiveUser.TRUE);
        this.isAdmin = (adminUser == AdminUser.TRUE);
    }

    @Override
    public String getHttpMethod() {
        return "POST";
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

    @Override
    protected String getRequestData() {
        /**
         * { "Users" : {"active": true/false, "admin": true/false, "password": password } }
         */
        JsonObject jsonObject;
        JsonObject jsonUserInfo = new JsonObject();

        jsonUserInfo.addProperty("active", isActive);
        jsonUserInfo.addProperty("admin", isAdmin);
        jsonUserInfo.addProperty("password", password);

        jsonObject = createJsonObject("Users", jsonUserInfo);
        Gson gson = new Gson();
        return gson.toJson(jsonObject);
    }
}
