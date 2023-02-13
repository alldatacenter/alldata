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
 * Token authentication.
 */
@NoArgsConstructor
@JsonTypeDefine(value = TokenAuthentication.TOKEN)
public class TokenAuthentication implements Authentication {

    public static final String TOKEN = "token";

    @Getter
    protected String token;

    public TokenAuthentication(String token) {
        this.token = token;
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.TOKEN;
    }

    @Override
    public void configure(Map<String, String> properties) {
        Preconditions.checkNotEmpty(properties, "Properties cannot be empty when init TokenAuthentication");
        this.token = properties.get(TOKEN);
    }

    @Override
    public String toString() {
        ObjectNode objectNode = JsonUtils.OBJECT_MAPPER.createObjectNode();
        objectNode.put(TOKEN, this.getToken());
        return objectNode.toString();
    }
}
