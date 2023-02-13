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

package org.apache.inlong.agent.message;

import org.apache.inlong.agent.plugin.Message;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * DefaultMessage is used in inner-data transfer, including two parts, header and body. Header is the attributes of
 * message, and body is the content of message.
 */
public class DefaultMessage implements Message {

    private final byte[] body;
    protected final Map<String, String> header;

    public DefaultMessage(byte[] body, Map<String, String> header) {
        this.body = body;
        this.header = header;
    }

    public DefaultMessage(byte[] body) {
        this(body, new HashMap<>());
    }

    @Override
    public byte[] getBody() {
        return body;
    }

    @Override
    public Map<String, String> getHeader() {
        return header;
    }

    @Override
    public String toString() {
        return new String(body, StandardCharsets.UTF_8);
    }
}
