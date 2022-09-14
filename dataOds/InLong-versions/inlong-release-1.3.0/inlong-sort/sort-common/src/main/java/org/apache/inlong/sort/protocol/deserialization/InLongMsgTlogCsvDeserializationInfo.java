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

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * It represents TLog CSV format of InLongMsg(m=10).
 */
public class InLongMsgTlogCsvDeserializationInfo extends InLongMsgDeserializationInfo {

    private static final long serialVersionUID = -6585242216925992303L;

    private final char delimiter;

    @JsonCreator
    public InLongMsgTlogCsvDeserializationInfo(
            @JsonProperty("tid") String tid,
            @JsonProperty("delimiter") char delimiter) {
        super(tid);
        this.delimiter = delimiter;
    }

    @JsonProperty("delimiter")
    public char getDelimiter() {
        return delimiter;
    }
}
