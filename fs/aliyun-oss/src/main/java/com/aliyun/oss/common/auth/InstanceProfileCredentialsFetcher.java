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

package com.aliyun.oss.common.auth;

import java.net.MalformedURLException;
import java.net.URL;

import com.aliyun.oss.common.auth.Credentials;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.HttpResponse;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class InstanceProfileCredentialsFetcher extends HttpCredentialsFetcher {

    public InstanceProfileCredentialsFetcher() {
    }

    public void setRoleName(String roleName) {
        if (null == roleName || roleName.isEmpty()) {
            throw new IllegalArgumentException("You must specifiy a valid role name.");
        }
        this.roleName = roleName;
    }

    public InstanceProfileCredentialsFetcher withRoleName(String roleName) {
        setRoleName(roleName);
        return this;
    }

    @Override
    public URL buildUrl() throws ClientException {
        try {
            return new URL("http://" + metadataServiceHost + URL_IN_ECS_METADATA + roleName);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    @Override
    public Credentials parse(HttpResponse response) throws ClientException {
        String jsonContent = new String(response.getHttpContent());

        try {
            JSONObject obj = new JSONObject(jsonContent);

            if (obj.has("Code") && obj.has("AccessKeyId") && obj.has("AccessKeySecret") && obj.has("SecurityToken")
                    && obj.has("Expiration")) {
            } else {
                throw new ClientException("Invalid json got from ECS Metadata service.");
            }

            if (!"Success".equals(obj.getString("Code"))) {
                throw new ClientException("Failed to get RAM session credentials from ECS metadata service.");
            }

            return new InstanceProfileCredentials(obj.getString("AccessKeyId"), obj.getString("AccessKeySecret"),
                    obj.getString("SecurityToken"), obj.getString("Expiration"));
        } catch (JSONException e) {
            throw new ClientException("InstanceProfileCredentialsFetcher.parse [" + jsonContent + "] exception:" + e);
        }
    }

    private static final String URL_IN_ECS_METADATA = "/latest/meta-data/ram/security-credentials/";
    private static final String metadataServiceHost = "100.100.100.200";

    private String roleName;

}
