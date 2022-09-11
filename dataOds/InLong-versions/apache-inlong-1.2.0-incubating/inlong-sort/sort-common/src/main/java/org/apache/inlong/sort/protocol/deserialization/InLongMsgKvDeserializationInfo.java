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

package org.apache.inlong.sort.protocol.deserialization;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * It represents KV format of InLongMsg(m=5).
 */
public class InLongMsgKvDeserializationInfo extends InLongMsgDeserializationInfo {

    private static final long serialVersionUID = 8431516458466278968L;

    private final char entryDelimiter;

    private final char kvDelimiter;

    public InLongMsgKvDeserializationInfo(
            @JsonProperty("tid") String tid,
            @JsonProperty("entry_delimiter") char entryDelimiter,
            @JsonProperty("kv_delimiter") char kvDelimiter
    ) {
        super(tid);
        this.entryDelimiter = entryDelimiter;
        this.kvDelimiter = kvDelimiter;
    }

    @JsonProperty("entry_delimiter")
    public char getEntryDelimiter() {
        return entryDelimiter;
    }

    @JsonProperty("kv_delimiter")
    public char getKvDelimiter() {
        return kvDelimiter;
    }
}
