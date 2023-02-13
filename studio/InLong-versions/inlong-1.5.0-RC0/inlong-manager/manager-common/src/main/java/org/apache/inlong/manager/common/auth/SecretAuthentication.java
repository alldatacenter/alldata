/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.common.auth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.common.util.Preconditions;

import java.util.Map;

/**
 * Secret authentication.
 */
@NoArgsConstructor
@JsonTypeDefine(value = SecretAuthentication.SECRET)
public class SecretAuthentication implements Authentication {

    public static final String SECRET = "secret";

    public static final String SECRET_ID = "secret_id";

    public static final String SECRET_KEY = "secret_key";

    @Getter
    protected String secretId;

    @Getter
    protected String secretKey;

    public SecretAuthentication(String secretId, String secretKey) {
        this.secretId = secretId;
        this.secretKey = secretKey;
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.SECRET;
    }

    @Override
    public void configure(Map<String, String> properties) {
        Preconditions.checkNotEmpty(properties, "Properties cannot be empty when init SecretAuthentication");
        this.secretId = properties.get(SECRET_ID);
        this.secretKey = properties.get(SECRET_KEY);
    }

    @Override
    public String toString() {
        ObjectNode objectNode = JsonUtils.OBJECT_MAPPER.createObjectNode();
        objectNode.put(SECRET_ID, this.getSecretId());
        objectNode.put(SECRET_KEY, this.getSecretKey());
        return objectNode.toString();
    }
}
