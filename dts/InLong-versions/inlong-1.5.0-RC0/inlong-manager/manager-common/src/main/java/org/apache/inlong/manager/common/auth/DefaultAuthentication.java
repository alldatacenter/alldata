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
 * Default authentication.
 */
@NoArgsConstructor
@JsonTypeDefine(value = DefaultAuthentication.DEFAULT)
public class DefaultAuthentication implements Authentication {

    public static final String DEFAULT = "default";

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    @Getter
    protected String username;

    @Getter
    protected String password;

    public DefaultAuthentication(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.UNAME_PASSWD;
    }

    @Override
    public void configure(Map<String, String> properties) {
        Preconditions.checkNotEmpty(properties, "Properties cannot be empty when init DefaultAuthentication");
        this.username = properties.get(USERNAME);
        this.password = properties.get(PASSWORD);
    }

    @Override
    public String toString() {
        ObjectNode objectNode = JsonUtils.OBJECT_MAPPER.createObjectNode();
        objectNode.put(USERNAME, this.getUsername());
        objectNode.put(PASSWORD, this.getPassword());
        return objectNode.toString();
    }
}
