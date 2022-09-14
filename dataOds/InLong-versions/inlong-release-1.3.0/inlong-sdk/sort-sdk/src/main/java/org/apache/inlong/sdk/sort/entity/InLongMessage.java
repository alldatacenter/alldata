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

package org.apache.inlong.sdk.sort.entity;

import java.util.Map;

public class InLongMessage {

    private String inlongGroupId;
    private String inlongStreamId;
    private long msgTime; //message generation time, milliseconds
    private String sourceIp; // agent ip of message generation
    private final Map<String, String> params;
    private final byte[] body;

    public InLongMessage(byte[] body, Map<String, String> params) {
        this.body = body;
        this.params = params;
    }

    public InLongMessage(String inlongGroupId, String inlongStreamId, long msgTime, String sourceIp, byte[] body,
            Map<String, String> params) {
        this.inlongGroupId = inlongGroupId;
        this.inlongStreamId = inlongStreamId;
        this.msgTime = msgTime;
        this.sourceIp = sourceIp;
        this.body = body;
        this.params = params;
    }

    public long getMsgTime() {
        return msgTime;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public byte[] getBody() {
        return body;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getInlongGroupId() {
        return inlongGroupId;
    }

    public String getInlongStreamId() {
        return inlongStreamId;
    }
}
