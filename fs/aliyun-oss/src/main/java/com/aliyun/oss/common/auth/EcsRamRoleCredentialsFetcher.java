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
import com.aliyun.oss.common.utils.AuthUtils;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.HttpResponse;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class EcsRamRoleCredentialsFetcher extends HttpCredentialsFetcher {

    public EcsRamRoleCredentialsFetcher(String ossAuthServerHost) {
        this.ossAuthServerHost = ossAuthServerHost;
    }

    @Override
    public URL buildUrl() throws ClientException {
        try {
            return new URL(ossAuthServerHost);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    public Credentials parse(HttpResponse response) throws ClientException {
        String jsonContent = new String(response.getHttpContent());

        try {
            JSONObject jsonObject = new JSONObject(jsonContent);

            if (!jsonObject.has("Code")) {
                throw new ClientException("Invalid json " + jsonContent + " got from ecs metadata server.");
            }

            if (!"Success".equals(jsonObject.get("Code"))) {
                throw new ClientException("Failed to get credentials from ecs metadata server");
            }

            if (!jsonObject.has("AccessKeyId") || !jsonObject.has("AccessKeySecret")) {
                throw new ClientException("Invalid json " + jsonContent + " got from ecs metadata server.");
            }

            String securityToken = null;
            if (jsonObject.has("SecurityToken")) {
                securityToken = jsonObject.getString("SecurityToken");
            }

            if (jsonObject.has("Expiration")) {
                return new InstanceProfileCredentials(jsonObject.getString("AccessKeyId"),
                        jsonObject.getString("AccessKeySecret"), securityToken, jsonObject.getString("Expiration"))
                                .withExpiredDuration(
                                        AuthUtils.DEFAULT_ECS_SESSION_TOKEN_DURATION_SECONDS)
                                .withExpiredFactor(0.85);
            }

            return new BasicCredentials(jsonObject.getString("AccessKeyId"), jsonObject.getString("AccessKeySecret"),
                    securityToken);
        } catch (JSONException e) {
            throw new ClientException("EcsRamRoleCredentialsFetcher.parse [" + jsonContent + "] exception:" + e);
        }
    }

    private String ossAuthServerHost;
}
